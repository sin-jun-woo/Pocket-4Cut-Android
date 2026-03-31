package com.pocket4cut.presentation.frameFlow

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.sp
import com.pocket4cut.frame.CollagePreviewScaledToFit
import com.pocket4cut.frame.CustomFrameDesign
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.frame.SeasonBackgroundFrameFactory
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import com.pocket4cut.ui.designsystem.theme.Season
import androidx.compose.material3.Icon

@Composable
fun SeasonBackgroundFramePickScreen(
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onCompleted: (Season) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seasons = remember { Season.entries.toList() }
    var selected by remember { mutableStateOf(Season.SPRING) }
    val seasonDesign = remember(selected) { SeasonBackgroundFrameFactory.design(selected) }

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
                text = "배경 프레임",
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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(horizontal = AppSpacing.Screen.horizontal),
                contentAlignment = Alignment.Center,
            ) {
                SeasonPreviewContent(
                    images = images,
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    customFrameDesign = seasonDesign,
                )
            }

            Spacer(Modifier.height(AppSpacing.md))

            Text(
                text = "시즌을 골라요",
                style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Text.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal),
            )

            Spacer(Modifier.height(AppSpacing.md))

            Column(
                modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                seasons.chunked(2).forEach { rowSeasons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    ) {
                        rowSeasons.forEach { season ->
                            val subtitle = seasonDecoEmojis(season)
                            SeasonCard(
                                season = season,
                                themeSubtitle = subtitle,
                                selected = season == selected,
                                onClick = { selected = season },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowSeasons.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.Screen.horizontal),
        ) {
            PrimaryButton(
                text = "${selected.displayName} 배경 선택",
                onClick = { onCompleted(selected) },
                fullWidth = true,
            )
            Spacer(Modifier.height(AppSpacing.Layout.ctaBottomSpace))
        }
    }
}

@Composable
private fun SeasonPreviewContent(
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    customFrameDesign: CustomFrameDesign,
) {
    if (images.isNotEmpty()) {
        CollagePreviewScaledToFit(
            images = images,
            frameType = frameType,
            frameStyle = frameStyle,
            theme = theme,
            customFrameDesign = customFrameDesign,
            modifier = Modifier
                .shadow(
                    elevation = 60.dp,
                    shape = RoundedCornerShape(AppLayout.Radius.xl),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f),
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
                .background(customFrameDesign.resolvedFillColor),
        )
    }
}

@Composable
private fun SeasonCard(
    season: Season,
    themeSubtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppLayout.Radius.lg)
    val seasonBaseHex = com.pocket4cut.frame.SeasonHTMLFrameStyle.baseHex(season)
    val seasonBgColor = Color((0xFF000000 or seasonBaseHex).toInt()).copy(alpha = 0.45f)
    Row(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 2.5.dp else AppLayout.BorderWidth.thin,
                color = if (selected) AppColors.Accent.pink else AppColors.Border.subtle,
                shape = shape,
            )
            .background(seasonBgColor)
            .clickable(onClick = onClick)
            .padding(AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Text(
            text = season.emoji,
            fontSize = 28.sp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = season.displayName,
                style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Text.primary,
            )
            Text(
                text = themeSubtitle,
                style = AppTypography.caption2,
                color = AppColors.Text.tertiary,
            )
        }
    }
}

private fun seasonDecoEmojis(season: Season): String = when (season) {
    Season.SPRING -> "💕⭐✨💫"
    Season.SUMMER -> "🌊☀️🐚🎐"
    Season.AUTUMN -> "🍂🍁🌰🦊"
    Season.WINTER -> "❄️⛄🌨️💎"
}
