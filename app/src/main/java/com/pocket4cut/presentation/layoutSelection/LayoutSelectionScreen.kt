package com.pocket4cut.presentation.layoutSelection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.LayoutType
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton

@Composable
fun LayoutSelectionScreen(
    selectedPhotoPaths: List<String>,
    requiredCount: Int,
    onSelectLayout: (FrameLayoutId) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layouts = remember(requiredCount) { FrameLayouts.bySlots(requiredCount) }
    var selectedLayout by remember { mutableStateOf(layouts.firstOrNull()?.id ?: FrameLayoutId.FOUR_VERTICAL) }

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
                    .padding(top = AppSpacing.xxxl, start = AppSpacing.Screen.horizontal, end = AppSpacing.Screen.horizontal, bottom = AppSpacing.xl),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text("레이아웃 선택", style = AppTypography.title1, color = AppColors.Text.primary)
                    Spacer(Modifier.height(AppSpacing.xxs))
                    Text("마음에 드는 배치를 골라주세요", style = AppTypography.callout, color = AppColors.Text.secondary)
                }
                IconCircleButton(onClick = onCancel, variant = IconButtonVariant.SOLID) {
                    Icon(Icons.Default.Close, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                }
            }

            // Layout grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppSpacing.Screen.horizontal),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                itemsIndexed(layouts) { _, style ->
                    LayoutCard(
                        frameStyle = style,
                        photos = selectedPhotoPaths,
                        isSelected = selectedLayout == style.id,
                        onSelect = { selectedLayout = style.id },
                    )
                }
            }
        }

        // Bottom CTA
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary)
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(bottom = AppSpacing.Layout.ctaBottomSpace, top = AppSpacing.md),
        ) {
            val name = layouts.firstOrNull { it.id == selectedLayout }?.name ?: ""
            PrimaryButton(
                text = "$name 선택",
                onClick = { onSelectLayout(selectedLayout) },
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun LayoutCard(
    frameStyle: FrameStyle,
    photos: List<String>,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val bg = if (isSelected) AppColors.Accent.pinkSubtle else AppColors.Background.tertiary
    val borderColor = if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppLayout.Radius.lg))
            .background(bg)
            .border(2.dp, borderColor, RoundedCornerShape(AppLayout.Radius.lg))
            .clickable(onClick = onSelect)
            .padding(AppSpacing.md),
    ) {
        Column {
            // Mini preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(AppLayout.Radius.sm))
                    .background(androidx.compose.ui.graphics.Color.White)
                    .padding(4.dp),
            ) {
                LayoutPreview(frameStyle, photos)
            }
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                frameStyle.name,
                style = AppTypography.subheadline.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = if (isSelected) AppColors.Text.primary else AppColors.Text.secondary,
                ),
            )
            Text(
                frameStyle.description,
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
                Icon(Icons.Default.Check, null, tint = AppColors.Text.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun LayoutPreview(style: FrameStyle, photos: List<String>) {
    val displayPhotos = photos.take(style.slots)
    when (style.layout) {
        LayoutType.VERTICAL -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            displayPhotos.forEach { path ->
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(2.dp)),
                )
            }
        }
        LayoutType.HORIZONTAL -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            displayPhotos.forEach { path ->
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)),
                )
            }
        }
        LayoutType.GRID -> {
            val cols = style.gridColumns
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                displayPhotos.chunked(cols).forEach { row ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        row.forEach { path ->
                            AsyncImage(
                                model = path,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)),
                            )
                        }
                        repeat(cols - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
