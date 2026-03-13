package com.budgetmanager.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index(value = ["is_active"]),
        Index(value = ["frequency"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "description", defaultValue = "")
    val description: String = "",

    @ColumnInfo(name = "frequency")
    val frequency: String,

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int? = null,

    @ColumnInfo(name = "day_of_month")
    val dayOfMonth: Int? = null,

    @ColumnInfo(name = "start_date")
    val startDate: String,

    @ColumnInfo(name = "end_date")
    val endDate: String? = null,

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Int = 1,

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: String = ""
)
