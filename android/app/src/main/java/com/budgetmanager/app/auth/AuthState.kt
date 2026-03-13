package com.budgetmanager.app.auth

sealed class AuthState {
    data class SignedIn(
        val name: String,
        val email: String,
        val photoUrl: String? = null
    ) : AuthState()

    data object SignedOut : AuthState()
    data object Loading : AuthState()
}
