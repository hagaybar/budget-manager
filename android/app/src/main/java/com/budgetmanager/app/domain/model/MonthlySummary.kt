package com.budgetmanager.app.domain.model

data class MonthlySummary(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netBalance: Double,
    val transactionCount: Int,
    val categoryBreakdowns: List<CategoryBreakdown>
)
