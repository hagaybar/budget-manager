package com.budgetmanager.app.domain.usecase

import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.entity.RecurringTransactionEntity
import com.budgetmanager.app.data.entity.TransactionEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared use case that generates transactions from active recurring definitions.
 *
 * For each active recurring definition, this enumerates every scheduled occurrence
 * in the backfill window
 *     [max(recurring.startDate, today - MAX_BACKFILL_DAYS),
 *      min(today, recurring.endDate)]
 * and inserts any that do not already exist. Backfilling ensures that a scheduled
 * day which was missed — because the device was off, WorkManager batched the job
 * past midnight, Doze mode deferred the worker, or the user simply did not open
 * the app that day — is still materialized the next time the worker runs or the
 * user opens the app.
 *
 * The operation is idempotent: [TransactionDao.countByRecurringAndDate] prevents
 * duplicate inserts, so calling it multiple times on the same day is safe.
 *
 * Used by both [com.budgetmanager.app.worker.RecurringTransactionWorker] (background)
 * and the UI layer (eager/synchronous on app open).
 */
@Singleton
class GenerateRecurringTransactionsUseCase @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

        /**
         * Maximum number of days to look back when backfilling missed occurrences.
         * Bounds the blast radius in pathological cases (a device that has been off
         * for months, a long uninstall, or a very old start_date) so we do not
         * flood the user with hundreds of transactions the first time the app
         * reopens.
         */
        const val MAX_BACKFILL_DAYS = 90L
    }

    /**
     * Generates any pending transactions for each active recurring definition,
     * including those whose scheduled day was missed within the backfill window.
     * Returns the total number of transactions created.
     */
    suspend operator fun invoke(): Int = execute(LocalDate.now())

    /**
     * Visible for testing: runs the generation pipeline against a caller-supplied
     * [today] so tests can exercise backfill behaviour at a fixed date without
     * having to mock JDK statics.
     */
    internal suspend fun execute(today: LocalDate): Int {
        val activeRecurring = recurringTransactionDao.getActive()
        var createdCount = 0

        for (recurring in activeRecurring) {
            createdCount += generateForRecurring(recurring, today)
        }

        return createdCount
    }

    /**
     * Enumerates all due occurrences for a single recurring definition within
     * the backfill window and inserts any that do not already exist. Returns
     * the number of transactions created for this definition.
     */
    private suspend fun generateForRecurring(
        recurring: RecurringTransactionEntity,
        today: LocalDate
    ): Int {
        val recStart = parseDateOrNull(recurring.startDate) ?: return 0
        val recEnd = recurring.endDate?.let { parseDateOrNull(it) }

        val windowStart = maxOf(recStart, today.minusDays(MAX_BACKFILL_DAYS))
        val windowEnd = if (recEnd != null) minOf(today, recEnd) else today

        if (windowStart.isAfter(windowEnd)) return 0

        val occurrences = enumerateOccurrences(recurring, windowStart, windowEnd)

        var created = 0
        for (occurrence in occurrences) {
            val dateStr = occurrence.format(DATE_FORMATTER)
            val alreadyExists = transactionDao.countByRecurringAndDate(recurring.id, dateStr) > 0
            if (alreadyExists) continue

            // Before inserting, look for an orphan (recurring_id IS NULL) that
            // matches this recurring's value pattern on this date. Such rows
            // exist when older bugs stripped the FK — most commonly the
            // edit-transaction flow that didn't preserve recurringId, but also
            // ON DELETE SET NULL when a recurring was deleted and re-added.
            // Relinking the orphan keeps the user's history intact and prevents
            // a duplicate from being created on every subsequent app open.
            val orphanId = transactionDao.findOrphanIdByValueOnDate(
                date = dateStr,
                type = recurring.type,
                amount = recurring.amount,
                category = recurring.category,
                budgetId = recurring.budgetId
            )
            if (orphanId != null) {
                transactionDao.setRecurringId(orphanId, recurring.id)
                continue
            }

            val transaction = TransactionEntity(
                type = recurring.type,
                amount = recurring.amount,
                category = recurring.category,
                description = recurring.description,
                date = dateStr,
                recurringId = recurring.id,
                budgetId = recurring.budgetId
            )
            transactionDao.insert(transaction)
            created++
        }
        return created
    }

    /**
     * Returns every scheduled occurrence date for [recurring] in
     * [[windowStart], [windowEnd]], inclusive. Respects the frequency convention:
     *   - weekly:  dayOfWeek 0=Monday..6=Sunday
     *   - monthly: dayOfMonth is clamped to the last day of each month when it
     *              overflows (e.g. 31 becomes 30 in April, 28/29 in February).
     */
    private fun enumerateOccurrences(
        recurring: RecurringTransactionEntity,
        windowStart: LocalDate,
        windowEnd: LocalDate
    ): List<LocalDate> {
        return when (recurring.frequency) {
            "weekly" -> {
                val dayOfWeek = recurring.dayOfWeek ?: return emptyList()
                // 0=Mon..6=Sun (app/Python convention) -> 1=Mon..7=Sun (java.time)
                val targetDow = dayOfWeek + 1
                val daysUntil = (targetDow - windowStart.dayOfWeek.value + 7) % 7
                var current = windowStart.plusDays(daysUntil.toLong())
                val result = mutableListOf<LocalDate>()
                while (!current.isAfter(windowEnd)) {
                    result.add(current)
                    current = current.plusWeeks(1)
                }
                result
            }
            "monthly" -> {
                val dayOfMonth = recurring.dayOfMonth ?: return emptyList()
                val result = mutableListOf<LocalDate>()
                var ym = YearMonth.from(windowStart)
                val endYm = YearMonth.from(windowEnd)
                while (!ym.isAfter(endYm)) {
                    val actualDay = minOf(dayOfMonth, ym.lengthOfMonth())
                    val occurrence = ym.atDay(actualDay)
                    if (!occurrence.isBefore(windowStart) && !occurrence.isAfter(windowEnd)) {
                        result.add(occurrence)
                    }
                    ym = ym.plusMonths(1)
                }
                result
            }
            else -> emptyList()
        }
    }

    private fun parseDateOrNull(iso: String): LocalDate? = try {
        LocalDate.parse(iso, DATE_FORMATTER)
    } catch (e: Exception) {
        null
    }
}
