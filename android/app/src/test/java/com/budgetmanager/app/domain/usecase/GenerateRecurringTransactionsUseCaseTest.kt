package com.budgetmanager.app.domain.usecase

import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.entity.RecurringTransactionEntity
import com.budgetmanager.app.data.entity.TransactionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for [GenerateRecurringTransactionsUseCase].
 *
 * The headline scenario motivating the backfill logic: a weekly-Friday recurring
 * transaction whose scheduled day passed while the worker was not running and the
 * user did not open the app. On the next invocation (any later day), the missed
 * Friday must be inserted exactly once and no earlier dates may be touched.
 */
class GenerateRecurringTransactionsUseCaseTest {

    private lateinit var recurringDao: RecurringTransactionDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var useCase: GenerateRecurringTransactionsUseCase
    private val insertedTransactions = mutableListOf<TransactionEntity>()

    @Before
    fun setUp() {
        recurringDao = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)
        useCase = GenerateRecurringTransactionsUseCase(recurringDao, transactionDao)
        insertedTransactions.clear()

        // Default: no pre-existing transactions; record every insert the use case
        // performs so tests can assert on the exact rows that would be written.
        coEvery { transactionDao.countByRecurringAndDate(any(), any()) } returns 0
        coEvery {
            transactionDao.findOrphanIdByValueOnDate(any(), any(), any(), any(), any())
        } returns null
        coEvery { transactionDao.insert(any()) } answers {
            val tx = firstArg<TransactionEntity>()
            insertedTransactions.add(tx)
            (insertedTransactions.size).toLong()
        }
    }

    // ── Core backfill behaviour ──────────────────────────────────────────

    @Test
    fun `backfills a missed weekly Friday when invoked on the next day`() = runTest {
        // Friday 2026-04-10 is the scheduled day; invocation happens on Saturday.
        val today = LocalDate.of(2026, 4, 11)
        val recurring = weeklyFriday(startDate = "2026-01-01")
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        val created = useCase.execute(today)

        assertTrue("expected at least one backfilled insert", created >= 1)
        val insertedDates = capturedInsertDates()
        assertTrue(
            "missed Friday 2026-04-10 must be backfilled, got: $insertedDates",
            insertedDates.contains("2026-04-10")
        )
    }

    @Test
    fun `weekly backfill inserts every Friday within the 90-day window`() = runTest {
        val today = LocalDate.of(2026, 4, 14) // Tuesday
        val recurring = weeklyFriday(startDate = "2026-01-01")
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        val created = useCase.execute(today)

        val insertedDates = capturedInsertDates()
        // Fridays in the window [today-90d=2026-01-14, today=2026-04-14]
        val expectedFridays = expectedFridaysBetween(
            LocalDate.of(2026, 1, 14),
            LocalDate.of(2026, 4, 14)
        )
        assertEquals(expectedFridays.size, created)
        assertEquals(expectedFridays, insertedDates.toSet())
    }

    // ── start_date floor: never insert before recurring.startDate ───────

    @Test
    fun `does not insert any occurrences earlier than startDate`() = runTest {
        // startDate is Friday 2026-04-03; today is 2026-04-14. Only 2026-04-03
        // and 2026-04-10 are valid Fridays within the window.
        val today = LocalDate.of(2026, 4, 14)
        val recurring = weeklyFriday(startDate = "2026-04-03")
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        useCase.execute(today)

        val insertedDates = capturedInsertDates()
        assertEquals(setOf("2026-04-03", "2026-04-10"), insertedDates.toSet())
        // Sanity: no date before startDate may ever appear.
        assertTrue(
            "inserts before startDate=2026-04-03: $insertedDates",
            insertedDates.all { it >= "2026-04-03" }
        )
    }

    @Test
    fun `monthly dayOfMonth earlier than startDate in same month is skipped`() = runTest {
        // Monthly on the 10th, but start_date=2026-02-15, today=2026-04-14.
        // February's 10th is before the start, so it must NOT be created.
        // March 10 and April 10 are valid.
        val today = LocalDate.of(2026, 4, 14)
        val recurring = RecurringTransactionEntity(
            id = 42,
            type = "income",
            amount = 1000.0,
            category = "Allowance",
            description = "",
            frequency = "monthly",
            dayOfMonth = 10,
            startDate = "2026-02-15",
            isActive = 1
        )
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        useCase.execute(today)

        val insertedDates = capturedInsertDates()
        assertEquals(setOf("2026-03-10", "2026-04-10"), insertedDates.toSet())
        assertTrue(
            "no insert may precede startDate=2026-02-15, got: $insertedDates",
            insertedDates.all { it >= "2026-02-15" }
        )
    }

    // ── 90-day cap ───────────────────────────────────────────────────────

    @Test
    fun `backfill window is capped at MAX_BACKFILL_DAYS even when startDate is far older`() = runTest {
        // startDate one year ago; only the last 90 days must be backfilled.
        val today = LocalDate.of(2026, 4, 14)
        val recurring = weeklyFriday(startDate = "2025-04-14")
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        useCase.execute(today)

        val insertedDates = capturedInsertDates()
        val cutoff = today.minusDays(GenerateRecurringTransactionsUseCase.MAX_BACKFILL_DAYS)
        assertTrue(
            "oldest insert must be within ${GenerateRecurringTransactionsUseCase.MAX_BACKFILL_DAYS} days: $insertedDates",
            insertedDates.all { LocalDate.parse(it) >= cutoff }
        )
    }

    // ── end_date ceiling ─────────────────────────────────────────────────

    @Test
    fun `does not insert occurrences after endDate`() = runTest {
        val today = LocalDate.of(2026, 4, 14)
        val recurring = weeklyFriday(startDate = "2026-01-01", endDate = "2026-03-31")
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        useCase.execute(today)

        val insertedDates = capturedInsertDates()
        assertTrue(
            "no insert may exceed endDate=2026-03-31: $insertedDates",
            insertedDates.all { it <= "2026-03-31" }
        )
        assertTrue(
            "expected some backfills in window, got none",
            insertedDates.isNotEmpty()
        )
    }

    // ── monthly clamp to last day ────────────────────────────────────────

    @Test
    fun `monthly day 31 clamps to last day of shorter months`() = runTest {
        val today = LocalDate.of(2026, 4, 30)
        val recurring = RecurringTransactionEntity(
            id = 7,
            type = "expense",
            amount = 500.0,
            category = "Rent",
            description = "",
            frequency = "monthly",
            dayOfMonth = 31,
            startDate = "2026-02-01",
            isActive = 1
        )
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        useCase.execute(today)

        val insertedDates = capturedInsertDates().toSet()
        // Feb has 28 days in 2026, March has 31, April has 30.
        assertEquals(setOf("2026-02-28", "2026-03-31", "2026-04-30"), insertedDates)
    }

    // ── idempotency ──────────────────────────────────────────────────────

    @Test
    fun `skips dates that already exist for this recurring`() = runTest {
        val today = LocalDate.of(2026, 4, 11)
        val recurring = weeklyFriday(startDate = "2026-04-01")
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        // Pretend 2026-04-03 already exists (e.g. user added it manually).
        coEvery { transactionDao.countByRecurringAndDate(recurring.id, "2026-04-03") } returns 1

        useCase.execute(today)

        val insertedDates = capturedInsertDates()
        assertTrue(
            "existing date 2026-04-03 must not be re-inserted: $insertedDates",
            !insertedDates.contains("2026-04-03")
        )
        assertTrue(
            "missed Friday 2026-04-10 must still be backfilled: $insertedDates",
            insertedDates.contains("2026-04-10")
        )
    }

    // ── inactive recurrings ──────────────────────────────────────────────

    @Test
    fun `does nothing when no active recurrings exist`() = runTest {
        val today = LocalDate.of(2026, 4, 14)
        coEvery { recurringDao.getActive() } returns emptyList()

        val created = useCase.execute(today)

        assertEquals(0, created)
        coVerify(exactly = 0) { transactionDao.insert(any()) }
    }

    // ── orphan relink (recurring_id stripped by edit / pre-FK app) ──────

    @Test
    fun `relinks orphan transaction by value match instead of inserting duplicate`() = runTest {
        val today = LocalDate.of(2026, 4, 11)
        val recurring = RecurringTransactionEntity(
            id = 7,
            type = "income",
            amount = 5000.0,
            category = "Salary",
            description = "",
            frequency = "weekly",
            dayOfWeek = 4, // Friday
            startDate = "2026-04-01",
            isActive = 1,
            budgetId = 2
        )
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        // 2026-04-10 (the missed Friday) has an orphan row matching the
        // recurring's value pattern. The use case must relink, not duplicate.
        coEvery {
            transactionDao.findOrphanIdByValueOnDate(
                date = "2026-04-10",
                type = "income",
                amount = 5000.0,
                category = "Salary",
                budgetId = 2
            )
        } returns 99L

        useCase.execute(today)

        coVerify(exactly = 1) { transactionDao.setRecurringId(99L, 7L) }
        // No new row inserted for the date that had a relinkable orphan.
        assertTrue(
            "must not insert a duplicate when an orphan was relinked: ${capturedInsertDates()}",
            "2026-04-10" !in capturedInsertDates()
        )
    }

    @Test
    fun `inserts when no orphan match exists for a date`() = runTest {
        val today = LocalDate.of(2026, 4, 11)
        val recurring = weeklyFriday(startDate = "2026-04-01")
        coEvery { recurringDao.getActive() } returns listOf(recurring)
        // Default mock returns null for findOrphanIdByValueOnDate (no match).

        useCase.execute(today)

        coVerify(exactly = 0) { transactionDao.setRecurringId(any(), any()) }
        assertTrue(
            "missed Friday must still be inserted when no orphan exists",
            "2026-04-10" in capturedInsertDates()
        )
    }

    // ── return value plumbing through the entity ─────────────────────────

    @Test
    fun `preserves type amount category description and budgetId on generated transactions`() = runTest {
        val today = LocalDate.of(2026, 4, 11)
        val recurring = RecurringTransactionEntity(
            id = 9,
            type = "expense",
            amount = 42.5,
            category = "Coffee",
            description = "Weekly latte",
            frequency = "weekly",
            dayOfWeek = 4, // Friday
            startDate = "2026-04-10",
            isActive = 1,
            budgetId = 3
        )
        coEvery { recurringDao.getActive() } returns listOf(recurring)

        useCase.execute(today)

        val inserted = capturedInserts()
        assertEquals(1, inserted.size)
        val tx = inserted.single()
        assertEquals("expense", tx.type)
        assertEquals(42.5, tx.amount, 0.0)
        assertEquals("Coffee", tx.category)
        assertEquals("Weekly latte", tx.description)
        assertEquals("2026-04-10", tx.date)
        assertEquals(9L, tx.recurringId)
        assertEquals(3L, tx.budgetId)
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private fun weeklyFriday(
        startDate: String,
        endDate: String? = null,
        id: Long = 1L
    ) = RecurringTransactionEntity(
        id = id,
        type = "income",
        amount = 100.0,
        category = "Paycheck",
        description = "",
        frequency = "weekly",
        dayOfWeek = 4, // 0=Mon..6=Sun convention -> 4=Friday
        startDate = startDate,
        endDate = endDate,
        isActive = 1
    )

    private fun capturedInserts(): List<TransactionEntity> = insertedTransactions.toList()

    private fun capturedInsertDates(): List<String> = capturedInserts().map { it.date }

    private fun expectedFridaysBetween(start: LocalDate, end: LocalDate): Set<String> {
        val result = mutableSetOf<String>()
        var cur = start
        while (cur.dayOfWeek.value != 5) cur = cur.plusDays(1) // 5 == Friday in java.time
        while (!cur.isAfter(end)) {
            result.add(cur.toString())
            cur = cur.plusWeeks(1)
        }
        return result
    }
}
