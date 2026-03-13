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
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager
) : ViewModel() {

    val authState: StateFlow<AuthState> = googleSignInManager.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    fun signInWithGoogle(context: Context) {
        googleSignInManager.signInWithGoogle(context)
    }

    fun continueAsGuest() {
        googleSignInManager.continueAsGuest()
    }

    fun signOut() {
        googleSignInManager.signOut()
    }
}
