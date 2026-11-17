package com.budgetmanager.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetmanager.app.data.entity.DeletedTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletedTransactionDao {

    @Query("SELECT * FROM deleted_transactions ORDER BY deleted_at DESC, id DESC")
    fun observeAll(): Flow<List<DeletedTransactionEntity>>

    @Query("SELECT * FROM deleted_transactions WHERE budget_id = :budgetId ORDER BY deleted_at DESC, id DESC")
    fun observeByBudget(budgetId: Long): Flow<List<DeletedTransactionEntity>>

    @Query("SELECT * FROM deleted_transactions WHERE id = :id")
    suspend fun getById(id: Long): DeletedTransactionEntity?

    @Query("SELECT * FROM deleted_transactions ORDER BY id ASC")
    suspend fun getAll(): List<DeletedTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeletedTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DeletedTransactionEntity>)

    @Query("DELETE FROM deleted_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM deleted_transactions WHERE budget_id = :budgetId")
    suspend fun deleteByBudget(budgetId: Long)

    @Query("DELETE FROM deleted_transactions")
    suspend fun deleteAll()

    /** Removes bin entries deleted before [cutoff] (ISO-8601, compared lexically). */
    @Query("DELETE FROM deleted_transactions WHERE deleted_at < :cutoff")
    suspend fun deleteOlderThan(cutoff: String): Int

    /**
     * Used by recurring generation so that an auto-generated occurrence the user
     * deleted is not re-created while it is still in the bin.
     */
    @Query("SELECT COUNT(*) FROM deleted_transactions WHERE recurring_id = :recurringId AND date = :date")
    suspend fun countByRecurringAndDate(recurringId: Long, date: String): Int
}
