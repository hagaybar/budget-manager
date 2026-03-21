package com.budgetmanager.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.domain.model.Budget
import com.budgetmanager.app.domain.util.currencySymbol
import com.budgetmanager.app.ui.theme.Spacing

/**
 * Refined budget selector dropdown with design system typography and spacing.
 *
 * Displays the active budget name as a tappable button. When expanded, shows
 * all budgets with their currency badges and a "Manage Budgets" action item.
 *
 * @param budgets              available budgets to choose from
 * @param activeBudget         currently selected budget (highlighted in bold)
 * @param onBudgetSelected     callback with the selected budget ID
 * @param onManageBudgetsClick callback to navigate to budget management
 * @param modifier             outer modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSelectorDropdown(
    budgets: List<Budget>,
    activeBudget: Budget?,
    onBudgetSelected: (Long) -> Unit,
    onManageBudgetsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        TextButton(
            onClick = { },
            modifier = Modifier.menuAnchor(),
        ) {
            Text(
                text = activeBudget?.name ?: "Select Budget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select budget",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            budgets.forEach { budget ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = budget.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (budget.id == activeBudget?.id)
                                    FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraSmall,
                            ) {
                                Text(
                                    text = currencySymbol(budget.currency),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.sm,
                                        vertical = 2.dp,
                                    ),
                                )
                            }
                        }
                    },
                    onClick = {
                        onBudgetSelected(budget.id)
                        expanded = false
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = Spacing.lg,
                        vertical = Spacing.md,
                    ),
                )
            }

            if (budgets.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.xs),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }

            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Manage Budgets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onManageBudgetsClick()
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Spacing.lg,
                    vertical = Spacing.md,
                ),
            )
        }
    }
}
