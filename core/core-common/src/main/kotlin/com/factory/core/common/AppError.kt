package com.factory.core.common

/**
 * The only error vocabulary feature code should reason about. Repository/ViewModel code
 * maps SDK-specific exceptions (Retrofit, Firebase, RevenueCat, ...) into one of these at
 * the boundary — never lets a raw [Throwable] cross into UI state. [message] is always
 * safe to log and, where noted, safe to show to a user; it never contains raw exception
 * text (which may include tokens, URLs, or stack details).
 */
sealed class AppError(open val message: String, open val cause: Throwable? = null) {

    data class Network(
        override val message: String = "Could not reach the server. Check your connection.",
        override val cause: Throwable? = null,
        val isOffline: Boolean = false,
    ) : AppError(message, cause)

    data class Auth(
        override val message: String,
        override val cause: Throwable? = null,
        val reason: AuthErrorReason = AuthErrorReason.UNKNOWN,
    ) : AppError(message, cause)

    data class Purchases(
        override val message: String,
        override val cause: Throwable? = null,
        val userCancelled: Boolean = false,
    ) : AppError(message, cause)

    data class Database(
        override val message: String = "A local storage error occurred.",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)

    data class Unknown(
        override val message: String = "Something went wrong.",
        override val cause: Throwable? = null,
    ) : AppError(message, cause)
}

enum class AuthErrorReason {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    NOT_CONFIGURED,
    USER_CANCELLED,
    NO_USER,
    UNKNOWN,
}
