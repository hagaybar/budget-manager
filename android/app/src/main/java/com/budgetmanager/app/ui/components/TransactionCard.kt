package com.budgetmanager.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.domain.model.Transaction
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.theme.Elevation
import com.budgetmanager.app.ui.theme.LocalFinanceColors
import com.budgetmanager.app.ui.theme.Spacing

/**
 * Premium transaction card with semantic coloring and clean hierarchy.
 *
 * Layout:
 * ```
 * [ icon ]  Category  (recurring badge)        Amount
 *           Description (if any)                Date
 * ```
 *
 * Uses design system spacing tokens, FinanceColors for the type
 * indicator icon, and surfaceContainerLow for the card background.
 */
@Composable
fun TransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val financeColors = LocalFinanceColors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 250,
                    easing = EaseInOutCubic
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = Elevation.low,
        ),
        onClick = { onClick?.invoke() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // -- Type indicator icon --
            val iconTint = when (transaction.type) {
                TransactionType.INCOME -> financeColors.income
                TransactionType.EXPENSE -> financeColors.expense
            }
            Icon(
                imageVector = when (transaction.type) {
                    TransactionType.INCOME -> Icons.Default.ArrowUpward
                    TransactionType.EXPENSE -> Icons.Default.ArrowDownward
                },
                contentDescription = transaction.type.value,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            // -- Category + description --
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (transaction.recurringId != null) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                if (transaction.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            // -- Amount + date --
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                AmountText(
                    amount = transaction.amount,
                    type = transaction.type,
                    size = AmountSize.Small,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
