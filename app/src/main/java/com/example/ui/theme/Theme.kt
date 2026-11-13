package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalThemeIsDark = staticCompositionLocalOf { true }

private val DarkColorScheme =
  darkColorScheme(
    primary = CosmosPrimaryStatic,
    secondary = CosmosSecondaryStatic,
    tertiary = CosmosTealStatic,
    background = ColorDarkBg,
    surface = ColorDarkSurface,
    onPrimary = CosmosTextLightStatic,
    onSecondary = CosmosTextLightStatic,
    onTertiary = CosmosTextLightStatic,
    onBackground = CosmosTextLightStatic,
    onSurface = CosmosTextLightStatic,
    outline = ColorDarkBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CosmosPrimaryStatic,
    secondary = CosmosSecondaryStatic,
    tertiary = CosmosTealStatic,
    background = ColorLightBg,
    surface = ColorLightSurface,
    onPrimary = CosmosTextDarkStatic,
    onSecondary = CosmosTextDarkStatic,
    onTertiary = CosmosTextDarkStatic,
    onBackground = CosmosTextDarkStatic,
    onSurface = CosmosTextDarkStatic,
    outline = ColorLightBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // We want classic dark theme by default
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
