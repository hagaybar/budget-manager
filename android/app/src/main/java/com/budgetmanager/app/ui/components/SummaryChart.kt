package com.budgetmanager.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.domain.model.CategoryBreakdown
import com.budgetmanager.app.domain.model.TransactionType
import com.budgetmanager.app.ui.theme.ExpenseRed
import com.budgetmanager.app.ui.theme.ExpenseRedDark
import com.budgetmanager.app.ui.theme.IncomeGreen
import com.budgetmanager.app.ui.theme.IncomeGreenDark

@Composable
fun SummaryChart(
    breakdowns: List<CategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    val maxAmount = breakdowns.maxOfOrNull { it.total } ?: 1.0
    val isDark = isSystemInDarkTheme()

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(breakdowns) {
        animationTriggered = true
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        breakdowns.forEachIndexed { index, breakdown ->
            val targetProgress = (breakdown.total / maxAmount).toFloat()
            val animatedProgress by animateFloatAsState(
                targetValue = if (animationTriggered) targetProgress else 0f,
                animationSpec = tween(
                    durationMillis = 600,
                    delayMillis = index * 80
                ),
                label = "progress_$index"
            )
            val barColor = when {
                breakdown.type == TransactionType.INCOME && isDark -> IncomeGreenDark
                breakdown.type == TransactionType.INCOME -> IncomeGreen
                isDark -> ExpenseRedDark
                else -> ExpenseRed
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = breakdown.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    AmountText(
                        amount = breakdown.total,
                        type = breakdown.type,
                        style = MaterialTheme.typography.labelLarge,
                        showSign = false
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
