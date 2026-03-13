package com.budgetmanager.app.data.repository

import com.budgetmanager.app.domain.model.MonthlySummary
import com.budgetmanager.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeFiltered(type: String?, category: String?, dateFrom: String?, dateTo: String?): Flow<List<Transaction>>
    fun observeById(id: Long): Flow<Transaction?>
    fun observeCategories(): Flow<List<String>>
    fun getMonthlySummary(year: Int, month: Int): Flow<MonthlySummary>
    suspend fun create(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): Transaction?
    suspend fun getAll(): List<Transaction>
    suspend fun deleteAll()
    suspend fun insertAll(transactions: List<Transaction>)
}
