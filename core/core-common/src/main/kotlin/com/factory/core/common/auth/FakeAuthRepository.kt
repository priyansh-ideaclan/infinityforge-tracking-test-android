package com.factory.core.common.auth

import com.factory.core.common.AppError
import com.factory.core.common.AppResult
import com.factory.core.common.AuthErrorReason
import com.factory.core.common.IdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * A real, always-available implementation with no Firebase dependency — used both by
 * tests and, at runtime, whenever `APP_SPEC.yaml`'s `auth.enabled` is false or
 * `auth.providers` selects no real provider. Keeps an in-memory user so sign-in/out and
 * "current user" UI states are genuinely exercisable without any backend.
 */
class FakeAuthRepository @Inject constructor(
    private val idGenerator: IdGenerator,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState

    private val registeredEmails = mutableMapOf<String, String>() // email -> password

    override suspend fun signInWithEmail(email: String, password: String): AppResult<AuthUser> {
        val storedPassword = registeredEmails[email]
        return if (storedPassword == null || storedPassword != password) {
            AppResult.Failure(
                AppError.Auth(
                    message = "Incorrect email or password.",
                    reason = AuthErrorReason.INVALID_CREDENTIALS,
                ),
            )
        } else {
            signIn(email = email, isAnonymous = false)
        }
    }

    override suspend fun registerWithEmail(email: String, password: String): AppResult<AuthUser> {
        if (registeredEmails.containsKey(email)) {
            return AppResult.Failure(
                AppError.Auth(
                    message = "An account with this email already exists.",
                    reason = AuthErrorReason.EMAIL_ALREADY_IN_USE,
                ),
            )
        }
        registeredEmails[email] = password
        return signIn(email = email, isAnonymous = false)
    }

    override suspend fun signInWithGoogle(idToken: String): AppResult<AuthUser> =
        signIn(email = "google-user@example.com", isAnonymous = false)

    override suspend fun signInAnonymously(): AppResult<AuthUser> =
        signIn(email = null, isAnonymous = true)

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> =
        if (registeredEmails.containsKey(email)) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(
                AppError.Auth(message = "No account found for this email.", reason = AuthErrorReason.NO_USER),
            )
        }

    override suspend fun signOut(): AppResult<Unit> {
        _authState.value = AuthState.SignedOut
        return AppResult.Success(Unit)
    }

    private fun signIn(email: String?, isAnonymous: Boolean): AppResult<AuthUser> {
        val user = AuthUser(
            id = idGenerator.newId(),
            email = email,
            displayName = email?.substringBefore("@"),
            isAnonymous = isAnonymous,
        )
        _authState.value = AuthState.SignedIn(user)
        return AppResult.Success(user)
    }
}
