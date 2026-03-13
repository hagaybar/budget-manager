package com.budgetmanager.app.data.repository

import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.entity.TransactionEntity
import com.budgetmanager.app.domain.model.CategoryBreakdown
import com.budgetmanager.app.domain.model.MonthlySummary
import com.budgetmanager.app.domain.model.Transaction
import com.budgetmanager.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeFiltered(
        type: String?,
        category: String?,
        dateFrom: String?,
        dateTo: String?
    ): Flow<List<Transaction>> =
        transactionDao.observeFiltered(type, category, dateFrom, dateTo)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Transaction?> =
        transactionDao.observeById(id).map { it?.toDomain() }

    override fun observeCategories(): Flow<List<String>> =
        transactionDao.observeCategories()

    override fun getMonthlySummary(year: Int, month: Int): Flow<MonthlySummary> {
        val startDate = String.format("%04d-%02d-01", year, month)
        val endDate = String.format("%04d-%02d-31", year, month)

        return combine(
            transactionDao.getMonthlyTotals(startDate, endDate),
            transactionDao.getMonthlyCategoryBreakdown(startDate, endDate)
        ) { totals, breakdowns ->
            MonthlySummary(
                year = year,
                month = month,
                totalIncome = totals.totalIncome,
                totalExpenses = totals.totalExpenses,
                netBalance = totals.totalIncome - totals.totalExpenses,
                transactionCount = totals.transactionCount,
                categoryBreakdowns = breakdowns.map { agg ->
                    CategoryBreakdown(
                        category = agg.category,
                        type = TransactionType.fromString(agg.type),
                        total = agg.total,
                        count = agg.count
                    )
                }
            )
        }
    }

    override suspend fun create(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())

    override suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction.toEntity())

    override suspend fun delete(id: Long) =
        transactionDao.deleteById(id)

    override suspend fun getById(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun getAll(): List<Transaction> =
        transactionDao.getAll().map { it.toDomain() }

    override suspend fun deleteAll() =
        transactionDao.deleteAll()

    override suspend fun insertAll(transactions: List<Transaction>) =
        transactionDao.insertAll(transactions.map { it.toEntity() })

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        type = TransactionType.fromString(type),
        amount = amount,
        category = category,
        description = description,
        date = date,
        createdAt = createdAt,
        recurringId = recurringId
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        type = type.value,
        amount = amount,
        category = category,
        description = description,
        date = date,
        createdAt = createdAt,
        recurringId = recurringId
    )
}
