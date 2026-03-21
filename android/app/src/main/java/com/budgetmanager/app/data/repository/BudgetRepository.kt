package com.budgetmanager.app.data.repository

import com.budgetmanager.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>
    fun observeById(id: Long): Flow<Budget?>
    fun observeActive(): Flow<Budget?>
    suspend fun create(budget: Budget): Long
    suspend fun update(budget: Budget)
    suspend fun delete(id: Long)
    suspend fun getActiveBudget(): Budget?
    suspend fun setActiveBudget(id: Long)
    suspend fun getAll(): List<Budget>
    suspend fun deleteAll()
    suspend fun insertAll(budgets: List<Budget>)
}
