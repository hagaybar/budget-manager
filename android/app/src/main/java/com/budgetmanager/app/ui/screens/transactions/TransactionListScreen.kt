package com.budgetmanager.app.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.domain.model.Transaction
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.components.EmptyStateView
import com.budgetmanager.app.ui.components.FilterBar
import com.budgetmanager.app.ui.components.LoadingState
import com.budgetmanager.app.ui.components.SwipeToDeleteContainer
import com.budgetmanager.app.ui.theme.CornerRadius
import com.budgetmanager.app.ui.theme.LocalFinanceColors
import com.budgetmanager.app.ui.theme.Spacing
import com.budgetmanager.app.ui.viewmodel.TransactionListViewModel

private const val LIST_ANIM_DURATION = 250

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(CornerRadius.extraLarge),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState(label = "Loading transactions...")
                }
                uiState.transactions.isEmpty() -> {
                    // Show filter bar even when empty so user can clear filters
                    FilterBar(
                        selectedType = uiState.filterType,
                        selectedCategory = uiState.filterCategory,
                        categories = uiState.availableCategories,
                        onTypeSelected = { type ->
                            viewModel.setFilter(
                                type = type,
                                category = uiState.filterCategory,
                                dateFrom = uiState.filterDateFrom,
                                dateTo = uiState.filterDateTo
                            )
                        },
                        onCategorySelected = { category ->
                            viewModel.setFilter(
                                type = uiState.filterType,
                                category = category,
                                dateFrom = uiState.filterDateFrom,
                                dateTo = uiState.filterDateTo
                            )
                        }
                    )
                    EmptyStateView(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "No transactions yet",
                        description = "Tap + to add your first transaction\nand start tracking your budget.",
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(pullToRefreshState.nestedScrollConnection)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // ── Hero Balance Card ────────────────────────────
                            item {
                                HeroBalanceCard(
                                    transactions = uiState.transactions,
                                    modifier = Modifier.padding(
                                        start = Spacing.lg,
                                        end = Spacing.lg,
                                        top = Spacing.lg,
                                        bottom = Spacing.sm
                                    )
                                )
                            }

                            // ── Filter Bar (compact) ─────────────────────────
                            item {
                                FilterBar(
                                    selectedType = uiState.filterType,
                                    selectedCategory = uiState.filterCategory,
                                    categories = uiState.availableCategories,
                                    onTypeSelected = { type ->
                                        viewModel.setFilter(
                                            type = type,
                                            category = uiState.filterCategory,
                                            dateFrom = uiState.filterDateFrom,
                                            dateTo = uiState.filterDateTo
                                        )
                                    },
                                    onCategorySelected = { category ->
                                        viewModel.setFilter(
                                            type = uiState.filterType,
                                            category = category,
                                            dateFrom = uiState.filterDateFrom,
                                            dateTo = uiState.filterDateTo
                                        )
                                    }
                                )
                            }

                            // ── Section Header ───────────────────────────────
                            item {
                                Text(
                                    text = "Recent Transactions",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.lg,
                                        vertical = Spacing.sm
                                    )
                                )
                            }

                            // ── Transaction Rows ─────────────────────────────
                            items(uiState.transactions, key = { it.id }) { transaction ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { visible = true }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(
                                        animationSpec = tween(
                                            durationMillis = LIST_ANIM_DURATION,
                                            easing = EaseInOutCubic
                                        )
                                    ) + slideInVertically(
                                        animationSpec = tween(
                                            durationMillis = LIST_ANIM_DURATION,
                                            easing = EaseInOutCubic
                                        ),
                                        initialOffsetY = { it / 4 }
                                    ),
                                ) {
                                    SwipeToDeleteContainer(
                                        onDelete = { viewModel.deleteTransaction(transaction.id) }
                                    ) {
                                        TransactionRow(
                                            transaction = transaction,
                                            onClick = { onEditTransaction(transaction.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Bottom spacing so FAB doesn't cover last item
                            item {
                                Spacer(modifier = Modifier.height(Spacing.xxxl + Spacing.xxl))
                            }
                        }

                        PullToRefreshContainer(
                            state = pullToRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Hero Balance Card — the first and biggest thing the user sees
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HeroBalanceCard(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current

    val totalIncome = transactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }
    val totalExpense = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val balanceColor = if (netBalance >= 0) {
        financeColors.balancePositive
    } else {
        financeColors.balanceNegative
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(CornerRadius.medium),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // "Current Balance" label
            Text(
                text = "Current Balance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Large balance number — the hero element
            val sign = if (netBalance >= 0) "" else "-"
            val absBalance = kotlin.math.abs(netBalance)
            Text(
                text = "${sign}\u20AA${String.format("%.2f", absBalance)}",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    letterSpacing = (-0.5).sp,
                ),
                color = balanceColor,
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Income / Expense summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Income total
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\u25B2",
                        color = financeColors.income,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Column {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "\u20AA${String.format("%.2f", totalIncome)}",
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                            ),
                            color = financeColors.income,
                        )
                    }
                }

                // Subtle vertical divider
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Expense total
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\u25BC",
                        color = financeColors.expense,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Column {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "\u20AA${String.format("%.2f", totalExpense)}",
                            style = TextStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                            ),
                            color = financeColors.expense,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Transaction Row — clean, lightweight row with colored dot indicator
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current

    val dotColor = when (transaction.type) {
        TransactionType.INCOME -> financeColors.income
        TransactionType.EXPENSE -> financeColors.expense
    }

    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> financeColors.income
        TransactionType.EXPENSE -> financeColors.expense
    }

    val amountPrefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
    }

    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colored dot indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            // Category + description (center)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (transaction.description.isNotBlank()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            // Amount + date (right-aligned)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix\u20AA${String.format("%.2f", transaction.amount)}",
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = amountColor,
                )
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        // Divider between items
        Divider(
            modifier = Modifier.padding(start = Spacing.lg + 10.dp + Spacing.md),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
        )
    }
}
