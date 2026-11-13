package com.budgetmanager.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.repository.RecycleBinRepository
import com.budgetmanager.app.domain.manager.ActiveBudgetManager
import com.budgetmanager.app.domain.model.DeletedTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val recycleBin: RecycleBinRepository,
    private val activeBudgetManager: ActiveBudgetManager
) : ViewModel() {

    data class UiState(
        val items: List<DeletedTransaction> = emptyList(),
        val isLoading: Boolean = true,
        /** Entry awaiting "delete forever" confirmation. */
        val pendingDeleteForever: DeletedTransaction? = null,
        val showEmptyBinConfirmation: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        purgeExpired()
        observeItems()
    }

    private fun purgeExpired() {
        viewModelScope.launch {
            try {
                recycleBin.purgeExpired()
            } catch (e: Exception) {
                Log.w("RecycleBinVM", "Failed to purge expired bin entries", e)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeItems() {
        viewModelScope.launch {
            activeBudgetManager.observeActiveBudgetId()
                .flatMapLatest { budgetId ->
                    if (budgetId > 0) recycleBin.observeByBudget(budgetId) else recycleBin.observeAll()
                }
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { items -> _uiState.update { it.copy(items = items, isLoading = false) } }
        }
    }

    fun restore(item: DeletedTransaction) {
        viewModelScope.launch {
            try {
                recycleBin.restore(item.transaction.id)
                _messages.emit("Transaction restored")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun requestDeleteForever(item: DeletedTransaction) {
        _uiState.update { it.copy(pendingDeleteForever = item) }
    }

    fun confirmDeleteForever() {
        val item = _uiState.value.pendingDeleteForever ?: return
        _uiState.update { it.copy(pendingDeleteForever = null) }
        viewModelScope.launch {
            try {
                recycleBin.deleteForever(item.transaction.id)
                _messages.emit("Transaction permanently deleted")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun requestEmptyBin() {
        if (_uiState.value.items.isNotEmpty()) {
            _uiState.update { it.copy(showEmptyBinConfirmation = true) }
        }
    }

    fun confirmEmptyBin() {
        _uiState.update { it.copy(showEmptyBinConfirmation = false) }
        viewModelScope.launch {
            try {
                val budgetId = activeBudgetManager.getActiveBudgetId()
                if (budgetId > 0) {
                    recycleBin.emptyBin(budgetId)
                } else {
                    _uiState.value.items.forEach { recycleBin.deleteForever(it.transaction.id) }
                }
                _messages.emit("Recycle Bin emptied")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun dismissDialogs() {
        _uiState.update { it.copy(pendingDeleteForever = null, showEmptyBinConfirmation = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
