package com.budgetmanager.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["is_active"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description", defaultValue = "")
    val description: String = "",

    @ColumnInfo(name = "currency", defaultValue = "ILS")
    val currency: String = "ILS",

    @ColumnInfo(name = "monthly_target")
    val monthlyTarget: Double? = null,

    @ColumnInfo(name = "is_active", defaultValue = "0")
    val isActive: Int = 0,

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: String = ""
)
