package com.viplove.licadvisornative.ui.theme

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
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = BrandNavyContainer,
    onPrimaryContainer = BrandNavy,
    secondary = BrandGold,
    onSecondary = Color(0xFF3B2A00),
    secondaryContainer = BrandGoldSoft,
    onSecondaryContainer = Color(0xFF6B4F00),
    tertiary = BrandGold,
    onTertiary = Color(0xFF3B2A00),
    background = LightBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = LightBackground,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary,
    outlineVariant = DividerSubtle,
    error = BrandDanger,
    onError = Color.White,
    errorContainer = BrandDangerBg,
    onErrorContainer = BrandDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandNavyContainer,
    onPrimary = BrandNavyDeep,
    primaryContainer = BrandNavy,
    onPrimaryContainer = Color.White,
    secondary = BrandGold,
    onSecondary = Color(0xFF3B2A00),
    secondaryContainer = Color(0xFF6B4F00),
    onSecondaryContainer = BrandGoldSoft,
    tertiary = BrandGold,
    onTertiary = Color(0xFF3B2A00),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF1B2438),
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkTextSecondary,
    outlineVariant = DarkDivider,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun LICAdvisorNativeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BrandNavy.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
