package com.budgetmanager.app.data

import com.budgetmanager.app.data.dao.CategoryAggregation
import com.budgetmanager.app.data.dao.MonthlyTotals
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.repository.TransactionRepositoryImpl
import com.budgetmanager.app.domain.model.TransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SummaryCalculationTest {

    private lateinit var dao: TransactionDao
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = TransactionRepositoryImpl(dao)
    }

    @Test
    fun `mixed income and expense computes correct totals`() = runTest {
        val totals = MonthlyTotals(totalIncome = 10000.0, totalExpenses = 7500.0, transactionCount = 8)
        val breakdowns = listOf(
            CategoryAggregation(type = "income", total = 10000.0, count = 2, category = "Salary"),
            CategoryAggregation(type = "expense", total = 4000.0, count = 3, category = "Rent"),
            CategoryAggregation(type = "expense", total = 2000.0, count = 2, category = "Food"),
            CategoryAggregation(type = "expense", total = 1500.0, count = 1, category = "Transport")
        )
        every { dao.getMonthlyTotals("2026-03-01", "2026-03-31") } returns flowOf(totals)
        every { dao.getMonthlyCategoryBreakdown("2026-03-01", "2026-03-31") } returns flowOf(breakdowns)

        val summary = repository.getMonthlySummary(2026, 3).first()

        assertEquals(10000.0, summary.totalIncome, 0.001)
        assertEquals(7500.0, summary.totalExpenses, 0.001)
        assertEquals(2500.0, summary.netBalance, 0.001)
        assertEquals(8, summary.transactionCount)
        assertEquals(4, summary.categoryBreakdowns.size)
    }

    @Test
    fun `empty month returns zeros`() = runTest {
        val totals = MonthlyTotals(totalIncome = 0.0, totalExpenses = 0.0, transactionCount = 0)
        every { dao.getMonthlyTotals("2026-01-01", "2026-01-31") } returns flowOf(totals)
        every { dao.getMonthlyCategoryBreakdown("2026-01-01", "2026-01-31") } returns flowOf(emptyList())

        val summary = repository.getMonthlySummary(2026, 1).first()

        assertEquals(0.0, summary.totalIncome, 0.001)
        assertEquals(0.0, summary.totalExpenses, 0.001)
        assertEquals(0.0, summary.netBalance, 0.001)
        assertEquals(0, summary.transactionCount)
        assertTrue(summary.categoryBreakdowns.isEmpty())
    }

    @Test
    fun `category breakdown maps types correctly`() = runTest {
        val totals = MonthlyTotals(totalIncome = 5000.0, totalExpenses = 3000.0, transactionCount = 4)
        val breakdowns = listOf(
            CategoryAggregation(type = "income", total = 5000.0, count = 1, category = "Salary"),
            CategoryAggregation(type = "expense", total = 3000.0, count = 3, category = "Food")
        )
        every { dao.getMonthlyTotals("2026-06-01", "2026-06-31") } returns flowOf(totals)
        every { dao.getMonthlyCategoryBreakdown("2026-06-01", "2026-06-31") } returns flowOf(breakdowns)

        val summary = repository.getMonthlySummary(2026, 6).first()
        val incomeBreakdowns = summary.categoryBreakdowns.filter { it.type == TransactionType.INCOME }
        val expenseBreakdowns = summary.categoryBreakdowns.filter { it.type == TransactionType.EXPENSE }

        assertEquals(1, incomeBreakdowns.size)
        assertEquals(1, expenseBreakdowns.size)
        assertEquals("Salary", incomeBreakdowns[0].category)
        assertEquals(5000.0, incomeBreakdowns[0].total, 0.001)
        assertEquals("Food", expenseBreakdowns[0].category)
        assertEquals(3000.0, expenseBreakdowns[0].total, 0.001)
    }

    @Test
    fun `net balance is income minus expenses`() = runTest {
        val totals = MonthlyTotals(totalIncome = 1200.0, totalExpenses = 1800.0, transactionCount = 5)
        every { dao.getMonthlyTotals("2026-02-01", "2026-02-31") } returns flowOf(totals)
        every { dao.getMonthlyCategoryBreakdown("2026-02-01", "2026-02-31") } returns flowOf(emptyList())

        val summary = repository.getMonthlySummary(2026, 2).first()

        assertEquals(-600.0, summary.netBalance, 0.001)
    }
}
