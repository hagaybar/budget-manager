package com.budgetmanager.app.ui.screens.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.components.DatePickerField
import com.budgetmanager.app.ui.viewmodel.AddEditTransactionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.setDate(selectedDate)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Transaction" else "Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Type toggle
            Row {
                FilterChip(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.setType(TransactionType.EXPENSE) },
                    label = { Text("Expense") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.setType(TransactionType.INCOME) },
                    label = { Text("Income") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount input with shekel prefix
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.setAmount(it) },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("\u20AA") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category selection
            OutlinedTextField(
                value = uiState.category,
                onValueChange = { viewModel.setCategory(it) },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.setDescription(it) },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date picker field
            DatePickerField(
                value = uiState.date.toString(),
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                label = "Date",
                onClick = { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save / Cancel buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                ) {
                    Text("Save")
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, color = androidx.compose.ui.graphics.Color.Red)
            }
        }
    }
}
