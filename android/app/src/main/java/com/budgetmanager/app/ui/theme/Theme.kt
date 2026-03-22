package com.budgetmanager.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================================
// Light Color Scheme — "Deep Trust Blue" palette
// ============================================================================
private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,

    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,

    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,

    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,

    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,

    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    scrim = md_theme_light_scrim,
    surfaceTint = md_theme_light_surfaceTint,

    surfaceContainer = md_theme_light_surfaceContainer,
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_light_surfaceContainerHighest,
    surfaceContainerLow = md_theme_light_surfaceContainerLow,
    surfaceContainerLowest = md_theme_light_surfaceContainerLowest,
    surfaceBright = md_theme_light_surfaceBright,
    surfaceDim = md_theme_light_surfaceDim,
)

// ============================================================================
// Dark Color Scheme — "Deep Trust Blue" dark variant
// ============================================================================
private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,

    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,

    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,

    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,

    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,

    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    scrim = md_theme_dark_scrim,
    surfaceTint = md_theme_dark_surfaceTint,

    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest,
    surfaceContainerLow = md_theme_dark_surfaceContainerLow,
    surfaceContainerLowest = md_theme_dark_surfaceContainerLowest,
    surfaceBright = md_theme_dark_surfaceBright,
    surfaceDim = md_theme_dark_surfaceDim,
)

// ============================================================================
// Shapes — design-system corner radius tokens wired into MaterialTheme
// ============================================================================
val BudgetShapes = Shapes(
    extraSmall = RoundedCornerShape(CornerRadius.extraSmall),   // 4dp — badges
    small = RoundedCornerShape(CornerRadius.small),             // 8dp — chips, inputs
    medium = RoundedCornerShape(CornerRadius.medium),           // 12dp — cards, dialogs
    large = RoundedCornerShape(CornerRadius.large),             // 16dp — bottom sheets
    extraLarge = RoundedCornerShape(CornerRadius.extraLarge),   // 24dp — FABs
)

// ============================================================================
// Semantic Finance Colors — provided via CompositionLocal
//
// Components read these via BudgetFinanceColors.current instead of
// calling isSystemInDarkTheme() and picking hardcoded colors.
// This correctly adapts to light/dark/dynamic themes.
// ============================================================================

/**
 * Holds the semantic color values used for financial data display.
 *
 * These are intentionally separate from the M3 color scheme because
 * income/expense are domain concepts, not UI-role concepts.
 */
@Immutable
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val balancePositive: Color,
    val balanceNegative: Color,
    val swipeDelete: Color,
    val glassBackground: Color,
    val glassBorder: Color,
)

/** Light mode finance colors. */
val LightFinanceColors = FinanceColors(
    income = IncomeGreen,
    expense = ExpenseRed,
    warning = WarningAmber,
    balancePositive = BalancePositive,
    balanceNegative = BalanceNegative,
    swipeDelete = SwipeDeleteRed,
    glassBackground = GlassLightBackground,
    glassBorder = GlassLightBorder,
)

/** Dark mode finance colors. */
val DarkFinanceColors = FinanceColors(
    income = IncomeGreenDark,
    expense = ExpenseRedDark,
    warning = WarningAmberDark,
    balancePositive = BalancePositiveDark,
    balanceNegative = BalanceNegativeDark,
    swipeDelete = SwipeDeleteRedDark,
    glassBackground = GlassDarkBackground,
    glassBorder = GlassDarkBorder,
)

/**
 * CompositionLocal for finance-specific semantic colors.
 *
 * Access in any composable via:
 *   val financeColors = LocalFinanceColors.current
 *   Text(color = financeColors.income, ...)
 */
val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

// ============================================================================
// Theme composable
// ============================================================================

@Composable
fun BudgetManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // --- Color scheme selection ---
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // --- Finance semantic colors adapt to theme mode ---
    val financeColors = if (darkTheme) DarkFinanceColors else LightFinanceColors

    // --- Status bar ---
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // --- Provide theme values ---
    CompositionLocalProvider(
        LocalFinanceColors provides financeColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BudgetTypography,
            shapes = BudgetShapes,
            content = content,
        )
    }
}
