package com.factory.core.tracking

import android.content.Context
import com.factory.core.common.DispatcherProvider
import com.factory.core.common.EnvironmentConfig
import com.factory.core.logging.Logger
import com.factory.core.tracking.firebase.FirebaseInfinityForgeProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val TAG = "InfinityForgeTracking"

/**
 * Selects this module's providers the same way `AnalyticsModule` selects
 * `core-analytics`'s own tracker: a real `FirebaseInfinityForgeProvider` only when a
 * real `google-services.json` was present at build time (`FirebaseApp` actually
 * initialized); otherwise a [DebugLoggingInfinityForgeTrackingProvider] in debug
 * builds (so the contract-compliant core is still exercisable and inspectable without
 * a live vendor), or no provider at all in release (envelopes are still built and
 * validated — they are simply not sent anywhere).
 *
 * Unlike `core-analytics`, this module intentionally has no `APP_SPEC.yaml`-driven
 * on/off switch: InfinityForge Tracking is this dedicated test app's whole purpose, so
 * `InfinityForgeTrackingClientImpl` is always selected, never
 * [NoOpInfinityForgeTrackingClient] (kept in this module for parity with the reference
 * Swift/RN implementations and for tests, but not wired here). See
 * Docs/INFINITYFORGE_TRACKING.md for this and other intentional simplifications.
 */
@Module
@InstallIn(SingletonComponent::class)
object TrackingModule {

    @Provides
    @Singleton
    fun provideInfinityForgeTrackingClient(
        @ApplicationContext context: Context,
        identity: InfinityForgeIdentity,
        metadata: InfinityForgeMetadata,
        logger: Logger,
        environmentConfig: EnvironmentConfig,
        dispatcherProvider: DispatcherProvider,
    ): InfinityForgeTrackingClient {
        val base = metadata.base()
        val firebaseReady = isFirebaseInitialized(context)
        val providers: List<InfinityForgeTrackingProvider> = when {
            firebaseReady -> listOf(
                FirebaseInfinityForgeProvider(FirebaseAnalytics.getInstance(context), logger, base.environment),
            )
            environmentConfig.isDebug -> listOf(DebugLoggingInfinityForgeTrackingProvider(logger))
            else -> emptyList()
        }
        val availability = when {
            firebaseReady -> InfinityForgeTrackingAvailability.CONFIGURED
            environmentConfig.isDebug -> InfinityForgeTrackingAvailability.DEBUG
            else -> InfinityForgeTrackingAvailability.MISSING_CONFIGURATION
        }
        if (!firebaseReady) {
            logger.warn(
                TAG,
                "InfinityForge Tracking: no google-services.json configured; using " +
                    (if (environmentConfig.isDebug) "DebugLoggingInfinityForgeTrackingProvider" else "no provider") +
                    ". See Docs/setup/firebase.md.",
            )
        }
        return InfinityForgeTrackingClientImpl(availability, providers, identity, base, logger, dispatcherProvider)
    }

    private fun isFirebaseInitialized(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()
}
