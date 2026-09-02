package com.factory.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factory.core.analytics.AnalyticsEvent
import com.factory.core.analytics.AnalyticsTracker
import com.factory.core.common.AppResult
import com.factory.core.common.auth.AuthRepository
import com.factory.core.navigation.FactoryDestination
import com.factory.core.navigation.FactoryNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val navigator: FactoryNavigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onGoToRegister() {
        viewModelScope.launch { navigator.navigate(FactoryDestination.Register) }
    }

    fun onGoToForgotPassword() {
        viewModelScope.launch { navigator.navigate(FactoryDestination.ForgotPassword) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter both email and password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signInWithEmail(state.email, state.password)) {
                is AppResult.Success -> {
                    analyticsTracker.track(AnalyticsEvent.AuthLoginSucceeded(method = "email_password"))
                    _uiState.update { it.copy(isLoading = false) }
                    navigator.navigate(FactoryDestination.Home, popUpToStart = true)
                }
                is AppResult.Failure -> {
                    analyticsTracker.track(
                        AnalyticsEvent.AuthLoginFailed(method = "email_password", reason = result.error.message),
                    )
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun onSignInAnonymously() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signInAnonymously()) {
                is AppResult.Success -> {
                    analyticsTracker.track(AnalyticsEvent.AuthLoginSucceeded(method = "anonymous"))
                    _uiState.update { it.copy(isLoading = false) }
                    navigator.navigate(FactoryDestination.Home, popUpToStart = true)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }
}
