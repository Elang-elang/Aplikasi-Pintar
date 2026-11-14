package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalThemeIsDark = compositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = AcademicDarkPrimaryStatic,
    secondary = AcademicDarkSecondaryStatic,
    tertiary = AcademicDarkPrimaryStatic,
    primaryContainer = AcademicBlueSoftDark,
    onPrimaryContainer = ColorDarkTextPrimary,
    background = ColorDarkBg,
    surface = ColorDarkSurface,
    surfaceVariant = ColorDarkSurfaceVariant,
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onTertiary = Color(0xFF0F172A),
    onBackground = ColorDarkTextPrimary,
    onSurface = ColorDarkTextPrimary,
    onSurfaceVariant = ColorDarkTextMuted,
    outline = ColorDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AcademicBluePrimaryStatic,
    secondary = AcademicBlueSecondaryStatic,
    tertiary = AcademicBlueAccentStatic,
    primaryContainer = AcademicBlueSoftLight,
    onPrimaryContainer = Color(0xFF1E3A8A),
    background = ColorLightBg,
    surface = ColorLightSurface,
    surfaceVariant = ColorLightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = ColorLightTextPrimary,
    onSurface = ColorLightTextPrimary,
    onSurfaceVariant = ColorLightTextMuted,
    outline = ColorLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
