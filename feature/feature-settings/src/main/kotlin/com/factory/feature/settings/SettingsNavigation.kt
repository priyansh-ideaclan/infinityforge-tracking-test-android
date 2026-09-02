package com.factory.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.factory.core.navigation.FactoryDestination
import com.factory.feature.settings.ui.SettingsRoute

fun NavGraphBuilder.settingsGraph() {
    composable<FactoryDestination.Settings> { SettingsRoute() }
}
