package com.budgetmanager.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilterBar(
    selectedType: String?,
    selectedCategory: String?,
    categories: List<String>,
    onTypeSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // All filter
        CategoryChip(
            category = "All",
            selected = selectedType == null && selectedCategory == null,
            onClick = {
                onTypeSelected(null)
                onCategorySelected(null)
            }
        )

        // Type filters
        CategoryChip(
            category = "Income",
            selected = selectedType == "income",
            onClick = {
                onTypeSelected(if (selectedType == "income") null else "income")
            }
        )
        CategoryChip(
            category = "Expense",
            selected = selectedType == "expense",
            onClick = {
                onTypeSelected(if (selectedType == "expense") null else "expense")
            }
        )

        // Category filters
        categories.forEach { category ->
            CategoryChip(
                category = category,
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(
                        if (selectedCategory == category) null else category
                    )
                }
            )
        }
    }
}
