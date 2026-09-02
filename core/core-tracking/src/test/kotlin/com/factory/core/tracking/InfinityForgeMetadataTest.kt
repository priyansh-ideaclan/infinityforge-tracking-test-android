package com.factory.core.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.factory.core.common.EnvironmentConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class InfinityForgeMetadataTest {

    private fun metadata(environmentName: String, logger: RecordingLogger = RecordingLogger()): InfinityForgeMetadata {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val environmentConfig = EnvironmentConfig(
            name = environmentName,
            baseUrl = "https://example.com/",
            isDebug = true,
            googleWebClientId = "",
        )
        return InfinityForgeMetadata(context, environmentConfig, logger)
    }

    @Test
    fun `platform is always android`() {
        assertEquals(InfinityForgePlatform.ANDROID, metadata("dev").platform())
    }

    @Test
    fun `dev environment name maps to development`() {
        assertEquals(InfinityForgeEnvironment.DEVELOPMENT, metadata("dev").environment())
    }

    @Test
    fun `staging environment name maps to preview`() {
        assertEquals(InfinityForgeEnvironment.PREVIEW, metadata("staging").environment())
    }

    @Test
    fun `prod environment name maps to production`() {
        assertEquals(InfinityForgeEnvironment.PRODUCTION, metadata("prod").environment())
    }

    @Test
    fun `an unrecognized environment name falls back to production and logs a diagnostic`() {
        val logger = RecordingLogger()
        val result = metadata("qa", logger).environment()

        assertEquals(InfinityForgeEnvironment.PRODUCTION, result)
        assertTrue(logger.messages.any { it.contains("qa") })
    }

    @Test
    fun `base() reads a non-blank app_id and app_version from PackageInfo`() {
        val base = metadata("dev").base()

        assertTrue(base.appId.isNotBlank())
        assertTrue(base.appVersion.isNotBlank())
        assertEquals("kotlin", base.sdkName)
        assertEquals(InfinityForgeEnvironment.DEVELOPMENT, base.environment)
        assertEquals(InfinityForgePlatform.ANDROID, base.platform)
    }

    @Test
    fun `timestamp is ISO 8601 UTC with a Z suffix`() {
        val instant = Instant.parse("2026-01-01T12:00:00Z")
        val timestamp = InfinityForgeMetadata.timestamp(instant)

        assertEquals("2026-01-01T12:00:00Z", timestamp)
    }
}
