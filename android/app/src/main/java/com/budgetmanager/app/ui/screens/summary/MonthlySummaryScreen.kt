package com.budgetmanager.app.ui.screens.summary

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.ui.components.AmountText
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.viewmodel.MonthlySummaryViewModel
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthlySummaryScreen(
    viewModel: MonthlySummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
            }
            Text(
                text = "${Month.of(uiState.month).getDisplayName(TextStyle.FULL, Locale.getDefault())} ${uiState.year}",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            uiState.summary?.let { summary ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Income", style = MaterialTheme.typography.labelMedium)
                            AmountText(amount = summary.totalIncome, type = TransactionType.INCOME)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Expenses", style = MaterialTheme.typography.labelMedium)
                            AmountText(amount = summary.totalExpenses, type = TransactionType.EXPENSE)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Balance", style = MaterialTheme.typography.labelMedium)
                            AmountText(
                                amount = summary.netBalance,
                                type = if (summary.netBalance >= 0) TransactionType.INCOME else TransactionType.EXPENSE
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(summary.categoryBreakdowns) { breakdown ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(breakdown.category)
                            AmountText(amount = breakdown.total, type = breakdown.type)
                        }
                    }
                }
            }
        }
    }
}
