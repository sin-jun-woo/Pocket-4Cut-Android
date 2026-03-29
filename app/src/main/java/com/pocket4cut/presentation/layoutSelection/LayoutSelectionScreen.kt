package com.pocket4cut.presentation.layoutSelection

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.frame.CollagePreviewScaledToFit
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.LayoutType
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
fun LayoutSelectionScreen(
    selectedPhotoPaths: List<String>,
    requiredCount: Int,
    frameType: FrameType,
    onSelectLayout: (FrameLayoutId) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layouts = remember(frameType) { FrameLayouts.bySlots(frameType.selectCount) }
    var selectedLayout by remember(layouts) {
        mutableStateOf(layouts.firstOrNull()?.id ?: FrameLayoutId.FOUR_VERTICAL)
    }
    val defaultTheme = remember(frameType) { FrameCatalog.themes(frameType).first() }

    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(selectedPhotoPaths, requiredCount) {
        if (selectedPhotoPaths.isEmpty()) {
            bitmaps = emptyList()
            return@LaunchedEffect
        }
        bitmaps = withContext(Dispatchers.IO) {
            selectedPhotoPaths.mapNotNull { path -> BitmapDecoding.decodeSampled(path, reqSize = 800) }
        }
    }

    val selectedStyle = remember(selectedLayout, layouts) {
        layouts.firstOrNull { it.id == selectedLayout } ?: layouts.firstOrNull()
    }
    val selectedName = selectedStyle?.name.orEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                IconCircleButton(onClick = onCancel, variant = IconButtonVariant.SOLID) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "레이아웃 선택",
                    style = AppTypography.title2,
                    color = AppColors.Text.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(AppLayout.Height.IconButton.md))
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = AppColors.Border.subtle,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(top = AppSpacing.lg),
            ) {
                Text(
                    text = "미리보기",
                    style = AppTypography.subheadline,
                    color = AppColors.Text.secondary,
                )
                Spacer(Modifier.height(AppSpacing.sm))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val previewShape = RoundedCornerShape(AppLayout.Radius.lg)
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 14.dp,
                                shape = previewShape,
                                clip = false,
                                ambientColor = Color.Black.copy(alpha = 0.35f),
                                spotColor = Color.Black.copy(alpha = 0.45f),
                            )
                            .size(width = 280.dp, height = 320.dp)
                            .clip(previewShape),
                    ) {
                        AnimatedContent(
                            targetState = selectedLayout,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith
                                    fadeOut(animationSpec = tween(140))
                            },
                            label = "collagePreview",
                        ) { layoutId ->
                            val previewStyle = remember(layoutId) { FrameLayouts.byId(layoutId) }
                            CollagePreviewScaledToFit(
                                images = bitmaps,
                                frameType = frameType,
                                frameStyle = previewStyle,
                                theme = defaultTheme,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.xl))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "배치 선택",
                    style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.Text.primary,
                )
                Text(
                    text = selectedName,
                    style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.Accent.pink,
                )
            }
            Spacer(Modifier.height(AppSpacing.xxs))
            Text(
                text = "마음에 드는 배치를 골라주세요",
                style = AppTypography.caption1,
                color = AppColors.Text.tertiary,
                modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal),
            )
            Spacer(Modifier.height(AppSpacing.md))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = AppSpacing.Screen.horizontal,
                    end = AppSpacing.Screen.horizontal,
                    bottom = 120.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                items(layouts, key = { it.id }) { style ->
                    LayoutCard(
                        frameStyle = style,
                        isSelected = selectedLayout == style.id,
                        onSelect = { selectedLayout = style.id },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary),
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = AppColors.Border.subtle,
            )
            PrimaryButton(
                text = "$selectedName 선택",
                onClick = { onSelectLayout(selectedLayout) },
                fullWidth = true,
                modifier = Modifier
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(top = AppSpacing.md, bottom = AppSpacing.Layout.ctaBottomSpace),
            )
        }
    }
}

@Composable
private fun LayoutCard(
    frameStyle: FrameStyle,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val bg = if (isSelected) AppColors.Accent.pinkSubtle else AppColors.Background.tertiary
    val borderColor = if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle
    val cardShape = RoundedCornerShape(AppLayout.Radius.lg)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = cardShape,
                        clip = false,
                        ambientColor = AppColors.Accent.pink.copy(alpha = 0.22f),
                        spotColor = AppColors.Accent.pink.copy(alpha = 0.38f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(cardShape)
            .background(bg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = cardShape,
            )
            .clickable(onClick = onSelect)
            .padding(AppSpacing.md),
    ) {
        Column {
            MiniAbstractLayoutPreview(
                frameStyle = frameStyle,
                isCardSelected = isSelected,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                text = frameStyle.name,
                style = AppTypography.subheadline.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) AppColors.Text.primary else AppColors.Text.secondary,
                ),
            )
            Text(
                text = frameStyle.description,
                style = AppTypography.caption1.copy(
                    color = if (isSelected) AppColors.Text.secondary else AppColors.Text.tertiary,
                ),
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppColors.Accent.pink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AppColors.Text.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniAbstractLayoutPreview(
    frameStyle: FrameStyle,
    isCardSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val cellFill = if (isCardSelected) {
        AppColors.Accent.pink.copy(alpha = 0.4f)
    } else {
        Color.White.copy(alpha = 0.2f)
    }
    val cellBorder = AppColors.Border.subtle
    val gap = 4.dp
    val cellShape = RoundedCornerShape(4.dp)

    @Composable
    fun Cell(mod: Modifier) {
        Box(
            modifier = mod
                .clip(cellShape)
                .background(cellFill)
                .border(1.dp, cellBorder, cellShape),
        )
    }

    when (frameStyle.layout) {
        LayoutType.VERTICAL -> {
            Column(
                modifier = modifier.height(100.dp),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                repeat(frameStyle.slots) {
                    Cell(Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
        LayoutType.HORIZONTAL -> {
            Row(
                modifier = modifier.height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                repeat(frameStyle.slots) {
                    Cell(Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        LayoutType.GRID -> {
            Column(
                modifier = modifier.height(100.dp),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                repeat(frameStyle.rows) { row ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        repeat(frameStyle.columns) { col ->
                            val index = row * frameStyle.columns + col
                            if (index < frameStyle.slots) {
                                Cell(Modifier.weight(1f).fillMaxHeight())
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
