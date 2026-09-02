package com.factory.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factory.core.designsystem.component.FactoryLoadingIndicator

@Composable
fun LoginRoute(viewModel: LoginViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LoginScreen(
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSubmit = viewModel::onSubmit,
        onSignInAnonymously = viewModel::onSignInAnonymously,
        onGoToRegister = viewModel::onGoToRegister,
        onGoToForgotPassword = viewModel::onGoToForgotPassword,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onSignInAnonymously: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoToForgotPassword: () -> Unit,
) {
    if (uiState.isLoading) {
        FactoryLoadingIndicator(label = "Signing in")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Sign in", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChanged,
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginScreenTestTags.EMAIL_FIELD),
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.testTag(LoginScreenTestTags.PASSWORD_FIELD),
        )

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(LoginScreenTestTags.ERROR_TEXT),
            )
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier.testTag(LoginScreenTestTags.SUBMIT_BUTTON),
        ) {
            Text("Sign in")
        }

        TextButton(onClick = onSignInAnonymously) {
            Text("Continue as guest")
        }

        TextButton(onClick = onGoToRegister) {
            Text("Create an account")
        }

        TextButton(onClick = onGoToForgotPassword) {
            Text("Forgot password?")
        }
    }
}
