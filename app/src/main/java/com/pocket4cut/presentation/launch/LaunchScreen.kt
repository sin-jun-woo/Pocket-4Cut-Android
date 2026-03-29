package com.pocket4cut.presentation.launch

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.ui.designsystem.*
import kotlinx.coroutines.delay

@Composable
fun LaunchScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var spinnerVisible by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "logoScale",
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "logoAlpha",
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "textAlpha",
    )
    val spinnerAlpha by animateFloatAsState(
        targetValue = if (spinnerVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "spinnerAlpha",
    )

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(300)
        textVisible = true
        delay(500)
        spinnerVisible = true
        delay(700)
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AppColors.Background.primary, AppColors.Background.secondary),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
            ) {
                Box(
                    modifier = Modifier
                        .scale(logoScale)
                        .graphicsLayer { alpha = logoAlpha }
                        .shadow(
                            elevation = 24.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = AppColors.Shadow.glow,
                            spotColor = AppColors.Shadow.glow,
                        )
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(AppColors.Gradient.candy),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    modifier = Modifier.graphicsLayer { alpha = textAlpha },
                ) {
                    Text(
                        text = "Pocket 4Cut",
                        style = AppTypography.title1.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                        ),
                        color = AppColors.Text.accent,
                    )
                    Text(
                        text = "나만의 인생네컷",
                        style = AppTypography.callout.copy(letterSpacing = 0.5.sp),
                        color = AppColors.Text.secondary,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            CircularProgressIndicator(
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .size(24.dp)
                    .graphicsLayer { alpha = spinnerAlpha },
                color = AppColors.Accent.pink,
                strokeWidth = 2.dp,
            )
        }
    }
}
