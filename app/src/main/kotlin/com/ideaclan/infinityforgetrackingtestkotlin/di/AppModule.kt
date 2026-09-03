package com.ideaclan.infinityforgetrackingtestkotlin.di

import com.factory.ads.admob.IsDebugBuild
import com.factory.ads.admob.ProductionAdUnitIds
import com.factory.core.common.EnvironmentConfig
import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider
import com.factory.core.database.DatabaseName
import com.factory.feature.auth.di.GoogleWebClientId
import com.factory.purchases.revenuecat.PremiumEntitlementId
import com.ideaclan.infinityforgetrackingtestkotlin.AppSpecFlags
import com.ideaclan.infinityforgetrackingtestkotlin.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The app's composition root for every "external configuration" value that core/feature
 * modules need but must not read `BuildConfig`/`APP_SPEC.yaml` for themselves (see
 * ARCHITECTURE.md's Gradle-structure section and AGENTS.md §12). This is the *only*
 * file in the app that references `BuildConfig` fields or `AppSpecFlags`.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEnvironmentConfig(): EnvironmentConfig = EnvironmentConfig(
        name = BuildConfig.ENVIRONMENT_NAME,
        baseUrl = BuildConfig.BASE_URL,
        isDebug = BuildConfig.DEBUG,
        googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
    )

    @Provides
    @Singleton
    fun provideFeatureFlagProvider(): FeatureFlagProvider = object : FeatureFlagProvider {
        override fun isEnabled(flag: FeatureFlag): Boolean = when (flag) {
            FeatureFlag.AUTH -> AppSpecFlags.AUTH_ENABLED
            FeatureFlag.AUTH_EMAIL_PASSWORD -> AppSpecFlags.AUTH_EMAIL_PASSWORD
            FeatureFlag.AUTH_GOOGLE -> AppSpecFlags.AUTH_GOOGLE
            FeatureFlag.AUTH_ANONYMOUS -> AppSpecFlags.AUTH_ANONYMOUS
            FeatureFlag.ONBOARDING -> AppSpecFlags.ONBOARDING_ENABLED
            FeatureFlag.ADS -> AppSpecFlags.ADS_ENABLED
            FeatureFlag.PURCHASES -> AppSpecFlags.PURCHASES_ENABLED
            FeatureFlag.ANALYTICS -> AppSpecFlags.ANALYTICS_ENABLED
            FeatureFlag.CRASH_REPORTING -> AppSpecFlags.CRASH_REPORTING_ENABLED
        }
    }

    @Provides
    @Singleton
    fun provideDatabaseName(): DatabaseName = DatabaseName(value = "infinityforge_tracking_test.db")

    @Provides
    @Singleton
    fun provideGoogleWebClientId(): GoogleWebClientId = GoogleWebClientId(value = BuildConfig.GOOGLE_WEB_CLIENT_ID)

    @Provides
    @Singleton
    fun provideProductionAdUnitIds(): ProductionAdUnitIds = ProductionAdUnitIds()

    @Provides
    @Singleton
    fun providePremiumEntitlementId(): PremiumEntitlementId = PremiumEntitlementId(value = "premium")

    @Provides
    @Singleton
    fun provideIsDebugBuild(): IsDebugBuild = IsDebugBuild(value = BuildConfig.DEBUG)
}
