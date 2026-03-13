package com.budgetmanager.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    @SerialName("created_at")
    val createdAt: String,
    val transactions: List<BackupTransaction>,
    @SerialName("recurring_transactions")
    val recurringTransactions: List<BackupRecurringTransaction>
)

@Serializable
data class BackupTransaction(
    val id: Long,
    val type: String,
    val amount: Double,
    val category: String,
    val description: String = "",
    val date: String,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("recurring_id")
    val recurringId: Long? = null
)

@Serializable
data class BackupRecurringTransaction(
    val id: Long,
    val type: String,
    val amount: Double,
    val category: String,
    val description: String = "",
    val frequency: String,
    @SerialName("day_of_week")
    val dayOfWeek: Int? = null,
    @SerialName("day_of_month")
    val dayOfMonth: Int? = null,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String? = null,
    @SerialName("is_active")
    val isActive: Int = 1,
    @SerialName("created_at")
    val createdAt: String = ""
)
