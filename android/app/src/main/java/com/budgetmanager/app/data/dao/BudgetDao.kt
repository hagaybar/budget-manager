package com.budgetmanager.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.budgetmanager.app.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY created_at ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE is_active = 1 LIMIT 1")
    fun observeActive(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE id = :id")
    fun observeById(id: Long): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveBudget(): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY id ASC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun count(): Int

    @Query("UPDATE budgets SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE budgets SET is_active = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Transaction
    suspend fun setActiveBudget(id: Long) {
        deactivateAll()
        activate(id)
    }
}
