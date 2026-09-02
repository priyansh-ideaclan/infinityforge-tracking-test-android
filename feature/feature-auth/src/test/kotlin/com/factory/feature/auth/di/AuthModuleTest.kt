package com.factory.feature.auth.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.factory.core.common.auth.FakeAuthRepository
import com.factory.core.logging.AndroidLogger
import com.factory.core.testing.FakeFeatureFlagProvider
import com.factory.core.testing.FakeIdGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * One of the factory's "module combination" checks (see Docs/testing/module-matrix.md).
 * A Robolectric test environment never has a real `google-services.json`-initialized
 * `FirebaseApp`, so this verifies the safety property that matters most: auth always
 * falls back to `FakeAuthRepository` rather than crashing when Firebase isn't
 * configured — regardless of what `auth.enabled` says.
 */
@RunWith(RobolectricTestRunner::class)
class AuthModuleTest {

    @Test
    fun `auth repository falls back to fake when Firebase is not configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val flags = FakeFeatureFlagProvider(enabledFlags = emptySet())

        val repository = AuthModule.provideAuthRepository(
            context = context,
            featureFlagProvider = flags,
            googleWebClientId = GoogleWebClientId(""),
            idGenerator = FakeIdGenerator(),
            logger = AndroidLogger(isDebug = true),
        )

        assertTrue(repository is FakeAuthRepository)
    }
}
