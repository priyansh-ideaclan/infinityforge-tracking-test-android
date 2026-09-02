package com.factory.core.common.auth

import com.factory.core.common.AppResult
import kotlinx.coroutines.flow.StateFlow

/**
 * The application-owned auth boundary. `feature-auth`'s UI/ViewModels depend on this
 * interface only; `FirebaseAuthRepository` (real) and `FakeAuthRepository`
 * (`core-testing`, and used at runtime when `auth.enabled: false`) are the two
 * implementations. No Firebase type appears in this file or in any caller of it.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun signInWithEmail(email: String, password: String): AppResult<AuthUser>
    suspend fun registerWithEmail(email: String, password: String): AppResult<AuthUser>
    suspend fun signInWithGoogle(idToken: String): AppResult<AuthUser>
    suspend fun signInAnonymously(): AppResult<AuthUser>
    suspend fun sendPasswordReset(email: String): AppResult<Unit>
    suspend fun signOut(): AppResult<Unit>
}
