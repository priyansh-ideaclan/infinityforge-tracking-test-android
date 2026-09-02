package com.factory.feature.auth.firebase

import com.factory.core.common.AppError
import com.factory.core.common.AppResult
import com.factory.core.common.AuthErrorReason
import com.factory.core.common.auth.AuthRepository
import com.factory.core.common.auth.AuthState
import com.factory.core.common.auth.AuthUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real Firebase Authentication implementation. Never referenced by feature UI directly
 * — only bound here, in `AuthModule`, behind [AuthRepository]. If `google-services.json`
 * is missing, `FirebaseAuth.getInstance()` still constructs, but every call fails with a
 * configuration error rather than crashing (see [runCatchingAuthCall]).
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val googleWebClientId: String,
    scope: CoroutineScope,
) : AuthRepository {

    override val authState: StateFlow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser.toAuthState())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.stateIn(scope, SharingStarted.Eagerly, firebaseAuth.currentUser.toAuthState())

    override suspend fun signInWithEmail(email: String, password: String): AppResult<AuthUser> =
        runCatchingAuthCall {
            suspendCancellableCoroutine { continuation ->
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { continuation.resume(it.user.toAuthUser()) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        }

    override suspend fun registerWithEmail(email: String, password: String): AppResult<AuthUser> =
        runCatchingAuthCall {
            suspendCancellableCoroutine { continuation ->
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { continuation.resume(it.user.toAuthUser()) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        }

    override suspend fun signInWithGoogle(idToken: String): AppResult<AuthUser> {
        if (googleWebClientId.isBlank()) {
            return AppResult.Failure(
                AppError.Auth(
                    message = "Google sign-in is not configured for this build.",
                    reason = AuthErrorReason.NOT_CONFIGURED,
                ),
            )
        }
        return runCatchingAuthCall {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            suspendCancellableCoroutine { continuation ->
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { continuation.resume(it.user.toAuthUser()) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        }
    }

    override suspend fun signInAnonymously(): AppResult<AuthUser> = runCatchingAuthCall {
        suspendCancellableCoroutine { continuation ->
            firebaseAuth.signInAnonymously()
                .addOnSuccessListener { continuation.resume(it.user.toAuthUser()) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> = runCatchingAuthCall {
        suspendCancellableCoroutine { continuation ->
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    override suspend fun signOut(): AppResult<Unit> {
        firebaseAuth.signOut()
        return AppResult.Success(Unit)
    }

    // Last-resort boundary for any Firebase Auth exception the specific catches above
    // don't name — never lets a raw exception escape into AuthRepository's callers.
    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> runCatchingAuthCall(block: () -> T): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AppResult.Failure(
                AppError.Auth(
                    message = "Incorrect email or password.",
                    cause = e,
                    reason = AuthErrorReason.INVALID_CREDENTIALS,
                ),
            )
        } catch (e: FirebaseAuthUserCollisionException) {
            AppResult.Failure(
                AppError.Auth(
                    message = "An account with this email already exists.",
                    cause = e,
                    reason = AuthErrorReason.EMAIL_ALREADY_IN_USE,
                ),
            )
        } catch (e: FirebaseAuthWeakPasswordException) {
            AppResult.Failure(
                AppError.Auth(message = "Password is too weak.", cause = e, reason = AuthErrorReason.WEAK_PASSWORD),
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.Auth(message = e.message ?: "Authentication failed.", cause = e))
        }
}

private fun FirebaseUser?.toAuthState(): AuthState =
    this?.let { AuthState.SignedIn(it.toAuthUser()) } ?: AuthState.SignedOut

private fun FirebaseUser?.toAuthUser(): AuthUser {
    val user = requireNotNull(this) { "FirebaseUser was null after a successful auth call." }
    return AuthUser(
        id = user.uid,
        email = user.email,
        displayName = user.displayName,
        isAnonymous = user.isAnonymous,
    )
}
