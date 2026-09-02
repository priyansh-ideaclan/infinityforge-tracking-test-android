package com.factory.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = FactoryPrimary,
    secondary = FactorySecondary,
    tertiary = FactoryTertiary,
)

private val DarkColors = darkColorScheme(
    primary = FactoryPrimaryDark,
    secondary = FactorySecondaryDark,
    tertiary = FactoryTertiaryDark,
)

/**
 * Single theming entry point for the whole app. [themeMode] resolves against the
 * system setting when [ThemeMode.SYSTEM]; [dynamicColor] is only honored on API 31+
 * (Material You) and silently falls back to the static [FactoryPrimary] palette below
 * that, so callers never need an SDK-version check of their own.
 */
@Composable
fun FactoryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FactoryTypography,
        content = content,
    )
}
