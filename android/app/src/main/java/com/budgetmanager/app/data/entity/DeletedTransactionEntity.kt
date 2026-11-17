package com.budgetmanager.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A transaction that the user deleted and that now sits in the recycle bin.
 *
 * Rows are moved here from `transactions` (keeping their original id) and can be
 * moved back with "Restore". Kept in a separate table so that every existing
 * query on `transactions` (lists, summaries, charts) keeps ignoring deleted rows
 * without needing an extra filter.
 *
 * `recurring_id` intentionally has no foreign key: a recurring definition may be
 * deleted while its occurrence is still in the bin. Restore clears the link in
 * that case. `budget_id` cascades so that deleting a budget also empties its bin.
 */
@Entity(
    tableName = "deleted_transactions",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["budget_id"]),
        Index(value = ["recurring_id", "date"]),
        Index(value = ["deleted_at"])
    ]
)
data class DeletedTransactionEntity(
    @PrimaryKey
    val id: Long,

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
    val recurringId: Long? = null,

    @ColumnInfo(name = "budget_id", defaultValue = "0")
    val budgetId: Long = 0,

    /** UTC ISO-8601 timestamp of when the transaction was moved to the bin. */
    @ColumnInfo(name = "deleted_at")
    val deletedAt: String
)
