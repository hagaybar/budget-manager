package com.budgetmanager.app.ui.screens.recyclebin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.data.repository.RecycleBinRepository
import com.budgetmanager.app.domain.model.DeletedTransaction
import com.budgetmanager.app.domain.model.Transaction
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.components.EmptyStateView
import com.budgetmanager.app.ui.components.LoadingState
import com.budgetmanager.app.ui.theme.LocalFinanceColors
import com.budgetmanager.app.ui.theme.Spacing
import com.budgetmanager.app.ui.viewmodel.RecycleBinViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    uiState.pendingDeleteForever?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Delete forever?") },
            text = {
                Text(
                    "${formatAmount(item.transaction)} · ${item.transaction.category} " +
                        "(${item.transaction.date}) will be permanently deleted. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteForever() }) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) { Text("Cancel") }
            }
        )
    }

    if (uiState.showEmptyBinConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Empty Recycle Bin?") },
            text = {
                Text(
                    "All ${uiState.items.size} transaction(s) in the Recycle Bin will be " +
                        "permanently deleted. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmEmptyBin() }) {
                    Text("Empty bin", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        TextButton(onClick = { viewModel.requestEmptyBin() }) {
                            Text("Empty bin", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(padding), label = "Loading...")
            }
            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    EmptyStateView(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "The Recycle Bin is empty",
                        description = "Deleted transactions are kept here for " +
                            "${RecycleBinRepository.RETENTION_DAYS} days."
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    item {
                        Text(
                            text = "Deleted transactions are kept here for " +
                                "${RecycleBinRepository.RETENTION_DAYS} days, then removed permanently.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                        )
                    }
                    items(uiState.items, key = { it.transaction.id }) { item ->
                        RecycleBinItem(
                            item = item,
                            onRestore = { viewModel.restore(item) },
                            onDeleteForever = { viewModel.requestDeleteForever(item) },
                            modifier = Modifier.padding(horizontal = Spacing.lg)
                        )
                    }
                    item { Spacer(modifier = Modifier.padding(bottom = Spacing.xl)) }
                }
            }
        }
    }
}

@Composable
private fun RecycleBinItem(
    item: DeletedTransaction,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val tx = item.transaction
    val color = if (tx.type == TransactionType.INCOME) financeColors.income else financeColors.expense

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.category,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tx.description.isNotBlank()) {
                        Text(
                            text = tx.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${tx.date} · deleted ${formatDeletedAt(item.deletedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = formatAmount(tx),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.padding(top = Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)
            ) {
                TextButton(onClick = onDeleteForever) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Restore")
                }
            }
        }
    }
}

private fun formatAmount(tx: Transaction): String {
    val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
    return "$sign₪${String.format("%.2f", tx.amount)}"
}

private val DELETED_AT_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** Accepts UTC instants (2026-11-13T09:30:00Z) and, defensively, local date-times. */
private fun formatDeletedAt(value: String): String = try {
    Instant.parse(value).atZone(ZoneId.systemDefault()).format(DELETED_AT_FORMAT)
} catch (_: Exception) {
    try {
        LocalDateTime.parse(value).format(DELETED_AT_FORMAT)
    } catch (_: Exception) {
        value.take(10)
    }
}
