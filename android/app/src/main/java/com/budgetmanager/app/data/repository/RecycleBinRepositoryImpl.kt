package com.budgetmanager.app.data.repository

import androidx.room.withTransaction
import com.budgetmanager.app.data.dao.DeletedTransactionDao
import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.db.BudgetDatabase
import com.budgetmanager.app.data.entity.DeletedTransactionEntity
import com.budgetmanager.app.data.entity.TransactionEntity
import com.budgetmanager.app.domain.model.DeletedTransaction
import com.budgetmanager.app.domain.model.Transaction
import com.budgetmanager.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecycleBinRepositoryImpl @Inject constructor(
    private val database: BudgetDatabase,
    private val transactionDao: TransactionDao,
    private val deletedDao: DeletedTransactionDao,
    private val recurringDao: RecurringTransactionDao
) : RecycleBinRepository {

    override fun observeByBudget(budgetId: Long): Flow<List<DeletedTransaction>> =
        deletedDao.observeByBudget(budgetId).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<DeletedTransaction>> =
        deletedDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun moveToBin(transactionId: Long): Boolean = database.withTransaction {
        val tx = transactionDao.getById(transactionId) ?: return@withTransaction false
        deletedDao.insert(tx.toDeleted(now()))
        transactionDao.deleteById(transactionId)
        true
    }

    override suspend fun restore(id: Long): Long? = database.withTransaction {
        val binned = deletedDao.getById(id) ?: return@withTransaction null
        // Keep the original id unless something else has taken it meanwhile
        // (e.g. after a backup import); 0 lets Room assign a fresh one.
        val targetId = if (transactionDao.getById(binned.id) == null) binned.id else 0L
        // The recurring definition may have been deleted while this was in the bin.
        val recurringId = binned.recurringId?.takeIf { recurringDao.getById(it) != null }
        val newId = transactionDao.insert(binned.toTransaction(targetId, recurringId))
        deletedDao.deleteById(id)
        newId
    }

    override suspend fun deleteForever(id: Long) = deletedDao.deleteById(id)

    override suspend fun emptyBin(budgetId: Long) = deletedDao.deleteByBudget(budgetId)

    override suspend fun purgeExpired(): Int {
        val cutoff = Instant.now()
            .minus(RecycleBinRepository.RETENTION_DAYS, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.SECONDS)
            .toString()
        return deletedDao.deleteOlderThan(cutoff)
    }

    /** UTC ISO-8601 (e.g. 2026-11-13T09:30:00Z), the same format the web app writes. */
    private fun now(): String =
        Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()

    private fun TransactionEntity.toDeleted(deletedAt: String) = DeletedTransactionEntity(
        id = id, type = type, amount = amount, category = category,
        description = description, date = date, createdAt = createdAt,
        recurringId = recurringId, budgetId = budgetId, deletedAt = deletedAt
    )

    private fun DeletedTransactionEntity.toTransaction(newId: Long, recurringId: Long?) = TransactionEntity(
        id = newId, type = type, amount = amount, category = category,
        description = description, date = date, createdAt = createdAt,
        recurringId = recurringId, budgetId = budgetId
    )

    private fun DeletedTransactionEntity.toDomain() = DeletedTransaction(
        transaction = Transaction(
            id = id,
            type = TransactionType.fromString(type),
            amount = amount,
            category = category,
            description = description,
            date = date,
            createdAt = createdAt,
            recurringId = recurringId,
            budgetId = budgetId
        ),
        deletedAt = deletedAt
    )
}
