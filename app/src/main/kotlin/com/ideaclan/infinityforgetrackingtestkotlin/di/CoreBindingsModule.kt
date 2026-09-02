package com.ideaclan.infinityforgetrackingtestkotlin.di

import com.factory.ads.admob.IsDebugBuild
import com.factory.core.common.Clock
import com.factory.core.common.DefaultDispatcherProvider
import com.factory.core.common.DispatcherProvider
import com.factory.core.common.IdGenerator
import com.factory.core.common.SystemClock
import com.factory.core.common.UuidIdGenerator
import com.factory.core.logging.AndroidLogger
import com.factory.core.logging.Logger
import com.factory.core.navigation.DefaultFactoryNavigator
import com.factory.core.navigation.FactoryNavigator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every `core-*` capability interface to its default implementation. These are
 * app-wide, provider-agnostic defaults (unlike `AuthRepository`/`AdsController`/
 * `PurchasesController`, which are bound per-`APP_SPEC.yaml` flag in their own
 * feature/vendor modules) — this module never changes based on `AppSpecFlags`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindingsModule {

    @Binds
    abstract fun bindIdGenerator(impl: UuidIdGenerator): IdGenerator

    @Binds
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    abstract fun bindFactoryNavigator(impl: DefaultFactoryNavigator): FactoryNavigator

    companion object {
        @Provides
        @Singleton
        fun provideLogger(isDebugBuild: IsDebugBuild): Logger = AndroidLogger(isDebugBuild.value)
    }
}
