package com.factory.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factory.core.analytics.AnalyticsEvent
import com.factory.core.analytics.AnalyticsTracker
import com.factory.core.common.AppResult
import com.factory.core.common.EnvironmentConfig
import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider
import com.factory.core.common.auth.AuthRepository
import com.factory.core.common.auth.AuthState
import com.factory.core.datastore.PreferenceKeys
import com.factory.core.datastore.PreferencesDataSource
import com.factory.core.designsystem.theme.ThemeMode
import com.factory.core.navigation.FactoryDestination
import com.factory.core.navigation.FactoryNavigator
import com.factory.purchases.api.PurchasesController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val isPremium: Boolean = false,
    val authState: AuthState = AuthState.Loading,
    val environmentName: String = "",
    val authEnabled: Boolean = false,
    val purchasesEnabled: Boolean = false,
    val isRestoringPurchases: Boolean = false,
    val restoreMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val purchasesController: PurchasesController,
    private val preferencesDataSource: PreferencesDataSource,
    private val featureFlagProvider: FeatureFlagProvider,
    private val environmentConfig: EnvironmentConfig,
    private val analyticsTracker: AnalyticsTracker,
    private val navigator: FactoryNavigator,
) : ViewModel() {

    private val isRestoringPurchases = MutableStateFlow(false)
    private val restoreMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            preferencesDataSource.stringFlow(PreferenceKeys.THEME_MODE, ThemeMode.SYSTEM.name),
            preferencesDataSource.booleanFlow(PreferenceKeys.DYNAMIC_COLOR_ENABLED, true),
            purchasesController.isPremium,
        ) { themeMode, dynamicColorEnabled, isPremium -> Triple(themeMode, dynamicColorEnabled, isPremium) },
        combine(
            authRepository.authState,
            isRestoringPurchases,
            restoreMessage,
        ) { authState, restoring, message -> Triple(authState, restoring, message) },
    ) { (themeMode, dynamicColorEnabled, isPremium), (authState, restoring, message) ->
        SettingsUiState(
            themeMode = ThemeMode.fromStorageValue(themeMode),
            dynamicColorEnabled = dynamicColorEnabled,
            isPremium = isPremium,
            authState = authState,
            isRestoringPurchases = restoring,
            restoreMessage = message,
            environmentName = environmentConfig.name,
            authEnabled = featureFlagProvider.isEnabled(FeatureFlag.AUTH),
            purchasesEnabled = featureFlagProvider.isEnabled(FeatureFlag.PURCHASES),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesDataSource.setString(PreferenceKeys.THEME_MODE, mode.name)
            analyticsTracker.track(AnalyticsEvent.SettingsThemeChanged(mode = mode.name))
        }
    }

    fun onDynamicColorToggled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setBoolean(PreferenceKeys.DYNAMIC_COLOR_ENABLED, enabled)
        }
    }

    fun onRestorePurchases() {
        viewModelScope.launch {
            isRestoringPurchases.value = true
            when (val result = purchasesController.restorePurchases()) {
                is AppResult.Success -> {
                    analyticsTracker.track(AnalyticsEvent.PurchaseRestored)
                    restoreMessage.value = "Purchases restored."
                }
                is AppResult.Failure -> restoreMessage.value = result.error.message
            }
            isRestoringPurchases.value = false
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            authRepository.signOut()
            analyticsTracker.track(AnalyticsEvent.AuthLogoutSucceeded)
            navigator.navigate(FactoryDestination.Login, popUpToStart = true)
        }
    }
}
