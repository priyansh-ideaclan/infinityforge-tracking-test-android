package com.factory.core.network

import com.factory.core.common.AppError
import com.factory.core.common.AppResult
import com.factory.core.logging.Logger
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private class RecordingLogger : Logger {
    val warnings = mutableListOf<String>()
    val errors = mutableListOf<String>()
    override fun debug(tag: String, message: String) = Unit
    override fun info(tag: String, message: String) = Unit
    override fun warn(tag: String, message: String, throwable: Throwable?) {
        warnings += message
    }
    override fun error(tag: String, message: String, throwable: Throwable?) {
        errors += message
    }
}

class SafeApiCallTest {

    @Test
    fun `successful call returns Success`() = runTest {
        val logger = RecordingLogger()

        val result = safeApiCall(logger) { "ok" }

        assertEquals(AppResult.Success("ok"), result)
    }

    @Test
    fun `IOException maps to offline Network error`() = runTest {
        val logger = RecordingLogger()

        val result = safeApiCall<Unit>(logger) { throw IOException("no connection") }

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is AppError.Network)
        assertTrue((error as AppError.Network).isOffline)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun `HttpException maps to Network error carrying the status code`() = runTest {
        val logger = RecordingLogger()
        val response = Response.error<Unit>(404, "not found".toResponseBody("text/plain".toMediaType()))

        val result = safeApiCall<Unit>(logger) { throw HttpException(response) }

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error as AppError.Network
        assertTrue(error.message.contains("404"))
    }

    @Test
    fun `unexpected exception maps to Unknown error and is logged as error`() = runTest {
        val logger = RecordingLogger()

        val result = safeApiCall<Unit>(logger) { throw IllegalStateException("boom") }

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Unknown)
        assertEquals(1, logger.errors.size)
    }
}
