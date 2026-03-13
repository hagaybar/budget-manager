package com.budgetmanager.app.ui.screens.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.ui.components.EmptyStateView
import com.budgetmanager.app.ui.components.FilterBar
import com.budgetmanager.app.ui.components.SwipeToDeleteContainer
import com.budgetmanager.app.ui.components.TransactionCard
import com.budgetmanager.app.ui.viewmodel.TransactionListViewModel

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
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.transactions.isEmpty() -> {
                    EmptyStateView(message = "No transactions yet.\nTap + to add one.")
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(pullToRefreshState.nestedScrollConnection)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.transactions, key = { it.id }) { transaction ->
                                SwipeToDeleteContainer(
                                    onDelete = { viewModel.deleteTransaction(transaction.id) }
                                ) {
                                    TransactionCard(
                                        transaction = transaction,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                        onClick = { onEditTransaction(transaction.id) }
                                    )
                                }
                            }
                        }

                        PullToRefreshContainer(
                            state = pullToRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }
}
