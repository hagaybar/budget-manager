package com.budgetmanager.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.auth.AuthState
import com.budgetmanager.app.auth.GoogleSignInManager
import com.budgetmanager.app.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val googleSignInManager: GoogleSignInManager
) : ViewModel() {

    data class UiState(
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        val message: String? = null,
        val authState: AuthState = AuthState.Loading
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            googleSignInManager.authState.collect { auth ->
                _uiState.update { it.copy(authState = auth) }
            }
        }
    }

    fun exportData(uri: Uri) {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                backupRepository.exportToUri(uri)
                _uiState.update { it.copy(isExporting = false, message = "Data exported successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, message = "Export failed: ${e.message}") }
            }
        }
    }

    fun importData(uri: Uri) {
        _uiState.update { it.copy(isImporting = true) }
        viewModelScope.launch {
            try {
                backupRepository.importFromUri(uri)
                _uiState.update { it.copy(isImporting = false, message = "Data imported successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, message = "Import failed: ${e.message}") }
            }
        }
    }

    fun signOut() {
        googleSignInManager.signOut()
    }
}
