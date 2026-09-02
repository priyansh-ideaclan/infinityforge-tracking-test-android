package com.factory.feature.auth

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.factory.core.navigation.FactoryDestination
import com.factory.feature.auth.ui.ForgotPasswordRoute
import com.factory.feature.auth.ui.LoginRoute
import com.factory.feature.auth.ui.RegisterRoute

/**
 * Registers the auth screens on the app's single top-level `NavHost`. Only reachable
 * when `auth.enabled` — see `app`'s `FactoryNavHost`.
 */
fun NavGraphBuilder.authGraph() {
    composable<FactoryDestination.Login> { LoginRoute() }
    composable<FactoryDestination.Register> { RegisterRoute() }
    composable<FactoryDestination.ForgotPassword> { ForgotPasswordRoute() }
}
