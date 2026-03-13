package com.budgetmanager.app.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.theme.ExpenseRed
import com.budgetmanager.app.ui.theme.ExpenseRedDark
import com.budgetmanager.app.ui.theme.IncomeGreen
import com.budgetmanager.app.ui.theme.IncomeGreenDark
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AmountText(
    amount: Double,
    type: TransactionType,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    showSign: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val formatter = NumberFormat.getCurrencyInstance(Locale("he", "IL"))
    val formattedAmount = formatter.format(amount)
    val prefix = when {
        !showSign -> ""
        type == TransactionType.INCOME -> "+"
        else -> "-"
    }
    val color = when (type) {
        TransactionType.INCOME -> if (isDark) IncomeGreenDark else IncomeGreen
        TransactionType.EXPENSE -> if (isDark) ExpenseRedDark else ExpenseRed
    }

    Text(
        text = "$prefix$formattedAmount",
        color = color,
        style = style,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}
