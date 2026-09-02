package com.ideaclan.infinityforgetrackingtestkotlin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider
import com.factory.core.common.auth.AuthRepository
import com.factory.core.common.auth.AuthState
import com.factory.core.datastore.PreferenceKeys
import com.factory.core.datastore.PreferencesDataSource
import com.factory.core.designsystem.theme.ThemeMode
import com.factory.core.navigation.FactoryDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    preferencesDataSource: PreferencesDataSource,
    featureFlagProvider: FeatureFlagProvider,
    authRepository: AuthRepository,
) : ViewModel() {

    /** Null while still resolving (auth state not yet known) — keep the splash/loading UI up. */
    val startDestination: StateFlow<FactoryDestination?> = combine(
        preferencesDataSource.booleanFlow(PreferenceKeys.ONBOARDING_COMPLETED, false),
        authRepository.authState,
    ) { onboardingCompleted, authState ->
        when {
            featureFlagProvider.isEnabled(FeatureFlag.ONBOARDING) && !onboardingCompleted ->
                FactoryDestination.Onboarding
            featureFlagProvider.isEnabled(FeatureFlag.AUTH) && authState is AuthState.SignedOut ->
                FactoryDestination.Login
            authState is AuthState.Loading -> null
            else -> FactoryDestination.Home
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode: StateFlow<ThemeMode> =
        preferencesDataSource.stringFlow(PreferenceKeys.THEME_MODE, ThemeMode.SYSTEM.name)
            .map { ThemeMode.fromStorageValue(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val dynamicColorEnabled: StateFlow<Boolean> =
        preferencesDataSource.booleanFlow(PreferenceKeys.DYNAMIC_COLOR_ENABLED, true)
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)
}
