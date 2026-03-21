package com.budgetmanager.app.domain.usecase

import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.entity.RecurringTransactionEntity
import com.budgetmanager.app.data.entity.TransactionEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared use case that generates transactions from active recurring definitions.
 *
 * The logic is idempotent — calling it multiple times on the same day is safe because
 * [TransactionDao.countByRecurringAndDate] prevents duplicate inserts.
 *
 * Used by both [com.budgetmanager.app.worker.RecurringTransactionWorker] (background)
 * and the UI layer (eager/synchronous on app open) so that recurring transactions are
 * always up-to-date when the user views them.
 */
@Singleton
class GenerateRecurringTransactionsUseCase @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * Generates transactions for today from all active recurring definitions.
     * Returns the number of transactions that were created.
     */
    suspend operator fun invoke(): Int {
        val today = LocalDate.now()
        val todayStr = today.format(DATE_FORMATTER)
        val activeRecurring = recurringTransactionDao.getActive()
        var createdCount = 0

        for (recurring in activeRecurring) {
            if (!shouldGenerateToday(recurring, today)) continue
            if (!isWithinDateRange(recurring, todayStr)) continue

            val alreadyExists = transactionDao.countByRecurringAndDate(recurring.id, todayStr) > 0
            if (alreadyExists) continue

            val transaction = TransactionEntity(
                type = recurring.type,
                amount = recurring.amount,
                category = recurring.category,
                description = recurring.description,
                date = todayStr,
                recurringId = recurring.id,
                budgetId = recurring.budgetId
            )
            transactionDao.insert(transaction)
            createdCount++
        }

        return createdCount
    }

    private fun shouldGenerateToday(
        recurring: RecurringTransactionEntity,
        today: LocalDate
    ): Boolean {
        return when (recurring.frequency) {
            "weekly" -> {
                val dayOfWeek = recurring.dayOfWeek ?: return false
                // dayOfWeek: 0=Monday .. 6=Sunday (matching Python's convention from the backend)
                // Java DayOfWeek: 1=Monday .. 7=Sunday
                today.dayOfWeek.value == dayOfWeek + 1
            }
            "monthly" -> {
                val dayOfMonth = recurring.dayOfMonth ?: return false
                val lastDay = today.lengthOfMonth()
                // If the recurring day exceeds the month's length, trigger on the last day
                if (dayOfMonth > lastDay) {
                    today.dayOfMonth == lastDay
                } else {
                    today.dayOfMonth == dayOfMonth
                }
            }
            else -> false
        }
    }

    private fun isWithinDateRange(recurring: RecurringTransactionEntity, todayStr: String): Boolean {
        if (recurring.startDate > todayStr) return false
        val endDate = recurring.endDate
        if (endDate != null && endDate < todayStr) return false
        return true
    }
}
