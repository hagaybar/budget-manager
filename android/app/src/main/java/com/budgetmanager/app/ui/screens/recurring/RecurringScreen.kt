package com.budgetmanager.app.ui.screens.recurring

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.domain.model.Frequency
import com.budgetmanager.app.domain.model.RecurringTransaction
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.domain.util.formatNextOccurrence
import com.budgetmanager.app.domain.util.getNextOccurrence
import com.budgetmanager.app.ui.components.AmountText
import com.budgetmanager.app.ui.components.AmountSize
import com.budgetmanager.app.ui.components.EmptyStateView
import com.budgetmanager.app.ui.components.LoadingState
import com.budgetmanager.app.ui.theme.CornerRadius
import com.budgetmanager.app.ui.theme.LocalFinanceColors
import com.budgetmanager.app.ui.theme.Spacing
import com.budgetmanager.app.ui.viewmodel.RecurringViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var viewingRecurring by remember { mutableStateOf<RecurringTransaction?>(null) }
    val financeColors = LocalFinanceColors.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(CornerRadius.extraLarge)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    modifier = Modifier.padding(padding),
                    label = "Loading recurring transactions..."
                )
            }
            uiState.recurringTransactions.isEmpty() && !uiState.showAddDialog -> {
                EmptyStateView(
                    icon = Icons.Outlined.Repeat,
                    title = "No recurring transactions",
                    description = "Tap + to add your first recurring transaction."
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    item { Spacer(modifier = Modifier.height(Spacing.xs)) }
                    items(uiState.recurringTransactions, key = { it.id }) { recurring ->
                        val typeColor = if (recurring.type == TransactionType.INCOME)
                            financeColors.income else financeColors.expense
                        val typeIcon = if (recurring.type == TransactionType.INCOME)
                            Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewingRecurring = recurring },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(CornerRadius.medium)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                // Top row: icon + category + badges | amount + switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Type icon badge (matching TransactionRow dot style)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(typeColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = typeIcon,
                                            contentDescription = null,
                                            tint = typeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(Spacing.md))

                                    // Category + description
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recurring.category,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (recurring.description.isNotBlank()) {
                                            Text(
                                                text = recurring.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(Spacing.sm))

                                    // Amount on the right
                                    Column(horizontalAlignment = Alignment.End) {
                                        AmountText(
                                            amount = recurring.amount,
                                            type = recurring.type,
                                            size = AmountSize.Small
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(Spacing.sm))

                                    // Active/inactive toggle
                                    Switch(
                                        checked = recurring.isActive,
                                        onCheckedChange = { viewModel.toggleActive(recurring.id) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(Spacing.md))

                                // Bottom row: frequency badge + next occurrence
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Frequency badge
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(CornerRadius.extraSmall)
                                    ) {
                                        Text(
                                            text = recurring.frequency.value.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(
                                                horizontal = Spacing.sm,
                                                vertical = Spacing.xs
                                            )
                                        )
                                    }

                                    // Next occurrence — prominent display
                                    val nextDate = getNextOccurrence(recurring)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (nextDate != null)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text(
                                            text = if (nextDate != null)
                                                formatNextOccurrence(nextDate)
                                            else "Paused",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (nextDate != null)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(Spacing.sm)) }
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
        shape = RoundedCornerShape(CornerRadius.large),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recurring.category,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DetailRow("Type", recurring.type.value.replaceFirstChar { it.uppercase() })
                DetailRow("Amount", "\u20AA ${"%.2f".format(recurring.amount)}")
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
            shape = RoundedCornerShape(CornerRadius.large),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
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

    val financeColors = LocalFinanceColors.current
    val dayOfWeekNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(CornerRadius.large),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                // Type toggle with FinanceColors
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") },
                        leadingIcon = if (type == TransactionType.INCOME) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = financeColors.income.copy(alpha = 0.12f),
                            selectedLabelColor = financeColors.income,
                            selectedLeadingIconColor = financeColors.income
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = financeColors.income.copy(alpha = 0.5f),
                            enabled = true,
                            selected = type == TransactionType.INCOME
                        )
                    )
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") },
                        leadingIcon = if (type == TransactionType.EXPENSE) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = financeColors.expense.copy(alpha = 0.12f),
                            selectedLabelColor = financeColors.expense,
                            selectedLeadingIconColor = financeColors.expense
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = financeColors.expense.copy(alpha = 0.5f),
                            enabled = true,
                            selected = type == TransactionType.EXPENSE
                        )
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; amountError = false },
                    label = { Text("Amount (\u20AA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) {{ Text("Enter a valid amount > 0") }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(CornerRadius.small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it; categoryError = false },
                    label = { Text("Category") },
                    isError = categoryError,
                    supportingText = if (categoryError) {{ Text("Category is required") }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(CornerRadius.small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(CornerRadius.small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Frequency toggle
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = frequency == Frequency.MONTHLY,
                        onClick = { frequency = Frequency.MONTHLY },
                        label = { Text("Monthly") },
                        leadingIcon = if (frequency == Frequency.MONTHLY) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
                            enabled = true,
                            selected = frequency == Frequency.MONTHLY
                        )
                    )
                    FilterChip(
                        selected = frequency == Frequency.WEEKLY,
                        onClick = { frequency = Frequency.WEEKLY },
                        label = { Text("Weekly") },
                        leadingIcon = if (frequency == Frequency.WEEKLY) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
                            enabled = true,
                            selected = frequency == Frequency.WEEKLY
                        )
                    )
                }

                // Day selector
                if (frequency == Frequency.MONTHLY) {
                    OutlinedTextField(
                        value = dayOfMonth,
                        onValueChange = { dayOfMonth = it },
                        label = { Text("Day of month (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(CornerRadius.small),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(CornerRadius.small),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
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
