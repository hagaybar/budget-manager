package com.budgetmanager.app.data.sync

import com.budgetmanager.app.domain.model.BackupData
import com.budgetmanager.app.domain.model.BackupTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.serialization.json.Json

class SyncDecisionTest {

    private fun tx(id: Long, amount: Double = 10.0) =
        BackupTransaction(id = id, type = "expense", amount = amount, category = "food", date = "2026-11-01")

    private fun data(vararg txs: BackupTransaction, createdAt: String = "2026-11-17T10:00") =
        BackupData(createdAt = createdAt, transactions = txs.toList(), recurringTransactions = emptyList())

    private val base = data(tx(1), tx(2))
    private val phoneEdited = data(tx(1), tx(2), tx(3))
    private val webEdited = data(tx(1), tx(2, amount = 99.0))
    private fun record(of: BackupData, mt: String = "T1") = SyncRecord(SyncDecision.fingerprint(of), mt)

    @Test fun `fingerprint ignores export time and order`() {
        assertEquals(
            SyncDecision.fingerprint(data(tx(1), tx(2), createdAt = "a")),
            SyncDecision.fingerprint(data(tx(2), tx(1), createdAt = "b"))
        )
    }

    @Test fun `no Drive file uploads`() =
        assertEquals(SyncAction.UPLOAD, SyncDecision.decide(base, null, null, record(base)))

    @Test fun `identical data does nothing even without history`() =
        assertEquals(SyncAction.NOTHING, SyncDecision.decide(base, data(tx(2), tx(1)), "T9", null))

    @Test fun `only phone changed uploads`() =
        assertEquals(SyncAction.UPLOAD, SyncDecision.decide(phoneEdited, base, "T1", record(base)))

    @Test fun `only Drive changed downloads`() =
        assertEquals(SyncAction.DOWNLOAD, SyncDecision.decide(base, webEdited, "T2", record(base)))

    @Test fun `both changed asks`() =
        assertEquals(SyncAction.ASK, SyncDecision.decide(phoneEdited, webEdited, "T2", record(base)))

    @Test fun `no history and both have different data asks`() =
        assertEquals(SyncAction.ASK, SyncDecision.decide(phoneEdited, webEdited, "T2", null))

    @Test fun `no history and empty phone downloads`() =
        assertEquals(SyncAction.DOWNLOAD, SyncDecision.decide(data(), base, "T2", null))

    @Test fun `no history and empty Drive uploads`() =
        assertEquals(SyncAction.UPLOAD, SyncDecision.decide(base, data(), "T2", null))

    @Test fun `contents differ but records say nothing changed asks`() =
        assertEquals(SyncAction.ASK, SyncDecision.decide(base, webEdited, "T1", record(base)))

    @Test fun `reads a Drive file written by the web app`() {
        // Shape written by app/static/index.html writeToDrive(): no top-level created_at.
        val web = """
            {"transactions":[{"id":1,"type":"expense","amount":12.5,"category":"food","description":"",
              "date":"2026-11-01","created_at":"2026-11-01T10:00:00.000Z","recurring_id":null,"budget_id":1}],
             "recurring_transactions":[],"deleted_transactions":[],
             "budgets":[{"id":1,"name":"My Budget","description":"","currency":"ILS","monthly_target":0,
              "is_active":1,"created_at":"2026-11-01T10:00:00.000Z"}],
             "last_modified":"2026-11-17T11:00:00.000Z"}
        """.trimIndent()
        val data = Json { ignoreUnknownKeys = true }.decodeFromString(BackupData.serializer(), web)
        assertEquals(1, data.transactions.size)
        assertEquals(1, data.budgets.size)
    }
}
