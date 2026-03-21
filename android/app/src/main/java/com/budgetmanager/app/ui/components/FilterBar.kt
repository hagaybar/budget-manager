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
import com.budgetmanager.app.ui.theme.Spacing

/**
 * Horizontally scrollable filter bar with type and category chips.
 *
 * Uses design system [Spacing] tokens for consistent padding and gaps.
 *
 * @param selectedType      currently selected type filter ("income", "expense", or null)
 * @param selectedCategory  currently selected category name, or null
 * @param categories        list of available category names
 * @param onTypeSelected    callback when a type chip is toggled
 * @param onCategorySelected callback when a category chip is toggled
 * @param modifier          outer modifier
 */
@Composable
fun FilterBar(
    selectedType: String?,
    selectedCategory: String?,
    categories: List<String>,
    onTypeSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // All filter
        CategoryChip(
            category = "All",
            selected = selectedType == null && selectedCategory == null,
            onClick = {
                onTypeSelected(null)
                onCategorySelected(null)
            },
        )

        // Type filters
        CategoryChip(
            category = "Income",
            selected = selectedType == "income",
            onClick = {
                onTypeSelected(if (selectedType == "income") null else "income")
            },
        )
        CategoryChip(
            category = "Expense",
            selected = selectedType == "expense",
            onClick = {
                onTypeSelected(if (selectedType == "expense") null else "expense")
            },
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
                },
            )
        }
    }
}
