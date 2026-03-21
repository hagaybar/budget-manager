package com.budgetmanager.app.ui.screens.signin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.R
import com.budgetmanager.app.auth.AuthState
import com.budgetmanager.app.ui.theme.CornerRadius
import com.budgetmanager.app.ui.theme.Spacing
import com.budgetmanager.app.ui.viewmodel.AuthViewModel

@Composable
fun SignInScreen(
    onSignInComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val signInError by viewModel.signInError.collectAsState()
    val isSigningIn by viewModel.isSigningIn.collectAsState()
    val context = LocalContext.current

    val colorScheme = MaterialTheme.colorScheme

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "fadeIn"
    )

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) {
            onSignInComplete()
        }
    }

    // Use primaryContainer as gradient anchor — it's dark in dark mode and light
    // in light mode, so onPrimaryContainer always provides good contrast.
    val gradientForeground = colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.primaryContainer,
                        colorScheme.surface,
                    )
                )
            )
    ) {
        // Decorative circles using gradient foreground color
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = gradientForeground.copy(alpha = 0.06f),
                radius = 200f,
                center = Offset(size.width * 0.8f, size.height * 0.15f)
            )
            drawCircle(
                color = gradientForeground.copy(alpha = 0.09f),
                radius = 150f,
                center = Offset(size.width * 0.15f, size.height * 0.3f)
            )
            drawCircle(
                color = gradientForeground.copy(alpha = 0.05f),
                radius = 300f,
                center = Offset(size.width * 0.5f, size.height * 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.xxl, vertical = Spacing.xxl)
                .alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            // App icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        gradientForeground.copy(alpha = 0.15f),
                        RoundedCornerShape(CornerRadius.extraLarge),
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = gradientForeground,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // App title
            Text(
                text = stringResource(R.string.signin_app_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = gradientForeground,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Subtitle
            Text(
                text = stringResource(R.string.signin_app_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = gradientForeground.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Currency badge
            Text(
                text = stringResource(R.string.signin_currency_badge),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = gradientForeground.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.weight(0.3f))

            // Error message
            AnimatedVisibility(
                visible = signInError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                signInError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                colorScheme.errorContainer.copy(alpha = 0.9f),
                                RoundedCornerShape(CornerRadius.small),
                            )
                            .padding(Spacing.md),
                    )
                }
            }

            if (signInError != null) {
                Spacer(modifier = Modifier.height(Spacing.lg))
            }

            // Buttons
            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator(color = gradientForeground)
                }
                else -> {
                    // Google Sign-In button
                    Button(
                        onClick = {
                            viewModel.clearError()
                            viewModel.signInWithGoogle(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gradientForeground,
                            contentColor = colorScheme.primaryContainer,
                        ),
                        shape = RoundedCornerShape(CornerRadius.medium),
                        enabled = !isSigningIn,
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = colorScheme.primaryContainer,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                stringResource(R.string.signin_google_button),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))

                    // Guest button
                    OutlinedButton(
                        onClick = { viewModel.continueAsGuest() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = gradientForeground,
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = gradientForeground.copy(alpha = 0.5f),
                        ),
                        shape = RoundedCornerShape(CornerRadius.medium),
                        enabled = !isSigningIn,
                    ) {
                        Text(
                            stringResource(R.string.signin_guest_button),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}
