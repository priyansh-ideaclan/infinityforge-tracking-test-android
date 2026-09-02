package com.factory.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factory.core.designsystem.component.FactoryLoadingIndicator

@Composable
fun ForgotPasswordRoute(viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    ForgotPasswordScreen(
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onSubmit = viewModel::onSubmit,
    )
}

@Composable
fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    if (uiState.isLoading) {
        FactoryLoadingIndicator(label = "Sending reset email")
        return
    }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Reset your password", style = MaterialTheme.typography.titleLarge)

        if (uiState.emailSent) {
            Text("If an account exists for this email, a reset link has been sent.")
            return@Column
        }

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChanged,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.errorMessage != null) {
            Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Button(onClick = onSubmit) {
            Text("Send reset email")
        }
    }
}
