package com.budgetmanager.app.data

import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.entity.RecurringTransactionEntity
import com.budgetmanager.app.data.repository.RecurringRepositoryImpl
import com.budgetmanager.app.domain.model.Frequency
import com.budgetmanager.app.domain.model.RecurringTransaction
import com.budgetmanager.app.domain.model.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecurringRepositoryTest {

    private lateinit var recurringDao: RecurringTransactionDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var repository: RecurringRepositoryImpl

    @Before
    fun setUp() {
        recurringDao = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)
        repository = RecurringRepositoryImpl(recurringDao, transactionDao)
    }

    // -- observeAll --

    @Test
    fun `observeAll returns mapped recurring transactions`() = runTest {
        val entities = listOf(
            RecurringTransactionEntity(
                id = 1, type = "income", amount = 8000.0, category = "Salary",
                frequency = "monthly", dayOfMonth = 1, startDate = "2026-01-01", isActive = 1
            )
        )
        every { recurringDao.observeAll() } returns flowOf(entities)

        val result = repository.observeAll().first()

        assertEquals(1, result.size)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(Frequency.MONTHLY, result[0].frequency)
        assertEquals(true, result[0].isActive)
    }

    // -- create --

    @Test
    fun `create calls dao insert and returns id`() = runTest {
        val recurring = RecurringTransaction(
            type = TransactionType.EXPENSE, amount = 500.0, category = "Rent",
            frequency = Frequency.MONTHLY, dayOfMonth = 1, startDate = "2026-01-01"
        )
        coEvery { recurringDao.insert(any()) } returns 7L

        val id = repository.create(recurring)

        assertEquals(7L, id)
        coVerify { recurringDao.insert(any()) }
    }

    // -- update --

    @Test
    fun `update calls dao update`() = runTest {
        val recurring = RecurringTransaction(
            id = 3, type = TransactionType.EXPENSE, amount = 600.0, category = "Rent",
            frequency = Frequency.MONTHLY, dayOfMonth = 1, startDate = "2026-01-01"
        )

        repository.update(recurring)

        coVerify { recurringDao.update(any()) }
    }

    // -- delete --

    @Test
    fun `delete nullifies recurring id then deletes`() = runTest {
        repository.delete(5L)

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            transactionDao.nullifyRecurringId(5L)
            recurringDao.deleteById(5L)
        }
    }

    // -- getById --

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { recurringDao.getById(99L) } returns null

        val result = repository.getById(99L)

        assertNull(result)
    }

    // -- generateTransactions (monthly) --

    @Test
    fun `generateTransactions creates monthly transactions`() = runTest {
        val entity = RecurringTransactionEntity(
            id = 1, type = "expense", amount = 500.0, category = "Rent",
            frequency = "monthly", dayOfMonth = 15, startDate = "2026-01-01", isActive = 1
        )
        coEvery { recurringDao.getById(1L) } returns entity
        coEvery { transactionDao.countByRecurringAndDate(1L, any()) } returns 0
        coEvery { transactionDao.insert(any()) } returnsMany listOf(100L, 101L, 102L)

        val result = repository.generateTransactions(1L, "2026-01-01", "2026-03-31")

        assertEquals(3, result.size)
        assertTrue(result.all { it.category == "Rent" })
        assertTrue(result.all { it.amount == 500.0 })
    }

    // -- generateTransactions skips existing --

    @Test
    fun `generateTransactions skips dates with existing transactions`() = runTest {
        val entity = RecurringTransactionEntity(
            id = 2, type = "income", amount = 8000.0, category = "Salary",
            frequency = "monthly", dayOfMonth = 1, startDate = "2026-01-01", isActive = 1
        )
        coEvery { recurringDao.getById(2L) } returns entity
        coEvery { transactionDao.countByRecurringAndDate(2L, "2026-01-01") } returns 1
        coEvery { transactionDao.countByRecurringAndDate(2L, "2026-02-01") } returns 0
        coEvery { transactionDao.insert(any()) } returns 200L

        val result = repository.generateTransactions(2L, "2026-01-01", "2026-02-28")

        assertEquals(1, result.size)
        assertEquals("2026-02-01", result[0].date)
    }

    // -- generateTransactions returns empty for missing recurring --

    @Test
    fun `generateTransactions returns empty when recurring not found`() = runTest {
        coEvery { recurringDao.getById(999L) } returns null

        val result = repository.generateTransactions(999L, "2026-01-01", "2026-03-31")

        assertTrue(result.isEmpty())
    }
}
