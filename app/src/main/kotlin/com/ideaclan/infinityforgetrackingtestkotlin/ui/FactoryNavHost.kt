package com.ideaclan.infinityforgetrackingtestkotlin.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.factory.ads.api.BannerAdRenderer
import com.factory.core.logging.Logger
import com.factory.core.navigation.FactoryDestination
import com.factory.core.navigation.FactoryNavigator
import com.factory.core.navigation.NavigationCommand
import com.factory.core.navigation.infinityForgeScreenName
import com.factory.core.tracking.InfinityForgeTrackingClient
import com.factory.feature.auth.authGraph
import com.factory.feature.home.homeGraph
import com.factory.feature.onboarding.onboardingGraph
import com.factory.feature.settings.ui.SettingsRoute
import kotlinx.coroutines.flow.collectLatest

private const val TRACKING_TAG = "InfinityForgeTracking"

/**
 * The single top-level nav graph — the only place all four feature modules' graphs are
 * composed together, since `app` is the only module allowed to see every feature.
 *
 * Also the single integration point for InfinityForge automatic screen tracking
 * (Docs/INFINITYFORGE_TRACKING.md): every destination change flows through
 * [NavController]'s own [NavController.OnDestinationChangedListener] — Navigation
 * Compose's real, central "the current destination changed" hook — resolves its
 * stable logical name via [infinityForgeScreenName], then calls
 * [InfinityForgeTrackingClient.screen], which already owns consecutive-duplicate
 * suppression and `previous_screen` bookkeeping (core-tracking's
 * `InfinityForgeTrackingClient.screen()`); this listener does not reimplement either.
 *
 * This test app inlines the Settings destination (rather than calling the factory's
 * `settingsGraph()`) purely to add an "Open Profile (test)" affordance and the
 * validation-only `FactoryDestination.Profile` destination — see
 * `ProfileTestScreen.kt` and `FactoryDestination.kt`'s `Profile` doc comment. Nothing
 * about the tracking integration itself lives here; this file's tracking-relevant code
 * (the DisposableEffect below) is otherwise identical to the factory's.
 */
@Composable
fun FactoryNavHost(
    navController: NavHostController,
    navigator: FactoryNavigator,
    startDestination: FactoryDestination,
    bannerAdRenderer: BannerAdRenderer,
    trackingClient: InfinityForgeTrackingClient,
    logger: Logger,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(navigator) {
        navigator.commands.collectLatest { command ->
            when (command) {
                is NavigationCommand.NavigateTo -> navController.navigate(command.destination) {
                    if (command.popUpToStart) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                is NavigationCommand.NavigateBack -> navController.popBackStack()
            }
        }
    }

    DisposableEffect(navController, trackingClient) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            // A tracking failure must never block or crash navigation (this task's
            // explicit requirement) — screen()/infinityForgeScreenName() are not
            // expected to throw (validation fails closed and provider dispatch is
            // isolated per-provider, see InfinityForgeDispatcher), but this listener
            // runs inline on Navigation Compose's own callback, so it is caught
            // defensively anyway rather than relying on that invariant holding forever.
            runCatching {
                destination.infinityForgeScreenName()?.let { screenName ->
                    trackingClient.screen(screenName)
                }
            }.onFailure { error ->
                logger.warn(TRACKING_TAG, "Automatic screen tracking failed for a navigation event", error)
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        authGraph()
        onboardingGraph()
        homeGraph(navController, bannerAdRenderer)
        composable<FactoryDestination.Settings> {
            Column {
                SettingsRoute()
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "InfinityForge Tracking validation — not a product affordance",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Button(
                    onClick = { navController.navigate(FactoryDestination.Profile) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text("Open Profile (test)") }
            }
        }
        composable<FactoryDestination.Profile> { ProfileTestScreen(navController) }
    }
}
