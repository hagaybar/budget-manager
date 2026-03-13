package com.budgetmanager.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budgetmanager.app.ui.screens.transactions.TransactionListScreen
import com.budgetmanager.app.ui.screens.transactions.AddEditTransactionScreen
import com.budgetmanager.app.ui.screens.summary.MonthlySummaryScreen
import com.budgetmanager.app.ui.screens.recurring.RecurringScreen
import com.budgetmanager.app.ui.screens.settings.SettingsScreen
import com.budgetmanager.app.ui.screens.signin.SignInScreen

@Composable
fun BudgetNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Transactions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    onAddTransaction = {
                        navController.navigate("transactions/add_edit?id=-1")
                    },
                    onEditTransaction = { id ->
                        navController.navigate("transactions/add_edit?id=$id")
                    }
                )
            }

            composable(
                route = "transactions/add_edit?id={id}",
                arguments = listOf(navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) {
                AddEditTransactionScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Summary.route) {
                MonthlySummaryScreen()
            }

            composable(Screen.Recurring.route) {
                RecurringScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(Screen.SignIn.route) {
                SignInScreen(
                    onSignInComplete = {
                        navController.navigate(Screen.Transactions.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
