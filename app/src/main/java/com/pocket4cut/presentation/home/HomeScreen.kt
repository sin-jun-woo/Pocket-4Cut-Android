package com.pocket4cut.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import com.pocket4cut.ui.designsystem.components.SecondaryButton

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onGallery: () -> Unit,
    onSettings: () -> Unit = {},
    galleryCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary)
            .padding(horizontal = AppSpacing.Screen.horizontal),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.xxxl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "POCKET / 4CUT",
                        style = AppTypography.caption1.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                        ),
                        color = AppColors.Text.primary,
                    )
                    Text(
                        text = "SELF PHOTO BOOTH",
                        style = AppTypography.caption2.copy(letterSpacing = 1.1.sp),
                        color = AppColors.Text.tertiary,
                    )
                }
                IconCircleButton(onClick = onSettings, variant = IconButtonVariant.SOLID) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.xxl))
            Text(
                text = "오늘의 네 컷을\n만들어볼까요?",
                style = AppTypography.largeTitle.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.8).sp,
                ),
                color = AppColors.Text.primary,
            )
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                text = "준비되면 바로 촬영을 시작하세요.",
                style = AppTypography.callout,
                color = AppColors.Text.secondary,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                PrintStripPreview()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.Layout.ctaBottomSpace),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            PrimaryButton(
                text = "촬영 시작",
                onClick = onStart,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                },
            )
            SecondaryButton(
                text = if (galleryCount > 0) "보관함  ·  $galleryCount" else "보관함",
                onClick = onGallery,
                icon = {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(19.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun PrintStripPreview() {
    val paperShape = RoundedCornerShape(AppLayout.Radius.xs)
    val photoTones = listOf(
        AppColors.Accent.peach,
        AppColors.Accent.sky,
        AppColors.Accent.mint,
        AppColors.Accent.pinkLight,
    )

    Column(
        modifier = Modifier
            .graphicsLayer { rotationZ = -1.8f }
            .shadow(
                elevation = 8.dp,
                shape = paperShape,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.16f),
            )
            .width(154.dp)
            .background(AppColors.Background.card, paperShape)
            .border(1.dp, AppColors.Border.subtle, paperShape)
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        photoTones.forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(color.copy(alpha = 0.52f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "0${index + 1}",
                    style = AppTypography.caption2.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = AppColors.Text.primary.copy(alpha = 0.52f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "POCKET 4CUT",
                style = AppTypography.caption2.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                color = AppColors.Text.primary,
            )
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 3.dp)
                    .background(AppColors.Accent.pink),
            )
        }
    }
}
