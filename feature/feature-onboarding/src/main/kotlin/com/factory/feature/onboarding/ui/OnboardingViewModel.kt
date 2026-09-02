package com.factory.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factory.core.analytics.AnalyticsEvent
import com.factory.core.analytics.AnalyticsTracker
import com.factory.core.common.FeatureFlag
import com.factory.core.common.FeatureFlagProvider
import com.factory.core.datastore.PreferenceKeys
import com.factory.core.datastore.PreferencesDataSource
import com.factory.core.navigation.FactoryDestination
import com.factory.core.navigation.FactoryNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The two-step default matches `APP_SPEC.yaml`'s `onboarding.steps`. This factory does
 * not parse YAML at runtime — `scripts/configure_app.py` is responsible for keeping
 * this list in sync with the spec when it differs from the template default (see
 * Docs/modules/feature-onboarding.md).
 */
data class OnboardingStep(val title: String, val body: String)

private val DEFAULT_STEPS = listOf(
    OnboardingStep(title = "Welcome", body = "Introduce your product in one sentence."),
    OnboardingStep(title = "What you can do", body = "Describe the core value in one sentence."),
)

data class OnboardingUiState(
    val steps: List<OnboardingStep> = DEFAULT_STEPS,
    val currentIndex: Int = 0,
) {
    val isLastStep: Boolean get() = currentIndex == steps.lastIndex
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val featureFlagProvider: FeatureFlagProvider,
    private val analyticsTracker: AnalyticsTracker,
    private val navigator: FactoryNavigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun onNext() {
        val state = _uiState.value
        if (!state.isLastStep) {
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
            return
        }
        finishOnboarding()
    }

    fun onSkip() = finishOnboarding()

    private fun finishOnboarding() {
        viewModelScope.launch {
            preferencesDataSource.setBoolean(PreferenceKeys.ONBOARDING_COMPLETED, true)
            analyticsTracker.track(AnalyticsEvent.OnboardingCompleted)
            val next = if (featureFlagProvider.isEnabled(FeatureFlag.AUTH)) {
                FactoryDestination.Login
            } else {
                FactoryDestination.Home
            }
            navigator.navigate(next, popUpToStart = true)
        }
    }
}
