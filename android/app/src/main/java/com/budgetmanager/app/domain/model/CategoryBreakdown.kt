package com.budgetmanager.app.domain.model

data class CategoryBreakdown(
    val category: String,
    val type: TransactionType,
    val total: Double,
    val count: Int
)
