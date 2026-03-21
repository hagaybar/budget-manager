package com.budgetmanager.app.domain.model

data class Budget(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val currency: String = "ILS",
    val monthlyTarget: Double? = null,
    val isActive: Boolean = false,
    val createdAt: String = ""
)
