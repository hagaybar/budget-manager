package com.budgetmanager.app.data.repository

import com.budgetmanager.app.domain.model.DeletedTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Recycle bin for transactions: deleting moves a transaction here instead of
 * erasing it, so it can be restored. Entries older than [RETENTION_DAYS] are
 * purged automatically.
 */
interface RecycleBinRepository {
    fun observeByBudget(budgetId: Long): Flow<List<DeletedTransaction>>
    fun observeAll(): Flow<List<DeletedTransaction>>

    /** Moves the transaction to the bin. Returns false if it no longer exists. */
    suspend fun moveToBin(transactionId: Long): Boolean

    /** Moves a bin entry back to the transaction list. Returns the restored id, or null. */
    suspend fun restore(id: Long): Long?

    /** Permanently deletes a single bin entry. */
    suspend fun deleteForever(id: Long)

    /** Permanently deletes every bin entry of [budgetId]. */
    suspend fun emptyBin(budgetId: Long)

    /** Permanently deletes entries older than [RETENTION_DAYS]. Returns how many were removed. */
    suspend fun purgeExpired(): Int

    companion object {
        const val RETENTION_DAYS = 30L
    }
}
