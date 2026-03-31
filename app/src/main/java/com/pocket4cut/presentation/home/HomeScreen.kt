package com.pocket4cut.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onGallery: () -> Unit,
    onSettings: () -> Unit = {},
    galleryCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val floatAnim = rememberInfiniteTransition(label = "float")
    val cameraScale by floatAnim.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "cameraScale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AppColors.Background.primary, AppColors.Background.secondary),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Settings button (top right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppSpacing.xxxl,
                        start = AppSpacing.Screen.horizontal,
                        end = AppSpacing.Screen.horizontal,
                    ),
                horizontalArrangement = Arrangement.End,
            ) {
                IconCircleButton(onClick = onSettings, variant = IconButtonVariant.DEFAULT) {
                    Icon(Icons.Default.Settings, null, tint = AppColors.Text.secondary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(AppSpacing.md))

            // Camera icon + title
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .scale(cameraScale)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = AppColors.Shadow.color,
                            spotColor = AppColors.Shadow.color,
                        )
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AppColors.Gradient.candy),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }

                Text(
                    text = "Pocket 4Cut",
                    style = AppTypography.title1.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                    ),
                    color = AppColors.Text.accent,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    Text(
                        "나만의 인생네컷 만들기",
                        style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.Text.secondary,
                    )
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = AppColors.Accent.pink,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.xl))

            // Two card buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                CardButton(
                    title = "촬영하기",
                    subtitle = "시작하기",
                    icon = Icons.Default.CameraAlt,
                    gradient = AppColors.Gradient.peachy,
                    iconColor = AppColors.Accent.pink,
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                )
                CardButton(
                    title = "보관함",
                    subtitle = "추억 모음",
                    icon = Icons.Default.PhotoLibrary,
                    gradient = AppColors.Gradient.dreamy,
                    iconColor = AppColors.Accent.sky,
                    onClick = onGallery,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // Tip card
            val tipText = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AppColors.Text.secondary)) {
                    append("친구들과 함께 찍는 ")
                }
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = AppColors.Accent.pink)) {
                    append("특별한 순간")
                }
            }
            val tipShape = RoundedCornerShape(AppLayout.Radius.lg)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .clip(tipShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AppColors.Accent.yellow.copy(alpha = 0.5f),
                                AppColors.Background.tertiary.copy(alpha = 0.5f),
                            ),
                        ),
                    )
                    .border(AppLayout.BorderWidth.thin, AppColors.Border.light, tipShape)
                    .padding(AppSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(tipText, style = AppTypography.callout)
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CardButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppLayout.Radius.xl)
    ScaleOnPress(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = shape,
                    ambientColor = AppColors.Shadow.color,
                    spotColor = AppColors.Shadow.color,
                )
                .clip(shape)
                .background(gradient)
                .border(AppLayout.BorderWidth.thin, AppColors.Border.light, shape)
                .padding(vertical = AppSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .shadow(8.dp, CircleShape, ambientColor = AppColors.Shadow.color, spotColor = AppColors.Shadow.color)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    style = AppTypography.headline.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = Color.White,
                )
                Text(subtitle, style = AppTypography.caption1, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}
