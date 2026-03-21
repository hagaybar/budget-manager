package com.budgetmanager.app.ui.screens.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.domain.model.Budget

data class CurrencyOption(val code: String, val symbol: String, val label: String)

val commonCurrencies = listOf(
    CurrencyOption("ILS", "\u20AA", "ILS \u20AA - Israeli Shekel"),
    CurrencyOption("USD", "$", "USD $ - US Dollar"),
    CurrencyOption("EUR", "\u20AC", "EUR \u20AC - Euro"),
    CurrencyOption("GBP", "\u00A3", "GBP \u00A3 - British Pound"),
    CurrencyOption("JPY", "\u00A5", "JPY \u00A5 - Japanese Yen"),
    CurrencyOption("CHF", "CHF", "CHF - Swiss Franc"),
    CurrencyOption("CAD", "CA$", "CAD - Canadian Dollar"),
    CurrencyOption("AUD", "A$", "AUD - Australian Dollar")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormDialog(
    budget: Budget? = null,
    onConfirm: (name: String, description: String, currency: String, monthlyTarget: Double?) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = budget != null

    var name by remember { mutableStateOf(budget?.name ?: "") }
    var description by remember { mutableStateOf(budget?.description ?: "") }
    var currency by remember { mutableStateOf(budget?.currency ?: "ILS") }
    var monthlyTarget by remember {
        mutableStateOf(budget?.monthlyTarget?.let { "%.2f".format(it) } ?: "")
    }
    var nameError by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    val selectedCurrencyLabel = commonCurrencies.find { it.code == currency }?.label ?: currency

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditMode) "Edit Budget" else "Create Budget")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Name") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Budget name is required") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Currency
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCurrencyLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        commonCurrencies.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    currency = option.code
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                // Monthly Target
                OutlinedTextField(
                    value = monthlyTarget,
                    onValueChange = { monthlyTarget = it },
                    label = { Text("Monthly Target (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@TextButton
                    }
                    onConfirm(
                        name.trim(),
                        description.trim(),
                        currency,
                        monthlyTarget.toDoubleOrNull()
                    )
                }
            ) {
                Text(if (isEditMode) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
