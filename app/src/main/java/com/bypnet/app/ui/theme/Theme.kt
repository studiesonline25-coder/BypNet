package com.bypnet.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BypNetDarkColorScheme = darkColorScheme(
    primary = Cyan400,
    onPrimary = DarkBackground,
    primaryContainer = Cyan700,
    onPrimaryContainer = Color.White,
    secondary = Teal400,
    onSecondary = DarkBackground,
    secondaryContainer = Teal500,
    onSecondaryContainer = Color.White,
    tertiary = GradientPurpleStart,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = StatusDisconnected,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = Cyan600,
    scrim = Color.Black
)

@Composable
fun BypNetTheme(content: @Composable () -> Unit) {
    val colorScheme = BypNetDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BypNetTypography,
        content = content
    )
}
