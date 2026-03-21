package com.budgetmanager.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.repository.BudgetRepository
import com.budgetmanager.app.domain.manager.ActiveBudgetManager
import com.budgetmanager.app.domain.model.Budget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val activeBudgetManager: ActiveBudgetManager
) : ViewModel() {

    data class UiState(
        val budgets: List<Budget> = emptyList(),
        val activeBudget: Budget? = null,
        val isLoading: Boolean = true,
        val showCreateDialog: Boolean = false,
        val showEditDialog: Boolean = false,
        val editingBudget: Budget? = null,
        val showDeleteConfirmation: Boolean = false,
        val deletingBudgetId: Long? = null,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                budgetRepository.observeAll(),
                activeBudgetManager.observeActiveBudgetId()
            ) { budgets, activeId ->
                val activeBudget = budgets.find { it.id == activeId }
                Pair(budgets, activeBudget)
            }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { (budgets, activeBudget) ->
                    _uiState.update {
                        it.copy(
                            budgets = budgets,
                            activeBudget = activeBudget,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun createBudget(
        name: String,
        description: String,
        currency: String,
        monthlyTarget: Double?
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Budget name is required") }
            return
        }
        viewModelScope.launch {
            try {
                val budget = Budget(
                    name = name.trim(),
                    description = description.trim(),
                    currency = currency.trim().ifBlank { "ILS" },
                    monthlyTarget = monthlyTarget
                )
                val id = budgetRepository.create(budget)
                // If this is the first budget, set it as active automatically
                if (_uiState.value.budgets.isEmpty()) {
                    activeBudgetManager.setActiveBudgetId(id)
                }
                _uiState.update { it.copy(showCreateDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateBudget(budget: Budget) {
        if (budget.name.isBlank()) {
            _uiState.update { it.copy(error = "Budget name is required") }
            return
        }
        viewModelScope.launch {
            try {
                budgetRepository.update(budget)
                _uiState.update { it.copy(showEditDialog = false, editingBudget = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            try {
                budgetRepository.delete(id)
                // If we deleted the active budget, switch to another one
                if (activeBudgetManager.getActiveBudgetId() == id) {
                    val remaining = _uiState.value.budgets.filter { it.id != id }
                    val newActiveId = remaining.firstOrNull()?.id ?: 0L
                    activeBudgetManager.setActiveBudgetId(newActiveId)
                }
                _uiState.update {
                    it.copy(showDeleteConfirmation = false, deletingBudgetId = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setActiveBudget(id: Long) {
        activeBudgetManager.setActiveBudgetId(id)
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun showEditDialog(budget: Budget) {
        _uiState.update { it.copy(showEditDialog = true, editingBudget = budget) }
    }

    fun showDeleteConfirmation(id: Long) {
        _uiState.update { it.copy(showDeleteConfirmation = true, deletingBudgetId = id) }
    }

    fun dismissDialogs() {
        _uiState.update {
            it.copy(
                showCreateDialog = false,
                showEditDialog = false,
                editingBudget = null,
                showDeleteConfirmation = false,
                deletingBudgetId = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
