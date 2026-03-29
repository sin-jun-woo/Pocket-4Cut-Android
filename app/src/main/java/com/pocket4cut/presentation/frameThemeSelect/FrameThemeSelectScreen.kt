package com.pocket4cut.presentation.frameThemeSelect

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.frame.CollagePreviewScaledToFit
import com.pocket4cut.frame.FrameCatalog
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FrameThemeSelectScreen(
    frameType: FrameType,
    selectedPhotoPaths: List<String>,
    frameStyle: FrameStyle,
    onSelectTheme: (FrameTheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themes = remember(frameType) { FrameCatalog.themes(frameType) }
    var selectedTheme by remember(themes) { mutableStateOf(themes.firstOrNull()) }
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedPhotoPaths) {
        if (selectedPhotoPaths.isEmpty()) return@LaunchedEffect
        isLoading = true
        bitmaps = withContext(Dispatchers.IO) {
            selectedPhotoPaths.mapNotNull { path -> BitmapDecoding.decodeSampled(path, 800) }
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppSpacing.xxxl,
                        start = AppSpacing.Screen.horizontal,
                        end = AppSpacing.Screen.horizontal,
                        bottom = AppSpacing.lg,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "프레임 선택",
                    style = AppTypography.title2,
                    color = AppColors.Text.primary,
                )
                Spacer(Modifier.size(44.dp))
            }

            // Center preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = AppColors.Accent.pink)
                } else if (selectedTheme != null) {
                    CollagePreviewScaledToFit(
                        images = bitmaps,
                        frameType = frameType,
                        frameStyle = frameStyle,
                        theme = selectedTheme!!,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // Bottom panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Background.primary)
                    .padding(horizontal = AppSpacing.Screen.horizontal),
            ) {
                Row(
                    modifier = Modifier.padding(bottom = AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Text(
                        text = "테마",
                        style = AppTypography.headline,
                        color = AppColors.Text.secondary,
                    )
                    Text(
                        text = selectedTheme?.name.orEmpty(),
                        style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.Accent.pink,
                    )
                }

                // Horizontal scrollable theme cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    themes.forEach { theme ->
                        ThemeCard(
                            theme = theme,
                            images = bitmaps,
                            frameType = frameType,
                            frameStyle = frameStyle,
                            isSelected = selectedTheme?.id == theme.id,
                            onSelect = { selectedTheme = theme },
                        )
                    }
                }

                Spacer(Modifier.height(AppSpacing.lg))

                PrimaryButton(
                    text = "${selectedTheme?.name ?: "테마"} 선택",
                    onClick = { selectedTheme?.let(onSelectTheme) },
                    enabled = selectedTheme != null,
                    fullWidth = true,
                )

                Spacer(Modifier.height(AppSpacing.Layout.ctaBottomSpace))
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: FrameTheme,
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(AppLayout.Radius.md)
    val borderColor = if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .width(140.dp)
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = AppColors.Accent.pink.copy(alpha = 0.2f),
                        spotColor = AppColors.Accent.pink.copy(alpha = 0.3f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(AppColors.Background.tertiary)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onSelect)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mini CollagePreview
        Box(
            modifier = Modifier
                .width(108.dp)
                .height(132.dp)
                .clip(RoundedCornerShape(AppLayout.Radius.xs)),
        ) {
            CollagePreviewScaledToFit(
                images = images,
                frameType = frameType,
                frameStyle = frameStyle,
                theme = theme,
                modifier = Modifier.fillMaxSize(),
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AppColors.Accent.pink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = theme.name,
            style = AppTypography.caption1.copy(
                color = if (isSelected) AppColors.Text.primary else AppColors.Text.secondary,
            ),
            maxLines = 1,
        )
    }
}
