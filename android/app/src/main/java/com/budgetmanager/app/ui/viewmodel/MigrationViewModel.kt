package com.budgetmanager.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.repository.BudgetRepository
import com.budgetmanager.app.data.repository.RecurringRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.domain.manager.ActiveBudgetManager
import com.budgetmanager.app.domain.model.Budget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MigrationViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val activeBudgetManager: ActiveBudgetManager
) : ViewModel() {

    data class UiState(
        val needsMigration: Boolean = false,
        val name: String = "My Budget",
        val description: String = "",
        val currency: String = "ILS",
        val monthlyTarget: String = "",
        val isMigrating: Boolean = false,
        val migrationComplete: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        checkMigrationNeeded()
    }

    private fun checkMigrationNeeded() {
        viewModelScope.launch {
            try {
                val budgets = budgetRepository.getAll()
                val transactions = transactionRepository.getAll()
                val recurring = recurringRepository.getAll()
                val hasData = transactions.isNotEmpty() || recurring.isNotEmpty()
                _uiState.update {
                    it.copy(needsMigration = budgets.isEmpty() && hasData)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun setDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun setCurrency(currency: String) {
        _uiState.update { it.copy(currency = currency) }
    }

    fun setTarget(target: String) {
        _uiState.update { it.copy(monthlyTarget = target) }
    }

    fun performMigration() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Budget name is required") }
            return
        }

        _uiState.update { it.copy(isMigrating = true, error = null) }
        viewModelScope.launch {
            try {
                // Create the budget
                val monthlyTarget = state.monthlyTarget.toDoubleOrNull()
                val budget = Budget(
                    name = state.name.trim(),
                    description = state.description.trim(),
                    currency = state.currency.trim().ifBlank { "ILS" },
                    monthlyTarget = monthlyTarget
                )
                val budgetId = budgetRepository.create(budget)

                // Batch-update all orphaned transactions to belong to this budget
                transactionRepository.assignOrphanedToBudget(budgetId)

                // Batch-update all orphaned recurring transactions to belong to this budget
                recurringRepository.assignOrphanedToBudget(budgetId)

                // Set this budget as active
                activeBudgetManager.setActiveBudgetId(budgetId)

                _uiState.update {
                    it.copy(
                        isMigrating = false,
                        migrationComplete = true,
                        needsMigration = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isMigrating = false, error = e.message)
                }
            }
        }
    }
}
