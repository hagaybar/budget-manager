package com.budgetmanager.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.budgetmanager.app.domain.usecase.GenerateRecurringTransactionsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val generateRecurringTransactions: GenerateRecurringTransactionsUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "recurring_transaction_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            generateRecurringTransactions()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
