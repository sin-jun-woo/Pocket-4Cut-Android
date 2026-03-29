package com.pocket4cut.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.ui.designsystem.*

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onGallery: () -> Unit,
    galleryCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .offset(y = (-100).dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AppColors.Accent.pinkSubtle, Color.Transparent),
                            radius = size.minDimension * 0.6f,
                        ),
                        radius = size.minDimension * 0.5f,
                    )
                },
        )

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.xxxl, start = AppSpacing.Screen.horizontal, end = AppSpacing.Screen.horizontal, bottom = AppSpacing.xl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(AppLayout.Radius.md))
                        .background(AppColors.Accent.pink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = AppColors.Text.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(AppSpacing.sm))
                Text(
                    text = "Pocket 4Cut",
                    style = AppTypography.title2.copy(letterSpacing = (-0.5).sp),
                    color = AppColors.Text.primary,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(216.dp)
                                .scale(pulseScale)
                                .border(2.dp, AppColors.Accent.pink.copy(alpha = pulseAlpha), CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .background(AppColors.Accent.pink)
                                .border(8.dp, AppColors.Background.secondary, CircleShape)
                                .clickable(onClick = onStart),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, null, tint = AppColors.Text.primary, modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(AppSpacing.sm))
                                Text("촬영 시작", style = AppTypography.headline, color = AppColors.Text.primary)
                            }
                        }
                    }
                    Spacer(Modifier.height(AppSpacing.xl))
                    Text("탭하여 나만의 인생네컷 만들기", style = AppTypography.callout, color = AppColors.Text.secondary)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = AppSpacing.Layout.ctaBottomSpace)
                    .clip(RoundedCornerShape(AppLayout.Radius.lg))
                    .background(AppColors.Background.tertiary)
                    .border(1.dp, AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.lg))
                    .clickable(onClick = onGallery)
                    .padding(AppSpacing.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(AppLayout.Radius.sm))
                                .background(AppColors.Overlay.white),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Image, null, tint = AppColors.Text.tertiary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(AppSpacing.sm))
                        Column {
                            Text("보관함", style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Text.primary)
                            Text("${galleryCount}개의 추억", style = AppTypography.caption1, color = AppColors.Text.secondary)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = AppColors.Text.tertiary)
                }
            }
        }
    }
}
