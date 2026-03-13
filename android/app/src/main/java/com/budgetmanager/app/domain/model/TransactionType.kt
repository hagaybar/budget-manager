package com.budgetmanager.app.domain.model

enum class TransactionType(val value: String) {
    INCOME("income"),
    EXPENSE("expense");

    companion object {
        fun fromString(value: String): TransactionType =
            entries.first { it.value == value }
    }
}
