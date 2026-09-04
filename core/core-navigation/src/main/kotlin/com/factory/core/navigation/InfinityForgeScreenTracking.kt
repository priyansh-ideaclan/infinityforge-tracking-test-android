package com.factory.core.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

/**
 * Resolves a navigated-to [NavDestination]'s stable, non-PII logical screen
 * name for InfinityForge automatic screen tracking (Docs/INFINITYFORGE_TRACKING.md,
 * infinityforge-tracking-module specification/screen-tracking.md).
 *
 * Derived generically from the matching [FactoryDestination] sealed subclass's own
 * simple name (e.g. `Home` -> "home", `ForgotPassword` -> "forgot_password") rather
 * than a manually maintained per-screen map, so every current *and future*
 * [FactoryDestination] — including one a generated app or test app adds locally — is
 * covered automatically the moment it's added to the sealed interface, with no second
 * place to remember to update. Route class names are app-chosen identifiers, never
 * user data, so this is safe by construction — there is no PII to leak.
 *
 * Returns null for a destination this app's [FactoryDestination] graph doesn't
 * recognize (there is currently no such case, since every composable in
 * `FactoryNavHost` is keyed by a `FactoryDestination` subclass, but this keeps the
 * resolver total rather than throwing if that ever changes) — the caller must treat
 * null as "do not call `screen()` for this destination", not as an error.
 */
fun NavDestination.infinityForgeScreenName(): String? =
    FactoryDestination::class.sealedSubclasses
        .firstOrNull { hasRoute(it) }
        ?.simpleName
        ?.toScreenTrackingSnakeCase()

private fun String.toScreenTrackingSnakeCase(): String =
    replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
