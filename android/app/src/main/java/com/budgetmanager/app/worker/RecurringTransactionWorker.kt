package com.budgetmanager.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.entity.RecurringTransactionEntity
import com.budgetmanager.app.data.entity.TransactionEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "recurring_transaction_worker"
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            val todayStr = today.format(DATE_FORMATTER)
            val activeRecurring = recurringTransactionDao.getActive()

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
                    recurringId = recurring.id
                )
                transactionDao.insert(transaction)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
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
