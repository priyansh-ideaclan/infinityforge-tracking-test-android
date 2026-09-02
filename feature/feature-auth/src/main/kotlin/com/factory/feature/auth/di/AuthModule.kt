package com.factory.feature.auth.di

import android.content.Context
import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider
import com.factory.core.common.IdGenerator
import com.factory.core.common.auth.AuthRepository
import com.factory.core.common.auth.FakeAuthRepository
import com.factory.core.logging.Logger
import com.factory.feature.auth.firebase.FirebaseAuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private const val TAG = "AuthModule"

/**
 * Google Sign-In web client ID, sourced from `BuildConfig.GOOGLE_WEB_CLIENT_ID` (itself
 * from external configuration — see `Docs/setup/firebase.md`). Wrapped so Hilt can
 * inject a plain string without ambiguity with other strings.
 */
data class GoogleWebClientId(val value: String)

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        featureFlagProvider: FeatureFlagProvider,
        googleWebClientId: GoogleWebClientId,
        idGenerator: IdGenerator,
        logger: Logger,
    ): AuthRepository {
        if (!featureFlagProvider.isEnabled(FeatureFlag.AUTH) || FirebaseApp.getApps(context).isEmpty()) {
            logger.warn(TAG, "Auth disabled or Firebase not configured; using FakeAuthRepository.")
            return FakeAuthRepository(idGenerator)
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return FirebaseAuthRepository(FirebaseAuth.getInstance(), googleWebClientId.value, scope)
    }
}
