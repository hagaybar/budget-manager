package com.budgetmanager.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.auth.AuthState
import com.budgetmanager.app.auth.GoogleSignInManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager
) : ViewModel() {

    val authState: StateFlow<AuthState> = googleSignInManager.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    val signInError: StateFlow<String?> = googleSignInManager.signInError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isSigningIn: StateFlow<Boolean> = googleSignInManager.isSigningIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Initiates Google Sign-In. Must be called with an Activity context
     * because Credential Manager requires an Activity to display the sign-in UI.
     */
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            googleSignInManager.signInWithGoogle(activityContext)
        }
    }

    fun continueAsGuest() {
        googleSignInManager.continueAsGuest()
    }

    fun signOut() {
        googleSignInManager.signOut()
    }

    fun clearError() {
        googleSignInManager.clearError()
    }
}
