package com.budgetmanager.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.theme.AmountDisplay
import com.budgetmanager.app.ui.theme.AmountDisplayLarge
import com.budgetmanager.app.ui.theme.AmountSmall
import com.budgetmanager.app.ui.theme.LocalFinanceColors
import java.text.NumberFormat
import java.util.Locale

/**
 * Predefined size variants for [AmountText].
 *
 * Each variant maps to one of the amount-specific [TextStyle] values
 * defined in Type.kt, optimized for numeric readability.
 */
enum class AmountSize {
    /** 28sp Bold — large summary totals */
    Large,
    /** 20sp SemiBold — standard card amounts (default) */
    Medium,
    /** 14sp Medium — inline / secondary amounts */
    Small,
}

/**
 * Semantic currency display composable.
 *
 * Automatically colors income amounts with [FinanceColors.income] (green)
 * and expense amounts with [FinanceColors.expense] (red) via the
 * [LocalFinanceColors] CompositionLocal — correctly adapting to
 * light, dark, and dynamic themes.
 *
 * @param amount    numeric value to format and display
 * @param type      [TransactionType.INCOME] or [TransactionType.EXPENSE]
 * @param modifier  outer modifier
 * @param size      controls the text style variant ([AmountSize])
 * @param style     override text style (takes precedence over [size])
 * @param showSign  whether to prefix + or - before the formatted amount
 */
@Composable
fun AmountText(
    amount: Double,
    type: TransactionType,
    modifier: Modifier = Modifier,
    size: AmountSize = AmountSize.Medium,
    style: TextStyle? = null,
    showSign: Boolean = true,
) {
    val financeColors = LocalFinanceColors.current

    val formatter = NumberFormat.getCurrencyInstance(Locale("he", "IL"))
    val formattedAmount = formatter.format(amount)

    val prefix = when {
        !showSign -> ""
        type == TransactionType.INCOME -> "+"
        else -> "-"
    }

    val color = when (type) {
        TransactionType.INCOME -> financeColors.income
        TransactionType.EXPENSE -> financeColors.expense
    }

    val textStyle = style ?: when (size) {
        AmountSize.Large -> AmountDisplayLarge
        AmountSize.Medium -> AmountDisplay
        AmountSize.Small -> AmountSmall
    }

    Text(
        text = "$prefix$formattedAmount",
        color = color,
        style = textStyle,
        modifier = modifier,
    )
}
