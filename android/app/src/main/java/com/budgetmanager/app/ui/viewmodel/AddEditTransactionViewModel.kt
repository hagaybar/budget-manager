package com.budgetmanager.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.domain.manager.ActiveBudgetManager
import com.budgetmanager.app.domain.model.Transaction
import com.budgetmanager.app.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle,
    private val activeBudgetManager: ActiveBudgetManager
) : ViewModel() {

    data class UiState(
        val isEditMode: Boolean = false,
        val transactionId: Long = 0,
        val type: TransactionType = TransactionType.EXPENSE,
        val amount: String = "",
        val category: String = "",
        val description: String = "",
        val date: LocalDate = LocalDate.now(),
        val budgetId: Long = 0,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val id = savedStateHandle.get<Long>("id") ?: -1L
        if (id > 0) {
            _uiState.update { it.copy(isEditMode = true, transactionId = id) }
            loadTransaction(id)
        }
    }

    private fun loadTransaction(id: Long) {
        viewModelScope.launch {
            val transaction = repository.getById(id) ?: return@launch
            _uiState.update {
                it.copy(
                    type = transaction.type,
                    amount = transaction.amount.toString(),
                    category = transaction.category,
                    description = transaction.description,
                    date = LocalDate.parse(transaction.date),
                    budgetId = transaction.budgetId
                )
            }
        }
    }

    fun setType(type: TransactionType) = _uiState.update { it.copy(type = type) }
    fun setAmount(amount: String) = _uiState.update { it.copy(amount = amount) }
    fun setCategory(category: String) = _uiState.update { it.copy(category = category) }
    fun setDescription(description: String) = _uiState.update { it.copy(description = description) }
    fun setDate(date: LocalDate) = _uiState.update { it.copy(date = date) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Invalid amount") }
            return
        }
        if (state.category.isBlank()) {
            _uiState.update { it.copy(error = "Category is required") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val budgetId = if (state.isEditMode) {
                    // Preserve existing budgetId when editing
                    state.budgetId
                } else {
                    // Use current active budget for new transactions
                    activeBudgetManager.getActiveBudgetId()
                }
                val transaction = Transaction(
                    id = if (state.isEditMode) state.transactionId else 0,
                    type = state.type,
                    amount = amount,
                    category = state.category.trim(),
                    description = state.description.trim(),
                    date = state.date.toString(),
                    budgetId = budgetId
                )
                if (state.isEditMode) {
                    repository.update(transaction)
                } else {
                    repository.create(transaction)
                }
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
