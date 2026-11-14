package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Clean Academic White & Blue Palette
val AcademicBluePrimaryStatic = Color(0xFF1D4ED8)
val AcademicBlueSecondaryStatic = Color(0xFF2563EB)
val AcademicBlueAccentStatic = Color(0xFF0F4C81)
val AcademicBlueSoftLight = Color(0xFFE8F0FE)
val AcademicBlueSoftDark = Color(0xFF1E3A5F)

val AcademicDarkPrimaryStatic = Color(0xFF60A5FA)
val AcademicDarkSecondaryStatic = Color(0xFF93C5FD)

val ColorLightBg = Color(0xFFF8FAFC)
val ColorLightSurface = Color(0xFFFFFFFF)
val ColorLightSurfaceVariant = Color(0xFFF1F5F9)
val ColorLightBorder = Color(0xFFE2E8F0)
val ColorLightTextPrimary = Color(0xFF0F172A)
val ColorLightTextMuted = Color(0xFF475569)

val ColorDarkBg = Color(0xFF111827)
val ColorDarkSurface = Color(0xFF1F2937)
val ColorDarkSurfaceVariant = Color(0xFF263142)
val ColorDarkBorder = Color(0xFF334155)
val ColorDarkTextPrimary = Color(0xFFF8FAFC)
val ColorDarkTextMuted = Color(0xFF94A3B8)

// Semantic adaptive tokens
val AcademicBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) ColorDarkBg else ColorLightBg

val AcademicSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) ColorDarkSurface else ColorLightSurface

val AcademicSurfaceSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) ColorDarkSurfaceVariant else ColorLightSurfaceVariant

val AcademicBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) AcademicDarkPrimaryStatic else AcademicBluePrimaryStatic

val AcademicBlueMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) AcademicDarkSecondaryStatic else AcademicBlueSecondaryStatic

val AcademicBlueTint: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) AcademicBlueSoftDark else AcademicBlueSoftLight

val AcademicTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) ColorDarkTextPrimary else ColorLightTextPrimary

val AcademicTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) ColorDarkTextMuted else ColorLightTextMuted

val AcademicBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) ColorDarkBorder else ColorLightBorder

val AcademicSuccess: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) Color(0xFF34D399) else Color(0xFF15803D)

val AcademicError: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) Color(0xFFF87171) else Color(0xFFDC2626)

val AcademicWarning: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeIsDark.current) Color(0xFFFBBF24) else Color(0xFFB45309)

// Backward-compatible semantic aliases pointing to Academic tokens
val CosmosBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicBackground

val CosmosSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicSurface

val CosmosPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicBlue

val CosmosSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicBlueMuted

val CosmosTeal: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicBlue

val CosmosTextLight: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicTextPrimary

val CosmosTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicTextMuted

val CosmosCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicBorder

val CosmosSuccess: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicSuccess

val CosmosError: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicError

val CosmosWarning: Color
    @Composable
    @ReadOnlyComposable
    get() = AcademicWarning
