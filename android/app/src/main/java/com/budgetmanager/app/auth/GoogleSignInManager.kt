package com.budgetmanager.app.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class GoogleSignInManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private object Keys {
        val NAME = stringPreferencesKey("auth_name")
        val EMAIL = stringPreferencesKey("auth_email")
        val PHOTO_URL = stringPreferencesKey("auth_photo_url")
    }

    init {
        scope.launch {
            restoreAuthState()
        }
    }

    private suspend fun restoreAuthState() {
        val prefs = context.authDataStore.data.first()
        val name = prefs[Keys.NAME]
        val email = prefs[Keys.EMAIL]
        val photoUrl = prefs[Keys.PHOTO_URL]

        _authState.value = if (name != null && email != null) {
            AuthState.SignedIn(name = name, email = email, photoUrl = photoUrl)
        } else {
            AuthState.SignedOut
        }
    }

    fun signInWithGoogle(context: Context) {
        // Demo/guest mode: works without Google credentials for APK testing
        // In production, this would use Credential Manager API
        val signedIn = AuthState.SignedIn(
            name = "Demo User",
            email = "demo@budgetmanager.app",
            photoUrl = null
        )
        _authState.value = signedIn
        scope.launch { persistAuthState(signedIn) }
    }

    fun continueAsGuest() {
        val guest = AuthState.SignedIn(
            name = "Guest User",
            email = "guest@local",
            photoUrl = null
        )
        _authState.value = guest
        scope.launch { persistAuthState(guest) }
    }

    fun signOut() {
        _authState.value = AuthState.SignedOut
        scope.launch { clearAuthState() }
    }

    private suspend fun persistAuthState(state: AuthState.SignedIn) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.NAME] = state.name
            prefs[Keys.EMAIL] = state.email
            state.photoUrl?.let { prefs[Keys.PHOTO_URL] = it } ?: prefs.remove(Keys.PHOTO_URL)
        }
    }

    private suspend fun clearAuthState() {
        context.authDataStore.edit { it.clear() }
    }
}
