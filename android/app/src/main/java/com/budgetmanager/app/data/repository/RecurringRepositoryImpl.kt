package com.budgetmanager.app.data.repository

import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.entity.RecurringTransactionEntity
import com.budgetmanager.app.data.entity.TransactionEntity
import com.budgetmanager.app.domain.model.Frequency
import com.budgetmanager.app.domain.model.RecurringTransaction
import com.budgetmanager.app.domain.model.Transaction
import com.budgetmanager.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val recurringDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao
) : RecurringRepository {

    override fun observeAll(): Flow<List<RecurringTransaction>> =
        recurringDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeActive(): Flow<List<RecurringTransaction>> =
        recurringDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<RecurringTransaction?> =
        recurringDao.observeById(id).map { it?.toDomain() }

    override suspend fun create(recurring: RecurringTransaction): Long =
        recurringDao.insert(recurring.toEntity())

    override suspend fun update(recurring: RecurringTransaction) =
        recurringDao.update(recurring.toEntity())

    override suspend fun delete(id: Long) {
        transactionDao.nullifyRecurringId(id)
        recurringDao.deleteById(id)
    }

    override suspend fun getById(id: Long): RecurringTransaction? =
        recurringDao.getById(id)?.toDomain()

    override suspend fun getAll(): List<RecurringTransaction> =
        recurringDao.getAll().map { it.toDomain() }

    override suspend fun deleteAll() =
        recurringDao.deleteAll()

    override suspend fun insertAll(recurring: List<RecurringTransaction>) =
        recurringDao.insertAll(recurring.map { it.toEntity() })

    override suspend fun generateTransactions(
        recurringId: Long,
        startDate: String,
        endDate: String
    ): List<Transaction> {
        val recurring = recurringDao.getById(recurringId) ?: return emptyList()
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        val generated = mutableListOf<Transaction>()

        var current = start
        while (!current.isAfter(end)) {
            val shouldGenerate = when (recurring.frequency) {
                "weekly" -> {
                    val targetDay = DayOfWeek.of((recurring.dayOfWeek ?: 0) + 1)
                    current.dayOfWeek == targetDay
                }
                "monthly" -> current.dayOfMonth == (recurring.dayOfMonth ?: 1)
                else -> false
            }

            if (shouldGenerate) {
                val dateStr = current.toString()
                val exists = transactionDao.countByRecurringAndDate(recurringId, dateStr) > 0
                if (!exists) {
                    val entity = TransactionEntity(
                        type = recurring.type,
                        amount = recurring.amount,
                        category = recurring.category,
                        description = recurring.description,
                        date = dateStr,
                        recurringId = recurringId
                    )
                    val id = transactionDao.insert(entity)
                    generated.add(
                        Transaction(
                            id = id,
                            type = TransactionType.fromString(recurring.type),
                            amount = recurring.amount,
                            category = recurring.category,
                            description = recurring.description,
                            date = dateStr,
                            recurringId = recurringId
                        )
                    )
                }
            }
            current = current.plusDays(1)
        }
        return generated
    }

    private fun RecurringTransactionEntity.toDomain() = RecurringTransaction(
        id = id,
        type = TransactionType.fromString(type),
        amount = amount,
        category = category,
        description = description,
        frequency = Frequency.fromString(frequency),
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        startDate = startDate,
        endDate = endDate,
        isActive = isActive == 1,
        createdAt = createdAt
    )

    private fun RecurringTransaction.toEntity() = RecurringTransactionEntity(
        id = id,
        type = type.value,
        amount = amount,
        category = category,
        description = description,
        frequency = frequency.value,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        startDate = startDate,
        endDate = endDate,
        isActive = if (isActive) 1 else 0,
        createdAt = createdAt
    )
}
