package com.budgetmanager.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.repository.RecurringRepository
import com.budgetmanager.app.domain.manager.ActiveBudgetManager
import com.budgetmanager.app.domain.model.RecurringTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repository: RecurringRepository,
    private val activeBudgetManager: ActiveBudgetManager
) : ViewModel() {

    data class UiState(
        val recurringTransactions: List<RecurringTransaction> = emptyList(),
        val isLoading: Boolean = true,
        val showAddDialog: Boolean = false,
        val editingRecurring: RecurringTransaction? = null,
        val generateResult: String? = null,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var recurringJob: Job? = null

    init {
        observeActiveBudget()
    }

    /**
     * Observes the active budget and reloads recurring transactions whenever it changes.
     */
    private fun observeActiveBudget() {
        viewModelScope.launch {
            activeBudgetManager.observeActiveBudgetId().collect { budgetId ->
                _uiState.update { it.copy(isLoading = true) }
                loadRecurring()
            }
        }
    }

    private fun loadRecurring() {
        recurringJob?.cancel()
        recurringJob = viewModelScope.launch {
            val budgetId = activeBudgetManager.getActiveBudgetId()
            if (budgetId > 0) {
                repository.observeAllByBudget(budgetId)
            } else {
                repository.observeAll()
            }
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { list ->
                    _uiState.update { it.copy(recurringTransactions = list, isLoading = false) }
                }
        }
    }

    fun create(recurring: RecurringTransaction) {
        viewModelScope.launch {
            try {
                val budgetId = activeBudgetManager.getActiveBudgetId()
                repository.create(recurring.copy(budgetId = budgetId))
                _uiState.update { it.copy(showAddDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun update(recurring: RecurringTransaction) {
        viewModelScope.launch {
            try {
                repository.update(recurring)
                _uiState.update { it.copy(editingRecurring = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repository.delete(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleActive(id: Long) {
        viewModelScope.launch {
            val recurring = repository.getById(id) ?: return@launch
            repository.update(recurring.copy(isActive = !recurring.isActive))
        }
    }

    fun generate(id: Long, startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                val generated = repository.generateTransactions(id, startDate, endDate)
                _uiState.update { it.copy(generateResult = "Generated ${generated.size} transactions") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true) }
    fun showEditDialog(recurring: RecurringTransaction) = _uiState.update { it.copy(editingRecurring = recurring) }
    fun dismissDialog() = _uiState.update { it.copy(showAddDialog = false, editingRecurring = null) }
}
