package com.budgetmanager.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.R
import com.budgetmanager.app.auth.AuthState
import com.budgetmanager.app.auth.GoogleSignInManager
import com.budgetmanager.app.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_prefs")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val googleSignInManager: GoogleSignInManager
) : ViewModel() {

    data class UiState(
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        val showImportConfirmation: Boolean = false,
        val pendingImportUri: Uri? = null,
        val lastBackupDate: String? = null,
        val authState: AuthState = AuthState.Loading,
        val navigateToSignIn: Boolean = false
    )

    sealed class SnackbarEvent {
        data class Success(val message: String) : SnackbarEvent()
        data class Error(val message: String) : SnackbarEvent()
    }

    private object Keys {
        val LAST_BACKUP_DATE = stringPreferencesKey("last_backup_date")
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>()
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            googleSignInManager.authState.collect { auth ->
                _uiState.update { it.copy(authState = auth) }
            }
        }
        viewModelScope.launch {
            loadLastBackupDate()
        }
    }

    private suspend fun loadLastBackupDate() {
        try {
            val prefs = context.backupDataStore.data.first()
            val date = prefs[Keys.LAST_BACKUP_DATE]
            _uiState.update { it.copy(lastBackupDate = date) }
        } catch (_: Exception) {
            // Ignore DataStore read errors
        }
    }

    private suspend fun saveLastBackupDate(date: String) {
        try {
            context.backupDataStore.edit { prefs ->
                prefs[Keys.LAST_BACKUP_DATE] = date
            }
            _uiState.update { it.copy(lastBackupDate = date) }
        } catch (_: Exception) {
            // Ignore DataStore write errors
        }
    }

    fun exportData(uri: Uri) {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                backupRepository.exportToUri(uri)
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                saveLastBackupDate(today)
                _uiState.update { it.copy(isExporting = false) }
                _snackbarEvents.emit(
                    SnackbarEvent.Success(context.getString(R.string.export_success))
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false) }
                val message = when {
                    e.message?.contains("permission", ignoreCase = true) == true ->
                        context.getString(R.string.error_export_permission)
                    e.message?.contains("space", ignoreCase = true) == true ->
                        context.getString(R.string.error_export_space)
                    else -> context.getString(
                        R.string.error_export_generic,
                        e.localizedMessage ?: context.getString(R.string.error_unknown)
                    )
                }
                _snackbarEvents.emit(SnackbarEvent.Error(message))
            }
        }
    }

    fun requestImport(uri: Uri) {
        _uiState.update {
            it.copy(
                showImportConfirmation = true,
                pendingImportUri = uri
            )
        }
    }

    fun confirmImport() {
        val uri = _uiState.value.pendingImportUri ?: return
        _uiState.update {
            it.copy(
                showImportConfirmation = false,
                pendingImportUri = null,
                isImporting = true
            )
        }
        viewModelScope.launch {
            try {
                backupRepository.importFromUri(uri)
                _uiState.update { it.copy(isImporting = false) }
                _snackbarEvents.emit(
                    SnackbarEvent.Success(context.getString(R.string.import_success))
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false) }
                val message = when {
                    e is kotlinx.serialization.SerializationException ||
                    e.message?.contains("JSON", ignoreCase = true) == true ||
                    e.message?.contains("parse", ignoreCase = true) == true ->
                        context.getString(R.string.error_import_invalid_file)
                    e.message?.contains("permission", ignoreCase = true) == true ->
                        context.getString(R.string.error_import_permission)
                    e is IllegalStateException && e.message?.contains("Could not read") == true ->
                        context.getString(R.string.error_import_read)
                    e is IllegalArgumentException ->
                        context.getString(R.string.error_import_validation)
                    else -> context.getString(
                        R.string.error_import_generic,
                        e.localizedMessage ?: context.getString(R.string.error_unknown)
                    )
                }
                _snackbarEvents.emit(SnackbarEvent.Error(message))
            }
        }
    }

    fun dismissImportConfirmation() {
        _uiState.update {
            it.copy(
                showImportConfirmation = false,
                pendingImportUri = null
            )
        }
    }

    fun signOut() {
        googleSignInManager.signOut()
        _uiState.update { it.copy(navigateToSignIn = true) }
    }

    fun onSignInNavigated() {
        _uiState.update { it.copy(navigateToSignIn = false) }
    }

    fun getSuggestedBackupFilename(): String {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "budget_backup_$today.json"
    }
}
