package com.factory.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    OnboardingScreen(uiState = uiState, onNext = viewModel::onNext, onSkip = viewModel::onSkip)
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val step = uiState.steps[uiState.currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = step.title, style = MaterialTheme.typography.titleLarge)
        Text(text = step.body, style = MaterialTheme.typography.bodyLarge)

        Button(onClick = onNext, modifier = Modifier.padding(top = 24.dp)) {
            Text(if (uiState.isLastStep) "Get started" else "Next")
        }

        if (!uiState.isLastStep) {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
    }
}
