package com.budgetmanager.app.data.sync

import com.budgetmanager.app.domain.model.BackupData
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/** What "Sync Now" should do, given what changed on each side since the last sync. */
enum class SyncAction {
    /** Both sides already match. */
    NOTHING,

    /** Only the phone changed (or Drive has no file yet): send the phone's data to Drive. */
    UPLOAD,

    /** Only Drive changed: replace the phone's data with Drive's. */
    DOWNLOAD,

    /** Both changed (or we can't tell): ask the user which side to keep. */
    ASK
}

/**
 * What the phone recorded at the end of the last successful sync.
 *
 * @param localFingerprint fingerprint of the phone's data right after that sync
 * @param driveModifiedTime Drive's `modifiedTime` for the file right after that sync
 */
data class SyncRecord(val localFingerprint: String, val driveModifiedTime: String)

object SyncDecision {

    private val json = Json { encodeDefaults = true }

    /**
     * Content fingerprint of [data]: ignores the export timestamp and list order,
     * so two exports of the same data match.
     */
    fun fingerprint(data: BackupData): String {
        val normalized = data.copy(
            createdAt = "",
            transactions = data.transactions.sortedBy { it.id },
            recurringTransactions = data.recurringTransactions.sortedBy { it.id },
            budgets = data.budgets.sortedBy { it.id },
            deletedTransactions = data.deletedTransactions.sortedBy { it.id }
        )
        val bytes = json.encodeToString(BackupData.serializer(), normalized).toByteArray()
        return MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    fun isEmpty(data: BackupData): Boolean =
        data.transactions.isEmpty() && data.recurringTransactions.isEmpty() &&
            data.deletedTransactions.isEmpty()

    /**
     * @param local the phone's current data
     * @param drive Drive's current data, or null if Drive has no file
     * @param driveModifiedTime Drive's current `modifiedTime`, or null if no file
     * @param lastSync what was recorded at the last successful sync, or null if never
     *        (e.g. first sync after updating to this version)
     */
    fun decide(
        local: BackupData,
        drive: BackupData?,
        driveModifiedTime: String?,
        lastSync: SyncRecord?
    ): SyncAction {
        if (drive == null) return SyncAction.UPLOAD

        val localFp = fingerprint(local)
        if (localFp == fingerprint(drive)) return SyncAction.NOTHING

        if (lastSync == null) {
            // No history: only act automatically when one side is empty.
            return when {
                isEmpty(local) -> SyncAction.DOWNLOAD
                isEmpty(drive) -> SyncAction.UPLOAD
                else -> SyncAction.ASK
            }
        }

        val phoneChanged = localFp != lastSync.localFingerprint
        val driveChanged = driveModifiedTime != lastSync.driveModifiedTime
        return when {
            phoneChanged && driveChanged -> SyncAction.ASK
            phoneChanged -> SyncAction.UPLOAD
            driveChanged -> SyncAction.DOWNLOAD
            // Neither side changed by our records, yet the contents differ:
            // the records can't be trusted, so don't guess.
            else -> SyncAction.ASK
        }
    }
}
