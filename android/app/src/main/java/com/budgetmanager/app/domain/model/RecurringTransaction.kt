package com.budgetmanager.app.domain.model

data class RecurringTransaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String = "",
    val frequency: Frequency,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val startDate: String,
    val endDate: String? = null,
    val isActive: Boolean = true,
    val createdAt: String = "",
    val budgetId: Long = 0
)
