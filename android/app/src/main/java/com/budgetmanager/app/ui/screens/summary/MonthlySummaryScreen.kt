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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.domain.model.CategoryBreakdown
import com.budgetmanager.app.domain.model.MonthlySummary
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.components.EmptyStateView
import com.budgetmanager.app.ui.components.LoadingState
import com.budgetmanager.app.ui.theme.CornerRadius
import com.budgetmanager.app.ui.theme.LocalFinanceColors
import com.budgetmanager.app.ui.theme.Spacing
import com.budgetmanager.app.ui.viewmodel.MonthlySummaryViewModel
import java.text.NumberFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private const val SUMMARY_ANIM_DURATION = 300

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
        // ── 1. MONTH HERO SECTION ──
        Spacer(modifier = Modifier.height(Spacing.xl))

        MonthHeroSection(
            year = uiState.year,
            month = uiState.month,
            netBalance = uiState.summary?.netBalance,
            onPrevious = { viewModel.previousMonth() },
            onNext = { viewModel.nextMonth() },
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        // ── Crossfade content on month change ──
        Crossfade(
            targetState = "${uiState.year}-${uiState.month}",
            animationSpec = tween(
                durationMillis = SUMMARY_ANIM_DURATION,
                easing = EaseInOutCubic
            ),
            label = "month-crossfade",
        ) { _ ->
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
                    SummaryContent(summary = summary)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Month Hero Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthHeroSection(
    year: Int,
    month: Int,
    netBalance: Double?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val financeColors = LocalFinanceColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Month/Year with navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    modifier = Modifier.size(28.dp),
                )
            }

            Text(
                text = "${Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())} $year",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(
                onClick = onNext,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        // Net balance displayed prominently below month
        if (netBalance != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))

            val balanceColor = if (netBalance >= 0) {
                financeColors.balancePositive
            } else {
                financeColors.balanceNegative
            }
            val formatter = NumberFormat.getCurrencyInstance(Locale("he", "IL"))
            val sign = if (netBalance >= 0) "+" else ""
            val formattedBalance = "$sign${formatter.format(netBalance)}"

            Text(
                text = formattedBalance,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = balanceColor,
            )
            Text(
                text = "Net Balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary Content (Income/Expense cards + Category breakdown)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryContent(summary: MonthlySummary) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // ── 2. INCOME vs EXPENSE VISUAL ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                IncomeExpenseCard(
                    label = "Income",
                    amount = summary.totalIncome,
                    type = TransactionType.INCOME,
                    modifier = Modifier.weight(1f),
                )
                IncomeExpenseCard(
                    label = "Expenses",
                    amount = summary.totalExpenses,
                    type = TransactionType.EXPENSE,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── 3. CATEGORY BREAKDOWN ──
        if (summary.categoryBreakdowns.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "Spending by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            val maxAmount = summary.categoryBreakdowns.maxOfOrNull { it.total } ?: 1.0

            items(summary.categoryBreakdowns) { breakdown ->
                CategoryBreakdownRow(
                    breakdown = breakdown,
                    maxAmount = maxAmount,
                )
            }
        }

        // ── 4. Transaction count footer ──
        item {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "${summary.transactionCount} transaction${if (summary.transactionCount != 1) "s" else ""} this month",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Income / Expense Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IncomeExpenseCard(
    label: String,
    amount: Double,
    type: TransactionType,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val accentColor = when (type) {
        TransactionType.INCOME -> financeColors.income
        TransactionType.EXPENSE -> financeColors.expense
    }
    val tintedBackground = accentColor.copy(alpha = 0.08f)
    val icon = when (type) {
        TransactionType.INCOME -> Icons.Rounded.ArrowUpward
        TransactionType.EXPENSE -> Icons.Rounded.ArrowDownward
    }

    val formatter = NumberFormat.getCurrencyInstance(Locale("he", "IL"))
    val formattedAmount = formatter.format(amount)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = tintedBackground,
        ),
        shape = RoundedCornerShape(CornerRadius.medium),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Category Breakdown Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryBreakdownRow(
    breakdown: CategoryBreakdown,
    maxAmount: Double,
) {
    val targetFraction = if (maxAmount > 0) {
        (breakdown.total / maxAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(
            durationMillis = 400,
            easing = EaseInOutCubic,
        ),
        label = "category-bar-${breakdown.category}",
    )

    val formatter = NumberFormat.getCurrencyInstance(Locale("he", "IL"))
    val formattedAmount = formatter.format(breakdown.total)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
    ) {
        // Top row: category name + count on left, amount on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = breakdown.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "${breakdown.count} tx",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Progress bar showing relative spend
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(CornerRadius.extraSmall)),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeCap = StrokeCap.Round,
        )
    }
}
