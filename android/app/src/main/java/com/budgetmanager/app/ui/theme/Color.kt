package com.budgetmanager.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Deep Trust Blue — Primary Tonal Palette
// Base: #2B5EA7 — confident, trustworthy, distinctive
// ============================================================================
val md_theme_light_primary = Color(0xFF2B5EA7)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFD4E3FF)
val md_theme_light_onPrimaryContainer = Color(0xFF001B3E)

val md_theme_dark_primary = Color(0xFFA5C8FF)
val md_theme_dark_onPrimary = Color(0xFF00315E)
val md_theme_dark_primaryContainer = Color(0xFF0E4483)
val md_theme_dark_onPrimaryContainer = Color(0xFFD4E3FF)

// ============================================================================
// Cool Slate — Secondary Tonal Palette
// Base: #526070 — understated, professional neutral-blue
// ============================================================================
val md_theme_light_secondary = Color(0xFF526070)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD6E4F0)
val md_theme_light_onSecondaryContainer = Color(0xFF0F1D2A)

val md_theme_dark_secondary = Color(0xFFBAC8D8)
val md_theme_dark_onSecondary = Color(0xFF243240)
val md_theme_dark_secondaryContainer = Color(0xFF3A4857)
val md_theme_dark_onSecondaryContainer = Color(0xFFD6E4F0)

// ============================================================================
// Muted Teal — Tertiary Tonal Palette
// Base: #4A7A6F — calming accent, nature-inspired trust signal
// ============================================================================
val md_theme_light_tertiary = Color(0xFF4A7A6F)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFCCEDE5)
val md_theme_light_onTertiaryContainer = Color(0xFF05201A)

val md_theme_dark_tertiary = Color(0xFFB0D1C9)
val md_theme_dark_onTertiary = Color(0xFF1C352F)
val md_theme_dark_tertiaryContainer = Color(0xFF334B45)
val md_theme_dark_onTertiaryContainer = Color(0xFFCCEDE5)

// ============================================================================
// Error — Refined Red
// Base: #BA1A1A — standard M3 error, restrained and deliberate
// ============================================================================
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

// ============================================================================
// Neutral — Warm-Cool Whites & Grays (NO purple tint)
// ============================================================================
val md_theme_light_background = Color(0xFFFAFCFF)   // Ice White — very slight blue warmth
val md_theme_light_onBackground = Color(0xFF1A1C1E)
val md_theme_light_surface = Color(0xFFFAFCFF)
val md_theme_light_onSurface = Color(0xFF1A1C1E)
val md_theme_light_surfaceVariant = Color(0xFFE0E3E8) // Cool grey — no purple
val md_theme_light_onSurfaceVariant = Color(0xFF43474E)
val md_theme_light_outline = Color(0xFF73777F)
val md_theme_light_outlineVariant = Color(0xFFC3C6CF)
val md_theme_light_inverseSurface = Color(0xFF2F3033)
val md_theme_light_inverseOnSurface = Color(0xFFF1F0F4)
val md_theme_light_inversePrimary = Color(0xFFA5C8FF)
val md_theme_light_scrim = Color(0xFF000000)
val md_theme_light_surfaceTint = Color(0xFF2B5EA7)

val md_theme_dark_background = Color(0xFF111418)     // Charcoal — deep, restful
val md_theme_dark_onBackground = Color(0xFFE2E2E6)
val md_theme_dark_surface = Color(0xFF1A1C20)        // Dark Slate — card surfaces
val md_theme_dark_onSurface = Color(0xFFE2E2E6)
val md_theme_dark_surfaceVariant = Color(0xFF42474E)  // Medium Slate
val md_theme_dark_onSurfaceVariant = Color(0xFFC3C6CF)
val md_theme_dark_outline = Color(0xFF8D9199)
val md_theme_dark_outlineVariant = Color(0xFF43474E)
val md_theme_dark_inverseSurface = Color(0xFFE2E2E6)
val md_theme_dark_inverseOnSurface = Color(0xFF2F3033)
val md_theme_dark_inversePrimary = Color(0xFF2B5EA7)
val md_theme_dark_scrim = Color(0xFF000000)
val md_theme_dark_surfaceTint = Color(0xFFA5C8FF)

// ============================================================================
// Tonal surface layers for dark mode elevation
// These provide subtle distinction between elevated surfaces
// ============================================================================
val md_theme_dark_surfaceContainer = Color(0xFF1E2024)
val md_theme_dark_surfaceContainerHigh = Color(0xFF282A2E)
val md_theme_dark_surfaceContainerHighest = Color(0xFF333539)
val md_theme_dark_surfaceContainerLow = Color(0xFF1A1C20)
val md_theme_dark_surfaceContainerLowest = Color(0xFF0D0F13)
val md_theme_dark_surfaceBright = Color(0xFF383A3E)
val md_theme_dark_surfaceDim = Color(0xFF111418)

val md_theme_light_surfaceContainer = Color(0xFFEEF0F4)
val md_theme_light_surfaceContainerHigh = Color(0xFFE8EAEE)
val md_theme_light_surfaceContainerHighest = Color(0xFFE2E4E8)
val md_theme_light_surfaceContainerLow = Color(0xFFF4F6FA)
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceBright = Color(0xFFFAFCFF)
val md_theme_light_surfaceDim = Color(0xFFDADCE0)

// ============================================================================
// Semantic Finance Colors — purpose-driven, NOT wired through M3 secondary
// These are delivered via CompositionLocal in Theme.kt
// ============================================================================

// Income / Positive Balance
val IncomeGreen = Color(0xFF2E7D32)          // Rich forest green — growth
val IncomeGreenDark = Color(0xFF6ECF81)      // Vivid bright green on dark surfaces

// Expense / Negative Balance
val ExpenseRed = Color(0xFFC62828)           // Warm authoritative red (not error red)
val ExpenseRedDark = Color(0xFFFFB4A8)       // Soft coral — readable on dark, not washed out

// Warning / Budget limits approaching
val WarningAmber = Color(0xFFF9A825)         // Visible amber — caution without alarm
val WarningAmberDark = Color(0xFFFFD54F)     // Bright warm amber for dark mode

// Balance aliases — differentiated from income/expense for distinct UI usage
val BalancePositive = Color(0xFF1B6E2D)
val BalancePositiveDark = Color(0xFF81D88D)
val BalanceNegative = Color(0xFFB71C1C)
val BalanceNegativeDark = Color(0xFFFF897D)

// Swipe delete — subtly different from expense red to avoid confusion
val SwipeDeleteRed = Color(0xFFD32F2F)
val SwipeDeleteRedDark = Color(0xFFEF5350)

// ============================================================================
// Chart / Category Colors — 10 distinguishable hues, WCAG AA on white/dark
// ============================================================================
val ChartColors = listOf(
    Color(0xFF2B5EA7),  // Deep Trust Blue (primary)
    Color(0xFF2E7D32),  // Forest Green
    Color(0xFFE65100),  // Deep Orange
    Color(0xFF6A1B9A),  // Purple
    Color(0xFFC62828),  // Red
    Color(0xFF4A7A6F),  // Muted Teal (tertiary)
    Color(0xFF0277BD),  // Light Blue
    Color(0xFF4E342E),  // Brown
    Color(0xFF37474F),  // Blue Grey
    Color(0xFFF9A825),  // Amber
)
