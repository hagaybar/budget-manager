package com.budgetmanager.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budgetmanager.app.auth.AuthState
import com.budgetmanager.app.ui.components.BudgetSelectorDropdown
import com.budgetmanager.app.ui.screens.budget.BudgetManagementScreen
import com.budgetmanager.app.ui.screens.migration.MigrationScreen
import com.budgetmanager.app.ui.screens.transactions.TransactionListScreen
import com.budgetmanager.app.ui.screens.transactions.AddEditTransactionScreen
import com.budgetmanager.app.ui.screens.summary.MonthlySummaryScreen
import com.budgetmanager.app.ui.screens.recurring.RecurringScreen
import com.budgetmanager.app.ui.screens.settings.SettingsScreen
import com.budgetmanager.app.ui.screens.signin.SignInScreen
import com.budgetmanager.app.ui.viewmodel.AuthViewModel
import com.budgetmanager.app.ui.viewmodel.BudgetViewModel
import com.budgetmanager.app.ui.viewmodel.MigrationViewModel

private const val NAV_ANIM_DURATION = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val budgetViewModel: BudgetViewModel = hiltViewModel()
    val budgetUiState by budgetViewModel.uiState.collectAsState()
    val migrationViewModel: MigrationViewModel = hiltViewModel()
    val migrationUiState by migrationViewModel.uiState.collectAsState()

    // Track whether migration overlay has been dismissed
    var migrationDismissed by rememberSaveable { mutableStateOf(false) }

    // On first launch (no persisted auth), navigate to sign-in
    LaunchedEffect(authState) {
        if (authState is AuthState.SignedOut && currentRoute == Screen.Transactions.route) {
            navController.navigate(Screen.SignIn.route) {
                popUpTo(Screen.Transactions.route) { inclusive = true }
            }
        }
    }

    // Determine which screens should show the budget selector in the top bar
    val mainScreenRoutes = setOf(
        Screen.Transactions.route,
        Screen.Summary.route,
        Screen.Recurring.route
    )
    val showBudgetSelector = currentRoute in mainScreenRoutes

    // Hide bottom nav bar on sign-in, add/edit, and budget management screens
    val showBottomBar = currentRoute != Screen.SignIn.route &&
        currentRoute != "transactions/add_edit?id={id}" &&
        currentRoute != Screen.BudgetManagement.route

    // Show migration screen as an overlay if needed
    val showMigration = migrationUiState.needsMigration && !migrationDismissed

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (showBudgetSelector) {
                    TopAppBar(
                        title = {
                            BudgetSelectorDropdown(
                                budgets = budgetUiState.budgets,
                                activeBudget = budgetUiState.activeBudget,
                                onBudgetSelected = { id ->
                                    budgetViewModel.setActiveBudget(id)
                                },
                                onManageBudgetsClick = {
                                    navController.navigate(Screen.BudgetManagement.route)
                                }
                            )
                        }
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Transactions.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) },
                popEnterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
                popExitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) },
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
                    SettingsScreen(
                        onNavigateToSignIn = {
                            navController.navigate(Screen.SignIn.route)
                        }
                    )
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

                composable(Screen.BudgetManagement.route) {
                    BudgetManagementScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // Migration overlay
        if (showMigration) {
            MigrationScreen(
                viewModel = migrationViewModel,
                onMigrationComplete = {
                    migrationDismissed = true
                }
            )
        }
    }
}
