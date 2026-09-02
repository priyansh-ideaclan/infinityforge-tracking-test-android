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

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val navigator: FactoryNavigator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun onEmailChanged(email: String) = _uiState.update { it.copy(email = email, errorMessage = null) }
    fun onPasswordChanged(password: String) = _uiState.update { it.copy(password = password, errorMessage = null) }
    fun onConfirmPasswordChanged(value: String) =
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter both email and password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.registerWithEmail(state.email, state.password)) {
                is AppResult.Success -> {
                    analyticsTracker.track(AnalyticsEvent.AuthRegistrationSucceeded(method = "email_password"))
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
