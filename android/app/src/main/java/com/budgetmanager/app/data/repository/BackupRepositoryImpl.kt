package com.budgetmanager.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.db.BudgetDatabase
import com.budgetmanager.app.data.entity.RecurringTransactionEntity
import com.budgetmanager.app.data.entity.TransactionEntity
import com.budgetmanager.app.domain.model.BackupData
import com.budgetmanager.app.domain.model.BackupRecurringTransaction
import com.budgetmanager.app.domain.model.BackupTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BudgetDatabase,
    private val transactionDao: TransactionDao,
    private val recurringDao: RecurringTransactionDao
) : BackupRepository {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    override suspend fun exportToJson(): BackupData {
        val transactions = transactionDao.getAll()
        val recurring = recurringDao.getAll()

        return BackupData(
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            transactions = transactions.map { it.toBackup() },
            recurringTransactions = recurring.map { it.toBackup() }
        )
    }

    override suspend fun importFromJson(data: BackupData) {
        database.withTransaction {
            transactionDao.deleteAll()
            recurringDao.deleteAll()

            recurringDao.insertAll(data.recurringTransactions.map { it.toEntity() })
            transactionDao.insertAll(data.transactions.map { it.toEntity() })
        }
    }

    override suspend fun exportToUri(uri: Uri) {
        val data = exportToJson()
        val jsonString = json.encodeToString(BackupData.serializer(), data)
        val stream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Could not open file for writing")
        stream.use { it.write(jsonString.toByteArray()) }
    }

    override suspend fun importFromUri(uri: Uri) {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw IllegalStateException("Could not read file")

        // Validate JSON structure BEFORE wiping any data
        val data = json.decodeFromString(BackupData.serializer(), jsonString)
        validateBackupData(data)

        // Only wipe and import after successful validation
        importFromJson(data)
    }

    /**
     * Validates that the parsed backup data contains required fields and
     * reasonable values before we proceed to wipe existing data.
     */
    private fun validateBackupData(data: BackupData) {
        // Ensure the createdAt timestamp is present
        require(data.createdAt.isNotBlank()) {
            "Backup file is missing a creation timestamp."
        }

        // Validate each transaction has required non-blank fields
        data.transactions.forEach { tx ->
            require(tx.type.isNotBlank()) { "Transaction missing type field." }
            require(tx.category.isNotBlank()) { "Transaction missing category field." }
            require(tx.date.isNotBlank()) { "Transaction missing date field." }
            require(tx.amount > 0) { "Transaction has invalid amount." }
        }

        // Validate each recurring transaction has required non-blank fields
        data.recurringTransactions.forEach { rtx ->
            require(rtx.type.isNotBlank()) { "Recurring transaction missing type field." }
            require(rtx.category.isNotBlank()) { "Recurring transaction missing category field." }
            require(rtx.frequency.isNotBlank()) { "Recurring transaction missing frequency field." }
            require(rtx.startDate.isNotBlank()) { "Recurring transaction missing start date." }
            require(rtx.amount > 0) { "Recurring transaction has invalid amount." }
        }
    }

    private fun TransactionEntity.toBackup() = BackupTransaction(
        id = id, type = type, amount = amount, category = category,
        description = description, date = date, createdAt = createdAt,
        recurringId = recurringId
    )

    private fun RecurringTransactionEntity.toBackup() = BackupRecurringTransaction(
        id = id, type = type, amount = amount, category = category,
        description = description, frequency = frequency, dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth, startDate = startDate, endDate = endDate,
        isActive = isActive, createdAt = createdAt
    )

    private fun BackupTransaction.toEntity() = TransactionEntity(
        id = id, type = type, amount = amount, category = category,
        description = description, date = date, createdAt = createdAt,
        recurringId = recurringId
    )

    private fun BackupRecurringTransaction.toEntity() = RecurringTransactionEntity(
        id = id, type = type, amount = amount, category = category,
        description = description, frequency = frequency, dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth, startDate = startDate, endDate = endDate,
        isActive = isActive, createdAt = createdAt
    )
}
