package com.budgetmanager.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Transactions : Screen("transactions", "Transactions", Icons.Default.Receipt)
    data object Summary : Screen("summary", "Summary", Icons.Default.Receipt)
    data object Recurring : Screen("recurring", "Recurring", Icons.Default.Repeat)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object AddEditTransaction : Screen("transactions/add_edit?id={id}", "Add/Edit", Icons.Default.Add)
    data object SignIn : Screen("sign_in", "Sign In", Icons.Default.Settings)
    data object BudgetManagement : Screen("budget_management", "Manage Budgets", Icons.Default.AccountBalance)

    companion object {
        val bottomNavItems = listOf(Transactions, Summary, Recurring, Settings)
    }
}
