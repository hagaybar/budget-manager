package com.budgetmanager.app.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.budgetmanager.app.R
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private object Keys {
        val NAME = stringPreferencesKey("auth_name")
        val EMAIL = stringPreferencesKey("auth_email")
        val PHOTO_URL = stringPreferencesKey("auth_photo_url")
    }

    companion object {
        private const val TAG = "GoogleSignInManager"
    }

    init {
        scope.launch {
            restoreAuthState()
        }
    }

    private suspend fun restoreAuthState() {
        try {
            val prefs = context.authDataStore.data.first()
            val name = prefs[Keys.NAME]
            val email = prefs[Keys.EMAIL]
            val photoUrl = prefs[Keys.PHOTO_URL]

            _authState.value = if (name != null && email != null) {
                AuthState.SignedIn(name = name, email = email, photoUrl = photoUrl)
            } else {
                AuthState.SignedOut
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore auth state", e)
            _authState.value = AuthState.SignedOut
        }
    }

    /**
     * Initiates Google Sign-In using the Credential Manager API.
     *
     * @param activityContext Must be an Activity context (not Application context)
     *   because CredentialManager requires an Activity to show the sign-in UI.
     */
    suspend fun signInWithGoogle(activityContext: Context) {
        _isSigningIn.value = true
        _signInError.value = null

        try {
            val webClientId = context.getString(
                context.resources.getIdentifier(
                    "default_web_client_id", "string", context.packageName
                )
            )

            if (webClientId.isBlank() || webClientId == "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com") {
                Log.w(TAG, "Web client ID is not configured. Please set default_web_client_id in strings.xml")
                _signInError.value = context.getString(R.string.auth_error_not_configured)
                _isSigningIn.value = false
                return
            }

            val credentialManager = CredentialManager.create(activityContext)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled sign-in")
            _signInError.value = null // Don't show error for user cancellation
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No credentials available", e)
            _signInError.value = context.getString(R.string.auth_error_no_accounts)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.type}", e)
            _signInError.value = when {
                e.message?.contains("play services", ignoreCase = true) == true ->
                    context.getString(R.string.auth_error_play_services)
                e.message?.contains("network", ignoreCase = true) == true ->
                    context.getString(R.string.auth_error_network)
                else ->
                    context.getString(
                        R.string.auth_error_sign_in_failed,
                        e.message ?: context.getString(R.string.error_unknown)
                    )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in", e)
            _signInError.value = context.getString(R.string.auth_error_unexpected)
        } finally {
            _isSigningIn.value = false
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                        val displayName = googleIdTokenCredential.displayName ?: "Google User"
                        val email = googleIdTokenCredential.id // The email address
                        val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                        Log.d(TAG, "Sign-in successful")

                        val signedIn = AuthState.SignedIn(
                            name = displayName,
                            email = email,
                            photoUrl = photoUrl
                        )
                        _authState.value = signedIn
                        persistAuthState(signedIn)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Failed to parse Google ID token", e)
                        _signInError.value = context.getString(R.string.auth_error_parse_token)
                    }
                } else {
                    Log.w(TAG, "Unexpected credential type: ${credential.type}")
                    _signInError.value = context.getString(R.string.auth_error_unexpected_credential)
                }
            }
            else -> {
                Log.w(TAG, "Unrecognized credential type: ${credential::class.java.name}")
                _signInError.value = context.getString(R.string.auth_error_unrecognized)
            }
        }
    }

    fun continueAsGuest() {
        val guest = AuthState.SignedIn(
            name = "Guest User",
            email = "guest@local",
            photoUrl = null
        )
        _authState.value = guest
        _signInError.value = null
        scope.launch { persistAuthState(guest) }
    }

    fun signOut() {
        _authState.value = AuthState.SignedOut
        _signInError.value = null
        _accessToken.value = null
        scope.launch { clearAuthState() }
    }

    fun clearError() {
        _signInError.value = null
    }

    /**
     * Retrieves an OAuth2 access token with the drive.appdata scope.
     * Uses GoogleAuthUtil which works with any Google account on the device.
     * Will automatically show a consent screen on first use.
     */
    suspend fun getDriveAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val email = (_authState.value as? AuthState.SignedIn)?.email ?: return@withContext null
            if (email == "guest@local") return@withContext null
            val account = android.accounts.Account(email, "com.google")
            val token = GoogleAuthUtil.getToken(
                context,
                account,
                "oauth2:https://www.googleapis.com/auth/drive.appdata"
            )
            _accessToken.value = token
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Drive access token", e)
            _accessToken.value = null
            null
        }
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
