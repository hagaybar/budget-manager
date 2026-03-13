package com.budgetmanager.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurring_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["recurring_id"])
    ]
)
data class TransactionEntity(
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

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: String = "",

    @ColumnInfo(name = "recurring_id")
    val recurringId: Long? = null
)
