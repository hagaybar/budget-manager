package com.budgetmanager.app.data

import com.budgetmanager.app.data.dao.CategoryAggregation
import com.budgetmanager.app.data.dao.MonthlyTotals
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.entity.TransactionEntity
import com.budgetmanager.app.data.repository.TransactionRepositoryImpl
import com.budgetmanager.app.domain.model.Transaction
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
import org.junit.Before
import org.junit.Test

class TransactionRepositoryTest {

    private lateinit var dao: TransactionDao
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = TransactionRepositoryImpl(dao)
    }

    // -- observeAll --

    @Test
    fun `observeAll returns mapped domain transactions`() = runTest {
        val entities = listOf(
            TransactionEntity(id = 1, type = "income", amount = 100.0, category = "Salary", date = "2026-03-01"),
            TransactionEntity(id = 2, type = "expense", amount = 50.0, category = "Food", date = "2026-03-02")
        )
        every { dao.observeAll() } returns flowOf(entities)

        val result = repository.observeAll().first()

        assertEquals(2, result.size)
        assertEquals(TransactionType.INCOME, result[0].type)
        assertEquals(100.0, result[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result[1].type)
    }

    @Test
    fun `observeAll returns empty list when no transactions`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())

        val result = repository.observeAll().first()

        assertEquals(0, result.size)
    }

    // -- create --

    @Test
    fun `create calls dao insert and returns id`() = runTest {
        val transaction = Transaction(
            type = TransactionType.EXPENSE,
            amount = 75.0,
            category = "Transport",
            date = "2026-03-10"
        )
        coEvery { dao.insert(any()) } returns 42L

        val id = repository.create(transaction)

        assertEquals(42L, id)
        coVerify { dao.insert(any()) }
    }

    // -- update --

    @Test
    fun `update calls dao update`() = runTest {
        val transaction = Transaction(
            id = 1,
            type = TransactionType.INCOME,
            amount = 200.0,
            category = "Freelance",
            date = "2026-03-05"
        )
        coEvery { dao.update(any()) } returns Unit

        repository.update(transaction)

        coVerify { dao.update(any()) }
    }

    // -- delete --

    @Test
    fun `delete calls dao deleteById`() = runTest {
        coEvery { dao.deleteById(5L) } returns Unit

        repository.delete(5L)

        coVerify { dao.deleteById(5L) }
    }

    // -- getById --

    @Test
    fun `getById returns domain transaction when found`() = runTest {
        val entity = TransactionEntity(
            id = 10, type = "expense", amount = 30.0,
            category = "Coffee", date = "2026-03-12"
        )
        coEvery { dao.getById(10L) } returns entity

        val result = repository.getById(10L)

        assertEquals(10L, result?.id)
        assertEquals(TransactionType.EXPENSE, result?.type)
        assertEquals("Coffee", result?.category)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(999L) } returns null

        val result = repository.getById(999L)

        assertNull(result)
    }

    // -- getMonthlySummary --

    @Test
    fun `getMonthlySummary combines totals and breakdowns`() = runTest {
        val totals = MonthlyTotals(totalIncome = 5000.0, totalExpenses = 3000.0, transactionCount = 10)
        val breakdowns = listOf(
            CategoryAggregation(type = "income", total = 5000.0, count = 3, category = "Salary"),
            CategoryAggregation(type = "expense", total = 2000.0, count = 5, category = "Food"),
            CategoryAggregation(type = "expense", total = 1000.0, count = 2, category = "Transport")
        )
        every { dao.getMonthlyTotals("2026-03-01", "2026-03-31") } returns flowOf(totals)
        every { dao.getMonthlyCategoryBreakdown("2026-03-01", "2026-03-31") } returns flowOf(breakdowns)

        val summary = repository.getMonthlySummary(2026, 3).first()

        assertEquals(5000.0, summary.totalIncome, 0.001)
        assertEquals(3000.0, summary.totalExpenses, 0.001)
        assertEquals(2000.0, summary.netBalance, 0.001)
        assertEquals(10, summary.transactionCount)
        assertEquals(3, summary.categoryBreakdowns.size)
    }
}
