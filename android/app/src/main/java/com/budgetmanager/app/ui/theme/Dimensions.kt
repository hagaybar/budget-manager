package com.budgetmanager.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design-system spacing tokens on a strict 4dp grid.
 *
 * Usage:
 *   Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
 *   Spacer(modifier = Modifier.height(Spacing.xl))
 */
object Spacing {
    /** 4dp — inline element gaps, icon-to-text */
    val xs = 4.dp
    /** 8dp — between related items (chip gaps, label-to-value) */
    val sm = 8.dp
    /** 12dp — form field gaps, card content sections */
    val md = 12.dp
    /** 16dp — screen padding, card internal padding, section gaps */
    val lg = 16.dp
    /** 24dp — between major sections */
    val xl = 24.dp
    /** 32dp — top/bottom screen padding, sign-in spacing */
    val xxl = 32.dp
    /** 48dp — feature section dividers */
    val xxxl = 48.dp
}

/**
 * Corner radius tokens for consistent rounding across components.
 *
 * Usage:
 *   RoundedCornerShape(CornerRadius.medium)       // standard cards
 *   RoundedCornerShape(CornerRadius.extraLarge)    // FABs, sign-in buttons
 */
object CornerRadius {
    /** 4dp — badges, small tags */
    val extraSmall = 4.dp
    /** 8dp — chips, small cards, input fields */
    val small = 8.dp
    /** 12dp — standard cards, dialogs */
    val medium = 12.dp
    /** 16dp — bottom sheets, large cards */
    val large = 16.dp
    /** 24dp — FABs, sign-in buttons */
    val extraLarge = 24.dp
}

/**
 * Elevation tokens for consistent shadow / tonal surface layering.
 *
 * In Material 3, elevation on dark mode surfaces manifests as a tonal
 * overlay rather than a drop shadow. These tokens work with both.
 */
object Elevation {
    /** 0dp — flat surfaces, backgrounds */
    val none = 0.dp
    /** 1dp — subtle card lift */
    val low = 1.dp
    /** 3dp — standard card / sheet elevation */
    val medium = 3.dp
    /** 6dp — dialogs, dropdown menus */
    val high = 6.dp
    /** 8dp — navigation drawers, modal bottom sheets */
    val extraHigh = 8.dp
}
