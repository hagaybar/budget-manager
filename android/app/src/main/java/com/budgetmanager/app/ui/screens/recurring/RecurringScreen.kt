package com.budgetmanager.app.ui.screens.recurring

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.domain.model.Frequency
import com.budgetmanager.app.domain.model.RecurringTransaction
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.domain.util.formatNextOccurrence
import com.budgetmanager.app.domain.util.getNextOccurrence
import com.budgetmanager.app.ui.components.AmountText
import com.budgetmanager.app.ui.components.EmptyStateView
import com.budgetmanager.app.ui.viewmodel.RecurringViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var viewingRecurring by remember { mutableStateOf<RecurringTransaction?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.recurringTransactions.isEmpty() && !uiState.showAddDialog -> {
                EmptyStateView(message = "No recurring transactions.\nTap + to add one.")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    items(uiState.recurringTransactions, key = { it.id }) { recurring ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewingRecurring = recurring }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(recurring.category, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${recurring.frequency.value} - ${recurring.type.value}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (recurring.description.isNotBlank()) {
                                        Text(recurring.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                    AmountText(amount = recurring.amount, type = recurring.type)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val nextDate = getNextOccurrence(recurring)
                                    if (nextDate != null) {
                                        Text(
                                            text = "Next: ${formatNextOccurrence(nextDate)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "Paused",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = recurring.isActive,
                                    onCheckedChange = { viewModel.toggleActive(recurring.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // View Detail Dialog
    viewingRecurring?.let { recurring ->
        ViewRecurringDialog(
            recurring = recurring,
            onDismiss = { viewingRecurring = null },
            onEdit = {
                viewingRecurring = null
                viewModel.showEditDialog(recurring)
            },
            onDelete = {
                viewingRecurring = null
                viewModel.delete(recurring.id)
            }
        )
    }

    // Add Dialog
    if (uiState.showAddDialog) {
        RecurringFormDialog(
            title = "Add Recurring Transaction",
            existing = null,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { recurring -> viewModel.create(recurring) }
        )
    }

    // Edit Dialog
    uiState.editingRecurring?.let { recurring ->
        RecurringFormDialog(
            title = "Edit Recurring Transaction",
            existing = recurring,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { updated -> viewModel.update(updated) }
        )
    }
}

@Composable
fun ViewRecurringDialog(
    recurring: RecurringTransaction,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(recurring.category, modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Type", recurring.type.value.replaceFirstChar { it.uppercase() })
                DetailRow("Amount", "₪ ${"%.2f".format(recurring.amount)}")
                DetailRow("Frequency", recurring.frequency.value.replaceFirstChar { it.uppercase() })
                if (recurring.frequency == Frequency.MONTHLY && recurring.dayOfMonth != null) {
                    DetailRow("Day of Month", recurring.dayOfMonth.toString())
                }
                if (recurring.frequency == Frequency.WEEKLY && recurring.dayOfWeek != null) {
                    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    DetailRow("Day of Week", dayNames.getOrElse(recurring.dayOfWeek) { "Unknown" })
                }
                if (recurring.description.isNotBlank()) {
                    DetailRow("Description", recurring.description)
                }
                DetailRow("Start Date", recurring.startDate)
                DetailRow("Status", if (recurring.isActive) "Active" else "Inactive")
                val nextDate = getNextOccurrence(recurring)
                if (nextDate != null) {
                    DetailRow("Next Occurrence", formatNextOccurrence(nextDate))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Recurring Transaction") },
            text = { Text("Are you sure you want to delete \"${recurring.category}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormDialog(
    title: String,
    existing: RecurringTransaction?,
    onDismiss: () -> Unit,
    onSave: (RecurringTransaction) -> Unit
) {
    var type by remember { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf(existing?.amount?.let { "%.2f".format(it) } ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var frequency by remember { mutableStateOf(existing?.frequency ?: Frequency.MONTHLY) }
    var dayOfMonth by remember { mutableStateOf(existing?.dayOfMonth?.toString() ?: "1") }
    var dayOfWeek by remember { mutableStateOf(existing?.dayOfWeek?.toString() ?: "0") }
    var amountError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }

    val dayOfWeekNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") }
                    )
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") }
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; amountError = false },
                    label = { Text("Amount (₪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) {{ Text("Enter a valid amount > 0") }} else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it; categoryError = false },
                    label = { Text("Category") },
                    isError = categoryError,
                    supportingText = if (categoryError) {{ Text("Category is required") }} else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = frequency == Frequency.MONTHLY,
                        onClick = { frequency = Frequency.MONTHLY },
                        label = { Text("Monthly") }
                    )
                    FilterChip(
                        selected = frequency == Frequency.WEEKLY,
                        onClick = { frequency = Frequency.WEEKLY },
                        label = { Text("Weekly") }
                    )
                }

                // Day selector
                if (frequency == Frequency.MONTHLY) {
                    OutlinedTextField(
                        value = dayOfMonth,
                        onValueChange = { dayOfMonth = it },
                        label = { Text("Day of month (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedDay = dayOfWeek.toIntOrNull() ?: 0
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = dayOfWeekNames.getOrElse(selectedDay) { "Monday" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Day of week") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            dayOfWeekNames.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = { dayOfWeek = index.toString(); expanded = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) { amountError = true; return@TextButton }
                if (category.isBlank()) { categoryError = true; return@TextButton }

                val recurring = RecurringTransaction(
                    id = existing?.id ?: 0,
                    type = type,
                    amount = amount,
                    category = category.trim(),
                    description = description.trim(),
                    frequency = frequency,
                    dayOfWeek = if (frequency == Frequency.WEEKLY) (dayOfWeek.toIntOrNull() ?: 0) else null,
                    dayOfMonth = if (frequency == Frequency.MONTHLY) (dayOfMonth.toIntOrNull() ?: 1) else null,
                    startDate = existing?.startDate ?: LocalDate.now().toString(),
                    isActive = existing?.isActive ?: true,
                    createdAt = existing?.createdAt ?: ""
                )
                onSave(recurring)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
