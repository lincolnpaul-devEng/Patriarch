package com.campus.network.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Patriarch Design System Theme
 * Built to keep app typography and theme colors constant across every screen.
 * Rooted in the visual identity established on Splash, Auth, and Onboarding.
 */

private val DarkColorScheme = darkColorScheme(
    primary = BrandBluePrimary,
    onPrimary = Color.White,
    primaryContainer = BrandBlueMid,
    onPrimaryContainer = Color.White,
    secondary = BrandBlueMid,
    onSecondary = Color.White,
    tertiary = BrandGold,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = BorderStrong,
    error = AlertRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBluePrimary,
    onPrimary = Color.White,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBluePrimary,
    secondary = BrandBlueMid,
    onSecondary = Color.White,
    tertiary = BrandGold,
    onTertiary = Color.Black,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = BorderSubtle,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun CampusNetworkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = PatriarchTypography,
        content = content
    )
}
