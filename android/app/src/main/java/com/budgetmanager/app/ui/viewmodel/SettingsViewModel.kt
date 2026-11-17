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
import com.budgetmanager.app.data.repository.BudgetRepository
import com.budgetmanager.app.data.sync.GoogleDriveSyncManager
import com.budgetmanager.app.data.sync.SyncAction
import com.budgetmanager.app.data.sync.SyncDecision
import com.budgetmanager.app.data.sync.SyncRecord
import com.budgetmanager.app.data.sync.SyncStatus
import com.budgetmanager.app.domain.model.BackupData
import com.budgetmanager.app.domain.manager.ActiveBudgetManager
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
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_prefs")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val googleSignInManager: GoogleSignInManager,
    private val budgetRepository: BudgetRepository,
    private val activeBudgetManager: ActiveBudgetManager,
    private val driveSyncManager: GoogleDriveSyncManager
) : ViewModel() {

    /** Google's Drive consent screen to show, when Google asks for (re)approval. */
    val driveConsentIntent = googleSignInManager.driveConsentIntent

    /** The Drive action to retry once the user approves access. */
    private var pendingDriveAction: (() -> Unit)? = null

    /** Called with the result of the consent screen launched from [driveConsentIntent]. */
    fun onDriveConsentResult(approved: Boolean) {
        googleSignInManager.onDriveConsentHandled()
        val retry = pendingDriveAction
        pendingDriveAction = null
        if (approved) {
            retry?.invoke()
        } else {
            viewModelScope.launch {
                _snackbarEvents.emit(SnackbarEvent.Error(
                    "Google Drive access was not approved, so sync can't run."
                ))
            }
        }
    }

    data class UiState(
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        val showImportConfirmation: Boolean = false,
        val pendingImportUri: Uri? = null,
        val lastBackupDate: String? = null,
        val authState: AuthState = AuthState.Loading,
        val navigateToSignIn: Boolean = false,
        val isSyncing: Boolean = false,
        val syncStatus: SyncStatus = SyncStatus.IDLE,
        val lastSyncDate: String? = null,
        /** Both phone and Drive changed since the last sync: ask which to keep. */
        val showSyncConflict: Boolean = false,
        /** A copy of the phone's data from before the last sync replaced it exists. */
        val hasSafetyCopy: Boolean = false
    )

    sealed class SnackbarEvent {
        data class Success(val message: String) : SnackbarEvent()
        data class Error(val message: String) : SnackbarEvent()
    }

    private object Keys {
        val LAST_BACKUP_DATE = stringPreferencesKey("last_backup_date")
        val LAST_SYNC_DATE = stringPreferencesKey("last_sync_date")
        val SYNC_LOCAL_FINGERPRINT = stringPreferencesKey("sync_local_fingerprint")
        val SYNC_DRIVE_MODIFIED = stringPreferencesKey("sync_drive_modified_time")
    }

    private companion object {
        const val SAFETY_COPY_NAME = "pre-sync-safety-copy.json"
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
            driveSyncManager.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        viewModelScope.launch {
            loadLastBackupDate()
        }
        viewModelScope.launch {
            loadLastSyncDate()
        }
        _uiState.update { it.copy(hasSafetyCopy = safetyCopyFile.exists()) }
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

    private suspend fun loadLastSyncDate() {
        try {
            val prefs = context.backupDataStore.data.first()
            val date = prefs[Keys.LAST_SYNC_DATE]
            _uiState.update { it.copy(lastSyncDate = date) }
        } catch (_: Exception) {
            // Ignore DataStore read errors
        }
    }

    private suspend fun saveLastSyncDate(date: String) {
        try {
            context.backupDataStore.edit { prefs ->
                prefs[Keys.LAST_SYNC_DATE] = date
            }
            _uiState.update { it.copy(lastSyncDate = date) }
        } catch (_: Exception) {
            // Ignore DataStore write errors
        }
    }

    /** Drive's copy and token held while the "both changed" dialog is open. */
    private var pendingConflict: Pair<String, GoogleDriveSyncManager.DriveSnapshot>? = null

    /** Copy of the phone's data saved just before the last sync replaced it. */
    private val safetyCopyFile: File get() = File(context.filesDir, SAFETY_COPY_NAME)

    /** Runs [block] with a Drive token, or reports why there isn't one. */
    private fun withDriveToken(retry: () -> Unit, block: suspend (String) -> Unit) {
        viewModelScope.launch {
            val token = googleSignInManager.getDriveAccessToken()
            if (token == null) {
                if (googleSignInManager.driveConsentIntent.value != null) {
                    // The screen shows Google's consent page; retry after approval.
                    pendingDriveAction = retry
                    return@launch
                }
                _snackbarEvents.emit(SnackbarEvent.Error(
                    googleSignInManager.lastDriveTokenError
                        ?: context.getString(R.string.sync_error_sign_in)
                ))
                return@launch
            }
            block(token)
        }
    }

    /**
     * Sync Now: works out which side changed since the last sync and only moves data in
     * that direction. Never replaces phone data without first saving a safety copy, and
     * asks when both sides changed.
     */
    fun syncWithDrive(): Unit = withDriveToken(retry = { syncWithDrive() }) { token ->
        _uiState.update { it.copy(isSyncing = true) }
        try {
            val snapshot = driveSyncManager.downloadFromDrive(token)
            val local = backupRepository.exportToJson()
            when (SyncDecision.decide(local, snapshot?.data, snapshot?.modifiedTime, loadSyncRecord())) {
                SyncAction.NOTHING -> {
                    saveSyncRecord(local, snapshot!!.modifiedTime)
                    finishSync(R.string.sync_up_to_date)
                }
                SyncAction.UPLOAD -> {
                    saveSyncRecord(local, driveSyncManager.uploadToDrive(token, local))
                    finishSync(R.string.sync_uploaded)
                }
                SyncAction.DOWNLOAD -> {
                    applyDriveCopy(snapshot!!)
                    finishSync(R.string.sync_downloaded)
                }
                SyncAction.ASK -> {
                    pendingConflict = token to snapshot!!
                    _uiState.update { it.copy(isSyncing = false, showSyncConflict = true) }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isSyncing = false) }
            _snackbarEvents.emit(SnackbarEvent.Error(
                context.getString(R.string.sync_error_generic, e.message ?: context.getString(R.string.error_unknown))
            ))
        }
    }

    /** Answer to the "both changed" dialog: keep the phone's data, or the web's. */
    fun resolveSyncConflict(keepPhone: Boolean) {
        val (token, snapshot) = pendingConflict ?: return
        pendingConflict = null
        _uiState.update { it.copy(showSyncConflict = false, isSyncing = true) }
        viewModelScope.launch {
            try {
                if (keepPhone) {
                    val local = backupRepository.exportToJson()
                    saveSyncRecord(local, driveSyncManager.uploadToDrive(token, local))
                    finishSync(R.string.sync_uploaded)
                } else {
                    applyDriveCopy(snapshot)
                    finishSync(R.string.sync_downloaded)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false) }
                _snackbarEvents.emit(SnackbarEvent.Error(
                    context.getString(R.string.sync_error_generic, e.message ?: context.getString(R.string.error_unknown))
                ))
            }
        }
    }

    fun dismissSyncConflict() {
        pendingConflict = null
        _uiState.update { it.copy(showSyncConflict = false) }
    }

    /** Saves a safety copy of the phone's data, then replaces it with Drive's. */
    private suspend fun applyDriveCopy(snapshot: GoogleDriveSyncManager.DriveSnapshot) {
        backupRepository.exportToUri(Uri.fromFile(safetyCopyFile))
        _uiState.update { it.copy(hasSafetyCopy = true) }
        backupRepository.importFromJson(snapshot.data)
        syncActiveBudget()
        saveSyncRecord(backupRepository.exportToJson(), snapshot.modifiedTime)
    }

    private suspend fun finishSync(messageRes: Int) {
        saveLastSyncDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        _uiState.update { it.copy(isSyncing = false) }
        _snackbarEvents.emit(SnackbarEvent.Success(context.getString(messageRes)))
    }

    /** Offers the safety copy through the normal "replace all data?" import confirmation. */
    fun requestRestoreSafetyCopy() {
        if (safetyCopyFile.exists()) requestImport(Uri.fromFile(safetyCopyFile))
    }

    private suspend fun loadSyncRecord(): SyncRecord? {
        val prefs = context.backupDataStore.data.first()
        val fp = prefs[Keys.SYNC_LOCAL_FINGERPRINT] ?: return null
        val mt = prefs[Keys.SYNC_DRIVE_MODIFIED] ?: return null
        return SyncRecord(fp, mt)
    }

    private suspend fun saveSyncRecord(local: BackupData, driveModifiedTime: String) {
        context.backupDataStore.edit { prefs ->
            prefs[Keys.SYNC_LOCAL_FINGERPRINT] = SyncDecision.fingerprint(local)
            prefs[Keys.SYNC_DRIVE_MODIFIED] = driveModifiedTime
        }
    }

    private suspend fun syncActiveBudget() {
        val activeBudget = budgetRepository.getActiveBudget()
        if (activeBudget != null) {
            activeBudgetManager.setActiveBudgetId(activeBudget.id)
        } else {
            val firstBudget = budgetRepository.getAll().firstOrNull()
            if (firstBudget != null) {
                budgetRepository.setActiveBudget(firstBudget.id)
                activeBudgetManager.setActiveBudgetId(firstBudget.id)
            } else {
                activeBudgetManager.setActiveBudgetId(0L)
            }
        }
    }

    /** Upload: the phone's data replaces Drive's, unconditionally. */
    fun uploadToDrive(): Unit = withDriveToken(retry = { uploadToDrive() }) { token ->
        _uiState.update { it.copy(isSyncing = true) }
        try {
            val data = backupRepository.exportToJson()
            saveSyncRecord(data, driveSyncManager.uploadToDrive(token, data))
            saveLastSyncDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            _uiState.update { it.copy(isSyncing = false) }
            _snackbarEvents.emit(SnackbarEvent.Success(context.getString(R.string.upload_success)))
        } catch (e: Exception) {
            _uiState.update { it.copy(isSyncing = false) }
            _snackbarEvents.emit(SnackbarEvent.Error(
                context.getString(R.string.upload_error_generic, e.message ?: context.getString(R.string.error_unknown))
            ))
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

                // After restore, sync ActiveBudgetManager with the restored data
                syncActiveBudget()

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
