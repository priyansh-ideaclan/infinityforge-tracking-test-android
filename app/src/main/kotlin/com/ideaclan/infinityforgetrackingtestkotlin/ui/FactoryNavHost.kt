package com.ideaclan.infinityforgetrackingtestkotlin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.factory.ads.api.BannerAdRenderer
import com.factory.core.navigation.FactoryDestination
import com.factory.core.navigation.FactoryNavigator
import com.factory.core.navigation.NavigationCommand
import com.factory.feature.auth.authGraph
import com.factory.feature.home.homeGraph
import com.factory.feature.onboarding.onboardingGraph
import com.factory.feature.settings.settingsGraph
import kotlinx.coroutines.flow.collectLatest

/**
 * The single top-level nav graph — the only place all four feature modules' graphs are
 * composed together, since `app` is the only module allowed to see every feature.
 */
@Composable
fun FactoryNavHost(
    navController: NavHostController,
    navigator: FactoryNavigator,
    startDestination: FactoryDestination,
    bannerAdRenderer: BannerAdRenderer,
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

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        authGraph()
        onboardingGraph()
        homeGraph(navController, bannerAdRenderer)
        settingsGraph()
    }
}
