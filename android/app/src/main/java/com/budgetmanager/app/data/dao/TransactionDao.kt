package com.budgetmanager.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.budgetmanager.app.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryAggregation(
    val type: String,
    val total: Double,
    val count: Int,
    val category: String
)

data class MonthlyTotals(
    val totalIncome: Double,
    val totalExpenses: Double,
    val transactionCount: Int
)

@Dao
interface TransactionDao {

    // ── Unscoped queries (kept for backup/export across all budgets) ──

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category = :category)
          AND (:dateFrom IS NULL OR date >= :dateFrom)
          AND (:dateTo IS NULL OR date <= :dateTo)
        ORDER BY date DESC, id DESC
    """)
    fun observeFiltered(
        type: String?,
        category: String?,
        dateFrom: String?,
        dateTo: String?
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("""
        SELECT * FROM transactions
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date DESC, id DESC
    """)
    fun observeByDateRange(startDate: String, endDate: String): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT category FROM transactions ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>

    @Query("""
        SELECT type, SUM(amount) as total, COUNT(*) as count, category
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY type, category
        ORDER BY category ASC
    """)
    fun getMonthlyCategoryBreakdown(
        startDate: String,
        endDate: String
    ): Flow<List<CategoryAggregation>>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) as totalIncome,
            COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) as totalExpenses,
            COUNT(*) as transactionCount
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
    """)
    fun getMonthlyTotals(startDate: String, endDate: String): Flow<MonthlyTotals>

    // ── Budget-scoped queries ──

    @Query("SELECT * FROM transactions WHERE budget_id = :budgetId ORDER BY date DESC, id DESC")
    fun observeAllByBudget(budgetId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE budget_id = :budgetId
          AND (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category = :category)
          AND (:dateFrom IS NULL OR date >= :dateFrom)
          AND (:dateTo IS NULL OR date <= :dateTo)
        ORDER BY date DESC, id DESC
    """)
    fun observeFilteredByBudget(
        budgetId: Long,
        type: String?,
        category: String?,
        dateFrom: String?,
        dateTo: String?
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE budget_id = :budgetId AND date >= :startDate AND date <= :endDate
        ORDER BY date DESC, id DESC
    """)
    fun observeByDateRangeByBudget(
        budgetId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT category FROM transactions WHERE budget_id = :budgetId ORDER BY category ASC")
    fun observeCategoriesByBudget(budgetId: Long): Flow<List<String>>

    @Query("""
        SELECT type, SUM(amount) as total, COUNT(*) as count, category
        FROM transactions
        WHERE budget_id = :budgetId AND date >= :startDate AND date <= :endDate
        GROUP BY type, category
        ORDER BY category ASC
    """)
    fun getMonthlyCategoryBreakdownByBudget(
        budgetId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<CategoryAggregation>>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) as totalIncome,
            COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) as totalExpenses,
            COUNT(*) as transactionCount
        FROM transactions
        WHERE budget_id = :budgetId AND date >= :startDate AND date <= :endDate
    """)
    fun getMonthlyTotalsByBudget(
        budgetId: Long,
        startDate: String,
        endDate: String
    ): Flow<MonthlyTotals>

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE recurring_id = :recurringId AND date = :date AND budget_id = :budgetId
    """)
    suspend fun countByRecurringAndDateByBudget(recurringId: Long, date: String, budgetId: Long): Int

    // ── Mutations & non-scoped reads (shared across all budgets) ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE recurring_id = :recurringId AND date = :date
    """)
    suspend fun countByRecurringAndDate(recurringId: Long, date: String): Int

    @Query("UPDATE transactions SET recurring_id = NULL WHERE recurring_id = :recurringId")
    suspend fun nullifyRecurringId(recurringId: Long)

    @Query("UPDATE transactions SET budget_id = :budgetId WHERE budget_id = 0")
    suspend fun assignOrphanedToBudget(budgetId: Long): Int
}
