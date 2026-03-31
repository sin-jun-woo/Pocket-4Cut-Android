package com.pocket4cut.presentation.frameFlow

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocket4cut.frame.CollagePreviewScaledToFit
import com.pocket4cut.frame.FrameColor
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton

@Composable
fun ColorFramePalettePickScreen(
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onCompleted: (FrameColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(frameType) { FrameColors.colorFramePalette(frameType) }
    var selected by remember(palette) { mutableStateOf(palette.first()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = AppSpacing.xxxl,
                    start = AppSpacing.Screen.horizontal,
                    end = AppSpacing.Screen.horizontal,
                    bottom = AppSpacing.md,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(AppLayout.Height.IconButton.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = "색 프레임",
                style = AppTypography.title2,
                color = AppColors.Text.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.width(AppLayout.Height.IconButton.md),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconCircleButton(onClick = onDismiss, variant = IconButtonVariant.SOLID) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(horizontal = AppSpacing.Screen.horizontal),
            contentAlignment = Alignment.Center,
        ) {
            if (images.isNotEmpty()) {
                CollagePreviewScaledToFit(
                    images = images,
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    overrideBackground = selected.color,
                    modifier = Modifier
                        .shadow(
                            elevation = 32.dp,
                            shape = RoundedCornerShape(AppLayout.Radius.xl),
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.28f),
                            spotColor = Color.Black.copy(alpha = 0.45f),
                        )
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(AppLayout.Radius.xl),
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.18f),
                            spotColor = Color.Black.copy(alpha = 0.28f),
                        )
                        .fillMaxWidth(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 60.dp,
                            shape = RoundedCornerShape(AppLayout.Radius.xl),
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.3f),
                            spotColor = Color.Black.copy(alpha = 0.3f),
                        )
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(AppLayout.Radius.xl))
                        .background(selected.color),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.Screen.horizontal),
        ) {
            Row(
                modifier = Modifier.padding(bottom = AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Text(
                    text = "색상",
                    style = AppTypography.headline,
                    color = AppColors.Text.secondary,
                )
                Text(
                    text = selected.name,
                    style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.Accent.pink,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                palette.forEach { color ->
                    ColorChip(
                        frameColor = color,
                        selected = color.id == selected.id,
                        onClick = { selected = color },
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))

            PrimaryButton(
                text = "${selected.name} 선택",
                onClick = { onCompleted(selected) },
                fullWidth = true,
            )

            Spacer(Modifier.height(AppSpacing.Layout.ctaBottomSpace))
        }
    }
}

@Composable
private fun ColorChip(
    frameColor: FrameColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) AppColors.Accent.pink else AppColors.Border.subtle,
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(frameColor.color),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.Accent.pink.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AppColors.Accent.pink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            frameColor.name,
            style = AppTypography.caption2.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) AppColors.Text.primary else AppColors.Text.tertiary,
        )
    }
}
