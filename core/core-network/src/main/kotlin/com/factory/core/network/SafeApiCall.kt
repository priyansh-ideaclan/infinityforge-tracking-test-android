package com.factory.core.network

import com.factory.core.common.AppError
import com.factory.core.common.AppResult
import com.factory.core.logging.Logger
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "SafeApiCall"

/**
 * Every Retrofit call in every repository goes through this — it is the one place that
 * turns [IOException] (offline/timeout), [HttpException] (4xx/5xx), and anything else
 * into the [AppError] vocabulary feature code understands. Repositories never catch
 * Retrofit exceptions directly.
 */
// This is the designated last-resort boundary that turns *any* remaining exception
// from a network call into AppError.Unknown instead of crashing — narrower catches
// above already handle the expected IOException/HttpException cases.
@Suppress("TooGenericExceptionCaught")
suspend fun <T> safeApiCall(logger: Logger, block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: IOException) {
    logger.warn(TAG, "Network IO failure", e)
    AppResult.Failure(AppError.Network(isOffline = true, cause = e))
} catch (e: HttpException) {
    logger.warn(TAG, "HTTP ${e.code()} from server", e)
    AppResult.Failure(
        AppError.Network(
            message = "The server returned an error (${e.code()}).",
            cause = e,
        ),
    )
} catch (e: Exception) {
    logger.error(TAG, "Unexpected error during API call", e)
    AppResult.Failure(AppError.Unknown(cause = e))
}
