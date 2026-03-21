package com.budgetmanager.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.budgetmanager.app.ui.theme.LocalFinanceColors
import com.budgetmanager.app.ui.theme.Spacing

/**
 * Swipe-to-delete wrapper with smooth animation and semantic coloring.
 *
 * Uses [FinanceColors.swipeDelete] from [LocalFinanceColors] so the
 * background adapts correctly to light / dark / dynamic themes.
 *
 * @param onDelete  invoked when the user completes a right-to-left swipe
 * @param modifier  outer modifier
 * @param content   the composable content that can be swiped away
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            SwipeToDeleteBackground(dismissState.targetValue)
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        content = { content() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteBackground(
    targetValue: SwipeToDismissBoxValue = SwipeToDismissBoxValue.Settled,
) {
    val financeColors = LocalFinanceColors.current

    val backgroundColor by animateColorAsState(
        targetValue = when (targetValue) {
            SwipeToDismissBoxValue.EndToStart -> financeColors.swipeDelete
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "swipe_bg_color",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (targetValue == SwipeToDismissBoxValue.EndToStart) 1.2f else 0.8f,
        animationSpec = tween(durationMillis = 200),
        label = "swipe_icon_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .padding(horizontal = Spacing.xl),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = Color.White,
            modifier = Modifier.scale(iconScale),
        )
    }
}
