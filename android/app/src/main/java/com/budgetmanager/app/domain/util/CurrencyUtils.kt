package com.budgetmanager.app.domain.util

/**
 * Returns the display symbol for a given ISO 4217 currency code.
 * Falls back to the code itself for unsupported currencies.
 */
fun currencySymbol(code: String): String = when (code.uppercase()) {
    "ILS" -> "\u20AA"
    "USD" -> "$"
    "EUR" -> "\u20AC"
    "GBP" -> "\u00A3"
    "JPY" -> "\u00A5"
    "CHF" -> "CHF"
    "CAD" -> "CA$"
    "AUD" -> "A$"
    else -> code
}
