package com.budgetmanager.app.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.components.AmountText
import com.budgetmanager.app.ui.components.AmountSize
import com.budgetmanager.app.ui.components.EmptyStateView
import com.budgetmanager.app.ui.components.FilterBar
import com.budgetmanager.app.ui.components.LoadingState
import com.budgetmanager.app.ui.components.SwipeToDeleteContainer
import com.budgetmanager.app.ui.components.TransactionCard
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutCubic
import com.budgetmanager.app.ui.theme.CornerRadius
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
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = LIST_ANIM_DURATION,
                        easing = EaseInOutCubic
                    )
                )
        ) {
            // Filter bar at top
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

            when {
                uiState.isLoading -> {
                    LoadingState(label = "Loading transactions...")
                }
                uiState.transactions.isEmpty() -> {
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
                            // Balance summary header
                            item {
                                BalanceSummaryRow(
                                    transactions = uiState.transactions,
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.lg,
                                        vertical = Spacing.sm
                                    )
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                            }

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
                                        TransactionCard(
                                            transaction = transaction,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = Spacing.lg,
                                                    vertical = Spacing.xs
                                                ),
                                            onClick = { onEditTransaction(transaction.id) }
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

/**
 * Horizontal row of three mini summary cards: Income, Expenses, and Net Balance.
 *
 * Computes totals from the current transaction list and uses semantic
 * [AmountText] coloring for each value.
 */
@Composable
private fun BalanceSummaryRow(
    transactions: List<com.budgetmanager.app.domain.model.Transaction>,
    modifier: Modifier = Modifier,
) {
    val totalIncome = transactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }
    val totalExpense = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    Row(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = LIST_ANIM_DURATION,
                    easing = EaseInOutCubic
                )
            ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Income card
        SummaryMiniCard(
            label = "Income",
            amount = totalIncome,
            type = TransactionType.INCOME,
            modifier = Modifier.weight(1f),
        )
        // Expense card
        SummaryMiniCard(
            label = "Expenses",
            amount = totalExpense,
            type = TransactionType.EXPENSE,
            modifier = Modifier.weight(1f),
        )
        // Balance card
        SummaryMiniCard(
            label = "Balance",
            amount = netBalance,
            type = if (netBalance >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
            modifier = Modifier.weight(1f),
            showSign = true,
        )
    }
}

@Composable
private fun SummaryMiniCard(
    label: String,
    amount: Double,
    type: TransactionType,
    modifier: Modifier = Modifier,
    showSign: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(CornerRadius.medium),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            AmountText(
                amount = amount,
                type = type,
                size = AmountSize.Small,
                showSign = showSign,
            )
        }
    }
}
