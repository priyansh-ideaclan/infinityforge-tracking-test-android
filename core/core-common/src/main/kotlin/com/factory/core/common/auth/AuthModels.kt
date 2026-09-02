package com.factory.core.common.auth

/** Provider-agnostic user model — never a Firebase `FirebaseUser` outside `feature-auth`'s Firebase implementation. */
data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val isAnonymous: Boolean,
)

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

enum class AuthProvider {
    EMAIL_PASSWORD,
    GOOGLE,
    ANONYMOUS,
}
