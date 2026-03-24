package com.budgetmanager.app.data.sync

import android.content.Context
import android.util.Log
import com.budgetmanager.app.domain.model.BackupData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus {
    IDLE, SYNCING, SYNCED, ERROR
}

@Singleton
class GoogleDriveSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GoogleDriveSyncManager"
        private const val DATA_FILENAME = "budget_data.json"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private var cachedFileId: String? = null

    /**
     * Download budget data from Google Drive appDataFolder.
     * Returns null if no file exists yet.
     */
    suspend fun downloadFromDrive(accessToken: String): BackupData? = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            _lastSyncError.value = null

            // Find the file
            val fileId = findDriveFile(accessToken)
            if (fileId == null) {
                _syncStatus.value = SyncStatus.SYNCED
                return@withContext null
            }
            cachedFileId = fileId

            // Download content
            val url = URL("$DRIVE_FILES_URL/$fileId?alt=media")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                throw Exception("Drive download failed: $error")
            }

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val data = json.decodeFromString(BackupData.serializer(), body)
            _syncStatus.value = SyncStatus.SYNCED
            data
        } catch (e: Exception) {
            Log.e(TAG, "Download from Drive failed", e)
            _syncStatus.value = SyncStatus.ERROR
            _lastSyncError.value = e.message
            null
        }
    }

    /**
     * Upload budget data to Google Drive appDataFolder.
     */
    suspend fun uploadToDrive(accessToken: String, data: BackupData): Boolean = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            _lastSyncError.value = null

            val jsonContent = json.encodeToString(BackupData.serializer(), data)

            // Check if file exists
            if (cachedFileId == null) {
                cachedFileId = findDriveFile(accessToken)
            }

            val boundary = "---budget_manager_boundary"
            val metadata = if (cachedFileId != null) {
                """{"name":"$DATA_FILENAME","mimeType":"application/json"}"""
            } else {
                """{"name":"$DATA_FILENAME","mimeType":"application/json","parents":["appDataFolder"]}"""
            }

            val multipartBody = "--$boundary\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                metadata + "\r\n" +
                "--$boundary\r\n" +
                "Content-Type: application/json\r\n\r\n" +
                jsonContent + "\r\n" +
                "--$boundary--"

            val url = if (cachedFileId != null) {
                URL("$DRIVE_UPLOAD_URL/${cachedFileId}?uploadType=multipart")
            } else {
                URL("$DRIVE_UPLOAD_URL?uploadType=multipart")
            }

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = if (cachedFileId != null) "PATCH" else "POST"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                doOutput = true
            }

            OutputStreamWriter(conn.outputStream).use { it.write(multipartBody) }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                throw Exception("Drive upload failed: $error")
            }

            // Parse response to get file ID if new file
            if (cachedFileId == null) {
                val responseBody = conn.inputStream.bufferedReader().readText()
                val idMatch = Regex(""""id"\s*:\s*"([^"]+)"""").find(responseBody)
                cachedFileId = idMatch?.groupValues?.get(1)
            }

            conn.disconnect()
            _syncStatus.value = SyncStatus.SYNCED
            true
        } catch (e: Exception) {
            Log.e(TAG, "Upload to Drive failed", e)
            _syncStatus.value = SyncStatus.ERROR
            _lastSyncError.value = e.message
            false
        }
    }

    private fun findDriveFile(accessToken: String): String? {
        val searchUrl = URL(
            "$DRIVE_FILES_URL?spaces=appDataFolder&q=name%3D%27$DATA_FILENAME%27&fields=files(id,name,modifiedTime)"
        )
        val conn = (searchUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            conn.disconnect()
            return null
        }

        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        // Simple JSON parsing for file ID
        val idMatch = Regex(""""id"\s*:\s*"([^"]+)"""").find(body)
        return idMatch?.groupValues?.get(1)
    }

    fun clearCache() {
        cachedFileId = null
        _syncStatus.value = SyncStatus.IDLE
        _lastSyncError.value = null
    }
}
