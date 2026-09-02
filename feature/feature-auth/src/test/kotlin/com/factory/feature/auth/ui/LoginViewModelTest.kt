package com.factory.feature.auth.ui

import app.cash.turbine.test
import com.factory.core.common.auth.FakeAuthRepository
import com.factory.core.navigation.DefaultFactoryNavigator
import com.factory.core.navigation.FactoryDestination
import com.factory.core.navigation.NavigationCommand
import com.factory.core.testing.FakeAnalyticsTracker
import com.factory.core.testing.FakeIdGenerator
import com.factory.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(FakeIdGenerator())
    private val navigator = DefaultFactoryNavigator()
    private val analyticsTracker = FakeAnalyticsTracker()
    private val viewModel = LoginViewModel(authRepository, analyticsTracker, navigator)

    @Test
    fun `submitting with blank fields shows an error and does not call the repository`() {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")

        viewModel.onSubmit()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `successful sign in navigates to Home and tracks an analytics event`() = runTest {
        authRepository.registerWithEmail("a@example.com", "password123")
        authRepository.signOut()
        viewModel.onEmailChanged("a@example.com")
        viewModel.onPasswordChanged("password123")

        navigator.commands.test {
            viewModel.onSubmit()

            val command = awaitItem() as NavigationCommand.NavigateTo
            assertEquals(FactoryDestination.Home, command.destination)
        }
        assertEquals(1, analyticsTracker.trackedEvents.size)
    }

    @Test
    fun `failed sign in surfaces the repository error`() {
        viewModel.onEmailChanged("nobody@example.com")
        viewModel.onPasswordChanged("wrong")

        viewModel.onSubmit()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }
}
