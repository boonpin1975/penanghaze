package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GeoOrange,
    onPrimary = PureWhite,
    primaryContainer = GeoOrangeLight,
    onPrimaryContainer = GeoOrangeDark,

    secondary = GeoGreen,
    onSecondary = PureWhite,
    secondaryContainer = GeoGreenLight,
    onSecondaryContainer = GeoGreen,

    tertiary = GeoBlue,
    onTertiary = PureWhite,
    tertiaryContainer = GeoBlueLight,
    onTertiaryContainer = GeoBlue,

    background = GeoBackground,
    onBackground = GeoTextPrimary,

    surface = GeoSurface,
    onSurface = GeoTextPrimary,
    surfaceVariant = GeoSurfaceCardMuted,
    onSurfaceVariant = GeoTextSecondary,

    outline = GeoBorder,
    outlineVariant = GeoBorderSubtle
)

private val DarkColorScheme = darkColorScheme(
    primary = GeoOrange,
    onPrimary = PureBlack,
    primaryContainer = Color(0xFF431407),
    onPrimaryContainer = GeoOrangeLight,

    secondary = GeoGreen,
    onSecondary = PureBlack,
    secondaryContainer = Color(0xFF052E16),
    onSecondaryContainer = GeoGreenLight,

    tertiary = GeoBlue,
    onTertiary = PureBlack,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = DarkBorder,
    outlineVariant = DarkSurfaceCardElevated
)

@Composable
fun PenangHazeTheme(
    darkTheme: Boolean = false, // Geometric Balance clean theme defaults to pristine light style
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PenangHazeTheme(darkTheme = darkTheme, content = content)
}
