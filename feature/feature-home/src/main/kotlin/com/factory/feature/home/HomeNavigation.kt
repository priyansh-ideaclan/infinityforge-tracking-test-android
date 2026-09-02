package com.factory.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.factory.ads.api.BannerAdRenderer
import com.factory.core.navigation.FactoryDestination
import com.factory.feature.home.ui.HomeRoute

fun NavGraphBuilder.homeGraph(navController: NavHostController, bannerAdRenderer: BannerAdRenderer) {
    composable<FactoryDestination.Home> {
        HomeRoute(
            bannerAdRenderer = bannerAdRenderer,
            onOpenSettings = { navController.navigate(FactoryDestination.Settings) },
        )
    }
}
