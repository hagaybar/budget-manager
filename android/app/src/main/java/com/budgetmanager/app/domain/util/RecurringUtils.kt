package com.budgetmanager.app.domain.util

import com.budgetmanager.app.domain.model.Frequency
import com.budgetmanager.app.domain.model.RecurringTransaction
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calculates the next occurrence date for a recurring transaction.
 * Returns null if the transaction is inactive.
 * If today matches the schedule, returns today.
 */
fun getNextOccurrence(recurring: RecurringTransaction): LocalDate? {
    if (!recurring.isActive) return null

    val today = LocalDate.now()

    return when (recurring.frequency) {
        Frequency.MONTHLY -> {
            val dayOfMonth = recurring.dayOfMonth ?: return null
            getNextMonthlyOccurrence(today, dayOfMonth)
        }
        Frequency.WEEKLY -> {
            val dayOfWeek = recurring.dayOfWeek ?: return null
            getNextWeeklyOccurrence(today, dayOfWeek)
        }
    }
}

private fun getNextMonthlyOccurrence(today: LocalDate, dayOfMonth: Int): LocalDate {
    // Try current month first
    val clampedDayThisMonth = minOf(dayOfMonth, YearMonth.of(today.year, today.month).lengthOfMonth())
    val thisMonthDate = today.withDayOfMonth(clampedDayThisMonth)

    if (!thisMonthDate.isBefore(today)) {
        return thisMonthDate
    }

    // Move to next month
    val nextMonth = today.plusMonths(1)
    val clampedDayNextMonth = minOf(dayOfMonth, YearMonth.of(nextMonth.year, nextMonth.month).lengthOfMonth())
    return nextMonth.withDayOfMonth(clampedDayNextMonth)
}

private fun getNextWeeklyOccurrence(today: LocalDate, dayOfWeek: Int): LocalDate {
    // dayOfWeek: Monday=0, Tuesday=1, ... Sunday=6
    // DayOfWeek.of: Monday=1, Tuesday=2, ... Sunday=7
    val targetDayOfWeek = DayOfWeek.of(dayOfWeek + 1)
    val todayDayOfWeek = today.dayOfWeek

    val daysUntil = (targetDayOfWeek.value - todayDayOfWeek.value + 7) % 7
    return today.plusDays(daysUntil.toLong())
}

/**
 * Formats a date for display as next occurrence.
 * Same year as today: "Mar 15"
 * Different year: "Mar 15, 2027"
 */
fun formatNextOccurrence(date: LocalDate): String {
    val today = LocalDate.now()
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    val day = date.dayOfMonth
    return if (date.year == today.year) {
        "$month $day"
    } else {
        "$month $day, ${date.year}"
    }
}
