package com.factory.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.factory.core.common.auth.AuthState
import com.factory.core.designsystem.theme.ThemeMode

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    SettingsScreen(
        uiState = uiState,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onDynamicColorToggled = viewModel::onDynamicColorToggled,
        onRestorePurchases = viewModel::onRestorePurchases,
        onSignOut = viewModel::onSignOut,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    onRestorePurchases: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Environment: ${uiState.environmentName}",
            style = MaterialTheme.typography.bodyMedium,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(text = "Theme", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = uiState.themeMode == mode,
                            onClick = { onThemeModeSelected(mode) },
                        ),
                ) {
                    RadioButton(selected = uiState.themeMode == mode, onClick = { onThemeModeSelected(mode) })
                    Text(text = mode.name)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Dynamic color", modifier = Modifier.padding(top = 12.dp))
            Switch(checked = uiState.dynamicColorEnabled, onCheckedChange = onDynamicColorToggled)
        }

        if (uiState.purchasesEnabled) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(text = if (uiState.isPremium) "Premium: active" else "Premium: not active")
            TextButton(onClick = onRestorePurchases, enabled = !uiState.isRestoringPurchases) {
                Text(if (uiState.isRestoringPurchases) "Restoring..." else "Restore purchases")
            }
            uiState.restoreMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }

        if (uiState.authEnabled && uiState.authState is AuthState.SignedIn) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            TextButton(onClick = onSignOut) {
                Text("Sign out")
            }
        }
    }
}
