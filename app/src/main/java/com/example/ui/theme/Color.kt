package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Static definitions for color schemes reference
val CosmosPrimaryStatic = Color(0xFF6366F1)
val CosmosSecondaryStatic = Color(0xFF8B5CF6)
val CosmosTealStatic = Color(0xFF14B8A6)
val CosmosTextLightStatic = Color(0xFFF8FAFC)
val CosmosTextDarkStatic = Color(0xFF0F172A)

val ColorDarkBg = Color(0xFF0F172A)
val ColorLightBg = Color(0xFFF1F5F9)
val ColorDarkSurface = Color(0xFF1E293B)
val ColorLightSurface = Color(0xFFFFFFFF)
val ColorDarkBorder = Color(0xFF334155)
val ColorLightBorder = Color(0xFFE2E8F0)

// Dynamic Compose-friendly properties
val CosmosBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) ColorDarkBg else ColorLightBg

val CosmosSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) ColorDarkSurface else ColorLightSurface

val CosmosPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) CosmosPrimaryStatic else Color(0xFF4F46E5)

val CosmosSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) CosmosSecondaryStatic else Color(0xFF7C3AED)

val CosmosTeal: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) CosmosTealStatic else Color(0xFF0D9488)

val CosmosTextLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) CosmosTextLightStatic else CosmosTextDarkStatic

val CosmosTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) Color(0xFF94A3B8) else Color(0xFF64748B)

val CosmosCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) ColorDarkBorder else ColorLightBorder

val CosmosSuccess = Color(0xFF10B981)
val CosmosError = Color(0xFFEF4444)
val CosmosWarning = Color(0xFFF59E0B)
