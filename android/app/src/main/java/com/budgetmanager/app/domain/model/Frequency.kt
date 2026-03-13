package com.budgetmanager.app.domain.model

enum class Frequency(val value: String) {
    WEEKLY("weekly"),
    MONTHLY("monthly");

    companion object {
        fun fromString(value: String): Frequency =
            entries.first { it.value == value }
    }
}
