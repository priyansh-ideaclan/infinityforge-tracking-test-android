package com.factory.core.analytics

import android.content.Context
import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider
import com.factory.core.logging.Logger
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val TAG = "AnalyticsModule"

/**
 * Selects the real Firebase-backed tracker/reporter only when **both** conditions hold:
 * `APP_SPEC.yaml` turned the capability on (via [FeatureFlagProvider]) **and**
 * `FirebaseApp` actually initialized (i.e. a real `google-services.json` was present at
 * build time — see the conditional plugin application in `app/build.gradle.kts`).
 * Otherwise this falls back to the no-op implementation and logs why, rather than
 * crashing or silently pretending analytics/crash reporting is live.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        @ApplicationContext context: Context,
        featureFlagProvider: FeatureFlagProvider,
        logger: Logger,
    ): AnalyticsTracker {
        if (!featureFlagProvider.isEnabled(FeatureFlag.ANALYTICS) || !isFirebaseInitialized(context)) {
            logger.warn(TAG, "Analytics disabled or Firebase not configured; using NoOpAnalyticsTracker.")
            return NoOpAnalyticsTracker()
        }
        return FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context))
    }

    @Provides
    @Singleton
    fun provideCrashReporter(
        @ApplicationContext context: Context,
        featureFlagProvider: FeatureFlagProvider,
        logger: Logger,
    ): CrashReporter {
        if (!featureFlagProvider.isEnabled(FeatureFlag.CRASH_REPORTING) || !isFirebaseInitialized(context)) {
            logger.warn(TAG, "Crash reporting disabled or Firebase not configured; using NoOpCrashReporter.")
            return NoOpCrashReporter()
        }
        return FirebaseCrashReporter(FirebaseCrashlytics.getInstance())
    }

    private fun isFirebaseInitialized(context: Context): Boolean =
        FirebaseApp.getApps(context).isNotEmpty()
}
