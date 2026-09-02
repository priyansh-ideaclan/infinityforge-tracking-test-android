package com.factory.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Static fallback palette used below API 31 or when dynamic color is disabled. Sourced
 * from `APP_SPEC.yaml`'s `branding.*_color` fields by `scripts/configure_app.py` — the
 * three colors below are the factory's own template defaults (Material 3 "baseline
 * purple") until an app-specific spec is configured.
 */
val FactoryPrimary = Color(0xFF6750A4)
val FactorySecondary = Color(0xFF625B71)
val FactoryTertiary = Color(0xFF7D5260)

val FactoryPrimaryDark = Color(0xFFD0BCFF)
val FactorySecondaryDark = Color(0xFFCCC2DC)
val FactoryTertiaryDark = Color(0xFFEFB8C8)
