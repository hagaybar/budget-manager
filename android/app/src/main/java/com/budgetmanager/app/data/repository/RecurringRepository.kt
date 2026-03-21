package com.budgetmanager.app.data.repository

import com.budgetmanager.app.domain.model.RecurringTransaction
import com.budgetmanager.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun observeAll(): Flow<List<RecurringTransaction>>
    fun observeActive(): Flow<List<RecurringTransaction>>
    fun observeById(id: Long): Flow<RecurringTransaction?>
    suspend fun create(recurring: RecurringTransaction): Long
    suspend fun update(recurring: RecurringTransaction)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): RecurringTransaction?
    suspend fun getAll(): List<RecurringTransaction>
    suspend fun deleteAll()
    suspend fun insertAll(recurring: List<RecurringTransaction>)
    suspend fun generateTransactions(recurringId: Long, startDate: String, endDate: String): List<Transaction>

    // Budget-scoped queries
    fun observeAllByBudget(budgetId: Long): Flow<List<RecurringTransaction>>
    fun observeActiveByBudget(budgetId: Long): Flow<List<RecurringTransaction>>
    suspend fun assignOrphanedToBudget(budgetId: Long): Int
}
