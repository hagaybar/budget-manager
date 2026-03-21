package com.budgetmanager.app.domain.model

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String = "",
    val date: String,
    val createdAt: String = "",
    val recurringId: Long? = null,
    val budgetId: Long = 0
)
