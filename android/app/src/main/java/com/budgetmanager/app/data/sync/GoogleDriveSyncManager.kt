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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    /** Drive's current copy: its data and when Drive last saw it change. */
    data class DriveSnapshot(val data: BackupData, val modifiedTime: String)

    private data class DriveFile(val id: String, val modifiedTime: String)

    /**
     * Download budget data from Google Drive appDataFolder.
     * Returns null only when Drive has no file yet; any failure throws.
     */
    suspend fun downloadFromDrive(accessToken: String): DriveSnapshot? = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            _lastSyncError.value = null

            val file = findDriveFile(accessToken)
            if (file == null) {
                cachedFileId = null
                _syncStatus.value = SyncStatus.SYNCED
                return@withContext null
            }
            cachedFileId = file.id

            val conn = (URL("$DRIVE_FILES_URL/${file.id}?alt=media").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                conn.disconnect()
                throw Exception("Drive download failed: $error")
            }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val data = json.decodeFromString(BackupData.serializer(), body)
            _syncStatus.value = SyncStatus.SYNCED
            DriveSnapshot(data, file.modifiedTime)
        } catch (e: Exception) {
            Log.e(TAG, "Download from Drive failed", e)
            _syncStatus.value = SyncStatus.ERROR
            _lastSyncError.value = e.message
            throw e
        }
    }

    /**
     * Upload budget data to Google Drive appDataFolder, replacing Drive's copy.
     * Returns Drive's new `modifiedTime`; any failure throws.
     */
    suspend fun uploadToDrive(accessToken: String, data: BackupData): String = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            _lastSyncError.value = null

            val jsonContent = json.encodeToString(BackupData.serializer(), data)

            if (cachedFileId == null) {
                cachedFileId = findDriveFile(accessToken)?.id
            }
            val fileId = cachedFileId

            val boundary = "---budget_manager_boundary"
            val metadata = if (fileId != null) {
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

            val url = if (fileId != null) {
                URL("$DRIVE_UPLOAD_URL/$fileId?uploadType=multipart&fields=id,modifiedTime")
            } else {
                URL("$DRIVE_UPLOAD_URL?uploadType=multipart&fields=id,modifiedTime")
            }

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = if (fileId != null) "PATCH" else "POST"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                doOutput = true
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(multipartBody) }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                conn.disconnect()
                throw Exception("Drive upload failed: $error")
            }

            val responseBody = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            cachedFileId = jsonField(responseBody, "id") ?: fileId
            val modifiedTime = jsonField(responseBody, "modifiedTime")
                ?: throw Exception("Drive upload succeeded but returned no modifiedTime")

            _syncStatus.value = SyncStatus.SYNCED
            modifiedTime
        } catch (e: Exception) {
            Log.e(TAG, "Upload to Drive failed", e)
            _syncStatus.value = SyncStatus.ERROR
            _lastSyncError.value = e.message
            throw e
        }
    }

    /** Finds the data file; null only when Drive confirms there is none. Errors throw. */
    private fun findDriveFile(accessToken: String): DriveFile? {
        val searchUrl = URL(
            "$DRIVE_FILES_URL?spaces=appDataFolder&q=name%3D%27$DATA_FILENAME%27" +
                "&orderBy=modifiedTime%20desc&fields=files(id,name,modifiedTime)"
        )
        val conn = (searchUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            conn.disconnect()
            throw Exception("Drive lookup failed: $error")
        }

        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()

        val first = json.parseToJsonElement(body).jsonObject["files"]?.jsonArray?.firstOrNull()
            ?.jsonObject ?: return null
        val id = first["id"]?.jsonPrimitive?.content ?: return null
        val modifiedTime = first["modifiedTime"]?.jsonPrimitive?.content ?: ""
        return DriveFile(id, modifiedTime)
    }

    private fun jsonField(body: String, name: String): String? =
        json.parseToJsonElement(body).jsonObject[name]?.jsonPrimitive?.content

    fun clearCache() {
        cachedFileId = null
        _syncStatus.value = SyncStatus.IDLE
        _lastSyncError.value = null
    }
}
