package com.budgetmanager.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetmanager.app.data.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    // ── Unscoped queries (kept for backup/export across all budgets) ──

    @Query("SELECT * FROM recurring_transactions ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE is_active = 1 ORDER BY created_at DESC")
    fun observeActive(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    fun observeById(id: Long): Flow<RecurringTransactionEntity?>

    // ── Budget-scoped queries ──

    @Query("SELECT * FROM recurring_transactions WHERE budget_id = :budgetId ORDER BY created_at DESC")
    fun observeAllByBudget(budgetId: Long): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE budget_id = :budgetId AND is_active = 1 ORDER BY created_at DESC")
    fun observeActiveByBudget(budgetId: Long): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE budget_id = :budgetId AND is_active = 1 ORDER BY id ASC")
    suspend fun getActiveByBudget(budgetId: Long): List<RecurringTransactionEntity>

    // ── Mutations & non-scoped reads (shared across all budgets) ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recurring: List<RecurringTransactionEntity>)

    @Update
    suspend fun update(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions ORDER BY id ASC")
    suspend fun getAll(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE is_active = 1 ORDER BY id ASC")
    suspend fun getActive(): List<RecurringTransactionEntity>

    @Query("UPDATE recurring_transactions SET budget_id = :budgetId WHERE budget_id = 0")
    suspend fun assignOrphanedToBudget(budgetId: Long): Int
}
