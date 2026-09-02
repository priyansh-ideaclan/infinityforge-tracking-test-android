package com.factory.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.factory.core.navigation.FactoryDestination
import com.factory.feature.onboarding.ui.OnboardingRoute

fun NavGraphBuilder.onboardingGraph() {
    composable<FactoryDestination.Onboarding> { OnboardingRoute() }
}
