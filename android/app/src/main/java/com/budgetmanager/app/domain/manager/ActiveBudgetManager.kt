package com.budgetmanager.app.domain.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.activeBudgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "active_budget"
)

@Singleton
class ActiveBudgetManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_ACTIVE_BUDGET_ID = longPreferencesKey("active_budget_id")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeBudgetId = MutableStateFlow(0L)

    init {
        scope.launch {
            val storedId = context.activeBudgetDataStore.data
                .map { prefs -> prefs[KEY_ACTIVE_BUDGET_ID] ?: 0L }
                .first()
            _activeBudgetId.value = storedId
        }
    }

    fun observeActiveBudgetId(): StateFlow<Long> = _activeBudgetId.asStateFlow()

    fun getActiveBudgetId(): Long = _activeBudgetId.value

    fun setActiveBudgetId(id: Long) {
        _activeBudgetId.value = id
        scope.launch {
            context.activeBudgetDataStore.edit { prefs ->
                prefs[KEY_ACTIVE_BUDGET_ID] = id
            }
        }
    }
}
