package com.budgetmanager.app.ui.screens.summary

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.domain.model.CategoryBreakdown
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.components.AmountSize
import com.budgetmanager.app.ui.components.AmountText
import com.budgetmanager.app.ui.components.EmptyStateView
import com.budgetmanager.app.ui.components.LoadingState
import com.budgetmanager.app.ui.theme.CornerRadius
import com.budgetmanager.app.ui.theme.LocalFinanceColors
import com.budgetmanager.app.ui.theme.Spacing
import com.budgetmanager.app.ui.viewmodel.MonthlySummaryViewModel
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private const val SUMMARY_ANIM_DURATION = 250

@Composable
fun MonthlySummaryScreen(
    viewModel: MonthlySummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg)
    ) {
        // ── Month navigation header ──
        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = { viewModel.previousMonth() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                )
            }

            Text(
                text = "${Month.of(uiState.month).getDisplayName(TextStyle.FULL, Locale.getDefault())} ${uiState.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            FilledIconButton(
                onClick = { viewModel.nextMonth() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Crossfade when month changes for a smooth content transition
        Crossfade(
            targetState = "${uiState.year}-${uiState.month}",
            animationSpec = tween(
                durationMillis = SUMMARY_ANIM_DURATION,
                easing = EaseInOutCubic
            ),
            label = "month-crossfade",
        ) { _ ->
            Column {
                when {
                    uiState.isLoading -> {
                        LoadingState(label = "Loading summary...")
                    }
                    uiState.summary == null -> {
                        EmptyStateView(
                            icon = Icons.Outlined.BarChart,
                            title = "No data for this month",
                            description = "Add transactions to see your monthly summary here.",
                        )
                    }
                    else -> {
                        val summary = uiState.summary!!

                        // ── Summary cards row ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            SummaryCard(
                                label = "Income",
                                amount = summary.totalIncome,
                                type = TransactionType.INCOME,
                                modifier = Modifier.weight(1f),
                            )
                            SummaryCard(
                                label = "Expenses",
                                amount = summary.totalExpenses,
                                type = TransactionType.EXPENSE,
                                modifier = Modifier.weight(1f),
                            )
                            SummaryCard(
                                label = "Balance",
                                amount = summary.netBalance,
                                type = if (summary.netBalance >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // ── Transaction count ──
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "${summary.transactionCount} transaction${if (summary.transactionCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = Spacing.xs),
                        )

                        Spacer(modifier = Modifier.height(Spacing.xl))

                        // ── Category breakdown ──
                        if (summary.categoryBreakdowns.isNotEmpty()) {
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))

                            val maxAmount = summary.categoryBreakdowns.maxOfOrNull { it.total } ?: 1.0

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                items(summary.categoryBreakdowns) { breakdown ->
                                    CategoryBreakdownItem(
                                        breakdown = breakdown,
                                        maxAmount = maxAmount,
                                    )
                                }

                                // Bottom spacing
                                item {
                                    Spacer(modifier = Modifier.height(Spacing.xxl))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual summary card for income, expenses, or balance.
 */
@Composable
private fun SummaryCard(
    label: String,
    amount: Double,
    type: TransactionType,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(CornerRadius.medium),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            AmountText(
                amount = amount,
                type = type,
                size = AmountSize.Small,
                showSign = label == "Balance",
            )
        }
    }
}

/**
 * Styled category breakdown row with name, count, amount, and relative progress bar.
 */
@Composable
private fun CategoryBreakdownItem(
    breakdown: CategoryBreakdown,
    maxAmount: Double,
) {
    val financeColors = LocalFinanceColors.current
    val targetFraction = if (maxAmount > 0) (breakdown.total / maxAmount).toFloat().coerceIn(0f, 1f) else 0f
    val fraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(
            durationMillis = 300,
            easing = EaseInOutCubic
        ),
        label = "category-bar-${breakdown.category}",
    )
    val barColor = when (breakdown.type) {
        TransactionType.EXPENSE -> financeColors.expense
        TransactionType.INCOME -> financeColors.income
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(CornerRadius.small),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = breakdown.category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${breakdown.count} transaction${if (breakdown.count != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                AmountText(
                    amount = breakdown.total,
                    type = breakdown.type,
                    size = AmountSize.Small,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Relative spend progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(CornerRadius.extraSmall))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(CornerRadius.extraSmall))
                        .background(barColor),
                )
            }
        }
    }
}
