package com.ideaclan.infinityforgetrackingtestkotlin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.factory.core.navigation.FactoryDestination

/**
 * A validation-only screen, not a product feature — exists solely so this test app's
 * real navigation graph has a third destination beyond Home/Settings to exercise
 * InfinityForge automatic screen tracking against (see FactoryNavHost.kt's
 * DisposableEffect and core-navigation's InfinityForgeScreenTracking.kt). Reached from
 * Settings via the "Open Profile (test)" affordance added directly in FactoryNavHost,
 * not from feature-settings' own SettingsScreen.kt, so this validation scaffolding
 * never touches shared product-feature code.
 *
 * "Reopen Profile" deliberately re-navigates to the same [FactoryDestination.Profile]
 * destination, so a Home → Settings → Profile → Profile → Home walk fires the
 * automatic-tracking listener twice in a row for "profile" — the exact case
 * `InfinityForgeTrackingClient.screen()`'s existing consecutive-duplicate suppression
 * must drop.
 */
@Composable
fun ProfileTestScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Profile (validation-only)", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Not a product screen — exists only to exercise automatic screen tracking.",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = { navController.navigate(FactoryDestination.Profile) }) {
            Text("Reopen Profile (consecutive duplicate)")
        }
        Button(onClick = { navController.navigate(FactoryDestination.Home) { popUpTo(FactoryDestination.Home) { inclusive = true } } }) {
            Text("Back to Home")
        }
    }
}
