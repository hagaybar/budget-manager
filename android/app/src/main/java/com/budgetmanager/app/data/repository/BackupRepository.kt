package com.budgetmanager.app.data.repository

import android.net.Uri
import com.budgetmanager.app.domain.model.BackupData

interface BackupRepository {
    suspend fun exportToJson(): BackupData
    suspend fun importFromJson(data: BackupData)
    suspend fun exportToUri(uri: Uri)
    suspend fun importFromUri(uri: Uri)
}
