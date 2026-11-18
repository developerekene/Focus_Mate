package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.SessionType
import com.example.model.ThemeMode

private val FocusDarkColorScheme = darkColorScheme(
    primary = FocusCoralDark,
    onPrimary = Color(0xFF141110),
    primaryContainer = FocusCoralContainerDark,
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = ShortBreakSageDark,
    onSecondary = Color(0xFF0A2018),
    secondaryContainer = ShortBreakContainerDark,
    onSecondaryContainer = Color(0xFFC2F5E1),
    tertiary = LongBreakIndigoDark,
    onTertiary = Color(0xFF0B1836),
    tertiaryContainer = LongBreakContainerDark,
    onTertiaryContainer = Color(0xFFD6E2FF),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant
)

private val FocusLightColorScheme = lightColorScheme(
    primary = FocusCoralLight,
    onPrimary = Color.White,
    primaryContainer = FocusCoralContainerLight,
    onPrimaryContainer = Color(0xFF4A1207),
    secondary = ShortBreakSageLight,
    onSecondary = Color.White,
    secondaryContainer = ShortBreakContainerLight,
    onSecondaryContainer = Color(0xFF0A3828),
    tertiary = LongBreakIndigoLight,
    onTertiary = Color.White,
    tertiaryContainer = LongBreakContainerLight,
    onTertiaryContainer = Color(0xFF102459),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    outline = LightOutline,
    outlineVariant = LightSurfaceVariant
)

val FocusMateShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun phaseAccentColor(sessionType: SessionType, isDark: Boolean): Color {
    return when (sessionType) {
        SessionType.FOCUS -> if (isDark) FocusCoralDark else FocusCoralLight
        SessionType.SHORT_BREAK -> if (isDark) ShortBreakSageDark else ShortBreakSageLight
        SessionType.LONG_BREAK -> if (isDark) LongBreakIndigoDark else LongBreakIndigoLight
    }
}

@Composable
fun phaseContainerColor(sessionType: SessionType, isDark: Boolean): Color {
    return when (sessionType) {
        SessionType.FOCUS -> if (isDark) FocusCoralContainerDark else FocusCoralContainerLight
        SessionType.SHORT_BREAK -> if (isDark) ShortBreakContainerDark else ShortBreakContainerLight
        SessionType.LONG_BREAK -> if (isDark) LongBreakContainerDark else LongBreakContainerLight
    }
}

@Composable
fun FocusMateTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) FocusDarkColorScheme else FocusLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FocusMateTypography,
        shapes = FocusMateShapes,
        content = content
    )
}
