package com.budgetmanager.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.domain.manager.ActiveBudgetManager
import com.budgetmanager.app.domain.model.MonthlySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MonthlySummaryViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val activeBudgetManager: ActiveBudgetManager
) : ViewModel() {

    data class UiState(
        val year: Int = LocalDate.now().year,
        val month: Int = LocalDate.now().monthValue,
        val summary: MonthlySummary? = null,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var summaryJob: Job? = null

    init {
        observeActiveBudget()
    }

    /**
     * Observes the active budget and reloads the summary whenever it changes.
     */
    private fun observeActiveBudget() {
        viewModelScope.launch {
            activeBudgetManager.observeActiveBudgetId().collect { budgetId ->
                _uiState.update { it.copy(isLoading = true) }
                loadSummary()
            }
        }
    }

    private fun loadSummary() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            val state = _uiState.value
            val budgetId = activeBudgetManager.getActiveBudgetId()
            if (budgetId > 0) {
                repository.getMonthlySummaryByBudget(budgetId, state.year, state.month)
            } else {
                repository.getMonthlySummary(state.year, state.month)
            }
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { summary ->
                    _uiState.update { it.copy(summary = summary, isLoading = false) }
                }
        }
    }

    fun setMonth(year: Int, month: Int) {
        _uiState.update { it.copy(year = year, month = month, isLoading = true) }
        loadSummary()
    }

    fun previousMonth() {
        val state = _uiState.value
        val newMonth = if (state.month == 1) 12 else state.month - 1
        val newYear = if (state.month == 1) state.year - 1 else state.year
        setMonth(newYear, newMonth)
    }

    fun nextMonth() {
        val state = _uiState.value
        val newMonth = if (state.month == 12) 1 else state.month + 1
        val newYear = if (state.month == 12) state.year + 1 else state.year
        setMonth(newYear, newMonth)
    }
}
