package com.budgetmanager.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.budgetmanager.app.data.repository.RecycleBinRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.domain.manager.ActiveBudgetManager
import com.budgetmanager.app.domain.model.Transaction
import com.budgetmanager.app.domain.usecase.GenerateRecurringTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val recycleBin: RecycleBinRepository,
    private val generateRecurringTransactions: GenerateRecurringTransactionsUseCase,
    private val activeBudgetManager: ActiveBudgetManager
) : ViewModel() {

    data class UiState(
        val transactions: List<Transaction> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val filterType: String? = null,
        val filterCategory: String? = null,
        val filterDateFrom: String? = null,
        val filterDateTo: String? = null,
        val availableCategories: List<String> = emptyList(),
        /** Transaction awaiting "are you sure?" confirmation, if any. */
        val pendingDelete: Transaction? = null,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Emits the id of each transaction moved to the recycle bin (drives the Undo snackbar). */
    private val _movedToBin = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val movedToBin: SharedFlow<Long> = _movedToBin.asSharedFlow()

    private var transactionJob: Job? = null
    private var categoriesJob: Job? = null

    init {
        generateRecurringTransactionsEagerly()
        observeActiveBudget()
    }

    /**
     * Observes the active budget and reloads transactions whenever it changes.
     */
    private fun observeActiveBudget() {
        viewModelScope.launch {
            activeBudgetManager.observeActiveBudgetId().collect { budgetId ->
                _uiState.update { it.copy(isLoading = true) }
                loadTransactions()
                loadCategories()
            }
        }
    }

    /**
     * Eagerly generates any pending recurring transactions for today.
     * This ensures transactions are up-to-date when the user opens the app,
     * without relying solely on WorkManager which may be delayed by Doze mode
     * or battery optimization. The operation is idempotent — duplicate checks
     * prevent creating the same transaction twice.
     */
    private fun generateRecurringTransactionsEagerly() {
        viewModelScope.launch {
            try {
                val created = generateRecurringTransactions()
                if (created > 0) {
                    Log.d("TransactionListVM", "Eagerly generated $created recurring transaction(s)")
                }
            } catch (e: Exception) {
                Log.w("TransactionListVM", "Failed to generate recurring transactions", e)
                // Non-fatal: WorkManager will handle it as a fallback
            }
        }
    }

    private fun loadTransactions() {
        transactionJob?.cancel()
        transactionJob = viewModelScope.launch {
            val state = _uiState.value
            val budgetId = activeBudgetManager.getActiveBudgetId()
            if (budgetId > 0) {
                repository.observeFilteredByBudget(
                    budgetId,
                    state.filterType, state.filterCategory,
                    state.filterDateFrom, state.filterDateTo
                )
            } else {
                repository.observeFiltered(
                    state.filterType, state.filterCategory,
                    state.filterDateFrom, state.filterDateTo
                )
            }.catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false, isRefreshing = false) }
            }.collect { transactions ->
                _uiState.update { it.copy(transactions = transactions, isLoading = false, isRefreshing = false) }
            }
        }
    }

    private fun loadCategories() {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            val budgetId = activeBudgetManager.getActiveBudgetId()
            val categoriesFlow = if (budgetId > 0) {
                repository.observeCategoriesByBudget(budgetId)
            } else {
                repository.observeCategories()
            }
            categoriesFlow.collect { categories ->
                _uiState.update { it.copy(availableCategories = categories) }
            }
        }
    }

    fun setFilter(type: String?, category: String?, dateFrom: String?, dateTo: String?) {
        _uiState.update {
            it.copy(
                filterType = type, filterCategory = category,
                filterDateFrom = dateFrom, filterDateTo = dateTo, isLoading = true
            )
        }
        loadTransactions()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadTransactions()
    }

    /** Asks the user to confirm before deleting [transaction]. */
    fun requestDelete(transaction: Transaction) {
        _uiState.update { it.copy(pendingDelete = transaction) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    /** Confirmed: moves the pending transaction to the recycle bin. */
    fun confirmDelete() {
        val transaction = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            try {
                if (recycleBin.moveToBin(transaction.id)) {
                    _movedToBin.emit(transaction.id)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /** Undo from the snackbar: restores the transaction from the recycle bin. */
    fun undoDelete(id: Long) {
        viewModelScope.launch {
            try {
                recycleBin.restore(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
