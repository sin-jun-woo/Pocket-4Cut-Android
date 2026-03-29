package com.pocket4cut.presentation.edit

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket4cut.frame.*
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*

@Composable
fun EditScreen(
    frameType: FrameType,
    sessionId: String,
    selectedIndexes: List<Int>,
    layoutId: String,
    onBack: () -> Unit,
    onContinueToDetailEdit: () -> Unit,
    onComplete: (resultPath: String) -> Unit,
    modifier: Modifier = Modifier,
    themeId: String = "",
    viewModel: EditViewModel = viewModel(),
) {
    val frameLayoutId = remember(layoutId) {
        runCatching { FrameLayoutId.valueOf(layoutId) }.getOrElse { FrameLayoutId.FOUR_VERTICAL }
    }
    val frameStyle = remember(frameLayoutId) { FrameLayouts.byId(frameLayoutId) }
    val frameTheme = remember(themeId, frameType) {
        FrameCatalog.themes(frameType).firstOrNull { it.id == themeId }
            ?: FrameCatalog.themes(frameType).first()
    }

    LaunchedEffect(frameType, sessionId, selectedIndexes, layoutId) {
        viewModel.init(frameType, sessionId, selectedIndexes, frameLayoutId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp),
        ) {
            // ── 1. Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppSpacing.Screen.top,
                        start = AppSpacing.Screen.horizontal,
                        end = AppSpacing.Screen.horizontal,
                        bottom = AppSpacing.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(Icons.Default.Close, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                }
                Text(
                    "편집",
                    style = AppTypography.title2,
                    color = AppColors.Text.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.size(44.dp))
            }
            HorizontalDivider(color = AppColors.Border.subtle, thickness = 0.5.dp)

            Spacer(Modifier.height(AppSpacing.xl))

            // ── 2. Preview ──
            PreviewSection(
                uiState = uiState,
                frameType = frameType,
                frameStyle = frameStyle,
                theme = frameTheme,
                bottomCaption = buildBottomCaption(uiState),
            )

            Spacer(Modifier.height(AppSpacing.xl))

            // ── 3. Filter ──
            FilterSection(uiState = uiState, onSelect = viewModel::setFilter)

            Spacer(Modifier.height(AppSpacing.xl))

            // ── 4. Frame Color ──
            FrameColorSection(uiState = uiState, onSelect = viewModel::setFrameColor)

            Spacer(Modifier.height(AppSpacing.xl))

            // ── 5. Text ──
            TextInputSection(
                uiState = uiState,
                onTextChange = viewModel::setText,
                focusManager = focusManager,
            )

            Spacer(Modifier.height(AppSpacing.xl))

            // ── 6. Date Toggle ──
            DateToggleSection(uiState = uiState, onToggle = viewModel::toggleDate)

            Spacer(Modifier.height(AppSpacing.xl))

            // ── 7. Order ──
            OrderSection(
                uiState = uiState,
                frameStyle = frameStyle,
                onTapCell = viewModel::tapOrderCell,
                onMoveSlot = viewModel::moveOrderSlot,
            )

            Spacer(Modifier.height(AppSpacing.xl))
        }

        // ── 8. Bottom Bar ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary),
        ) {
            HorizontalDivider(color = AppColors.Border.subtle, thickness = 0.5.dp)
            Spacer(Modifier.height(AppSpacing.md))
            PrimaryButton(
                text = "사진별 상세 편집",
                onClick = {
                    viewModel.persistPendingForDetailEdit(sessionId)
                    onContinueToDetailEdit()
                },
                fullWidth = true,
                icon = {
                    Icon(Icons.Default.Edit, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = AppSpacing.Layout.ctaBottomSpace),
            )
        }
    }
}

// ── Preview Section ──────────────────────────────────────────────────────────

@Composable
private fun PreviewSection(
    uiState: EditUiState,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    bottomCaption: String?,
) {
    Column(modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal)) {
        Text(
            "미리보기",
            style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.Text.secondary,
        )
        Spacer(Modifier.height(AppSpacing.md))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .clip(RoundedCornerShape(AppLayout.Radius.xl))
                    .background(uiState.selectedFrameColor.color),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = AppColors.Accent.pink)
            }
        } else if (uiState.filteredPreviewImages.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .clip(RoundedCornerShape(AppLayout.Radius.xl)),
            ) {
                CollagePreviewScaledToFit(
                    images = uiState.filteredPreviewImages,
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    overrideBackground = uiState.selectedFrameColor.color,
                    bottomCaption = bottomCaption,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        val overlayText = buildOverlayText(uiState)
        if (overlayText.isNotEmpty()) {
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                overlayText,
                style = AppTypography.caption1,
                color = AppColors.Text.tertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private fun buildOverlayText(state: EditUiState): String {
    val parts = buildList {
        if (state.customText.isNotBlank()) add(state.customText)
        if (state.showDate) add(state.dateString)
    }
    return parts.joinToString("  ·  ")
}

private fun buildBottomCaption(state: EditUiState): String? {
    val parts = buildList {
        if (state.customText.isNotBlank()) add(state.customText.trim())
        if (state.showDate) add(state.dateString)
    }
    if (parts.isEmpty()) return null
    return parts.joinToString(" · ")
}

// ── Filter Section ───────────────────────────────────────────────────────────

@Composable
private fun FilterSection(
    uiState: EditUiState,
    onSelect: (FilterId) -> Unit,
) {
    Column(modifier = Modifier.padding(start = AppSpacing.Screen.horizontal)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = AppSpacing.Screen.horizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "필터",
                style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Text.secondary,
            )
            Text(
                uiState.selectedFilter.displayName,
                style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Accent.pink,
            )
        }
        Spacer(Modifier.height(AppSpacing.md))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            FilterId.entries.forEach { filter ->
                val isSelected = uiState.selectedFilter == filter
                FilterChip(
                    filter = filter,
                    isSelected = isSelected,
                    thumbnail = uiState.filterChipThumbnails[filter],
                    onClick = { onSelect(filter) },
                )
            }
            Spacer(Modifier.width(AppSpacing.Screen.horizontal))
        }
    }
}

@Composable
private fun FilterChip(
    filter: FilterId,
    isSelected: Boolean,
    thumbnail: Bitmap?,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .then(
                    if (isSelected) Modifier.shadow(8.dp, CircleShape, ambientColor = AppColors.Accent.pink.copy(alpha = 0.3f), spotColor = AppColors.Accent.pink.copy(alpha = 0.4f))
                    else Modifier,
                )
                .clip(CircleShape)
                .background(AppColors.Background.tertiary)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle,
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = filter.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Default.PhotoCamera,
                    null,
                    tint = if (isSelected) AppColors.Accent.pink else AppColors.Text.secondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(AppSpacing.xxs))
        Text(
            filter.displayName,
            style = AppTypography.caption1.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (isSelected) AppColors.Text.primary else AppColors.Text.secondary,
        )
    }
}

// ── Frame Color Section ──────────────────────────────────────────────────────

@Composable
private fun FrameColorSection(
    uiState: EditUiState,
    onSelect: (FrameColor) -> Unit,
) {
    Column(modifier = Modifier.padding(start = AppSpacing.Screen.horizontal)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = AppSpacing.Screen.horizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "프레임 색상",
                style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Text.secondary,
            )
            Text(
                uiState.selectedFrameColor.name,
                style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Accent.pink,
            )
        }
        Spacer(Modifier.height(AppSpacing.md))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            FrameColors.all.forEach { fc ->
                val isSelected = uiState.selectedFrameColor.id == fc.id
                FrameColorChip(fc = fc, isSelected = isSelected, onClick = { onSelect(fc) })
            }
            Spacer(Modifier.width(AppSpacing.Screen.horizontal))
        }
    }
}

@Composable
private fun FrameColorChip(
    fc: FrameColor,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .then(
                    if (isSelected) Modifier.shadow(8.dp, CircleShape, ambientColor = AppColors.Accent.pink.copy(alpha = 0.3f), spotColor = AppColors.Accent.pink.copy(alpha = 0.4f))
                    else Modifier,
                )
                .clip(CircleShape)
                .then(
                    if (fc.gradientBrush != null) Modifier.background(fc.gradientBrush, CircleShape)
                    else Modifier.background(fc.color, CircleShape),
                )
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle,
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(AppColors.Accent.pink.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AppColors.Accent.pink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(AppSpacing.xxs))
        Text(
            fc.name,
            style = AppTypography.caption2.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (isSelected) AppColors.Text.primary else AppColors.Text.tertiary,
        )
    }
}

// ── Text Input Section ───────────────────────────────────────────────────────

@Composable
private fun TextInputSection(
    uiState: EditUiState,
    onTextChange: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.TextFormat,
                null,
                tint = AppColors.Text.secondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(AppSpacing.sm))
            Text(
                "텍스트",
                style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Text.secondary,
            )
        }
        Spacer(Modifier.height(AppSpacing.md))
        TextField(
            value = uiState.customText,
            onValueChange = { if (it.length <= 30) onTextChange(it) },
            placeholder = { Text("메시지를 입력하세요", color = AppColors.Text.tertiary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AppColors.Background.secondary,
                unfocusedContainerColor = AppColors.Background.tertiary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = AppColors.Accent.pink,
                focusedTextColor = AppColors.Text.primary,
                unfocusedTextColor = AppColors.Text.primary,
            ),
            shape = RoundedCornerShape(AppLayout.Radius.md),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .then(
                    if (isFocused) Modifier.border(1.5.dp, AppColors.Accent.pink, RoundedCornerShape(AppLayout.Radius.md))
                    else Modifier.border(1.dp, AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.md)),
                ),
        )
        Spacer(Modifier.height(AppSpacing.xs))
        Text(
            "${uiState.customText.length} / 30",
            style = AppTypography.caption1,
            color = AppColors.Text.tertiary,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

// ── Date Toggle Section ──────────────────────────────────────────────────────

@Composable
private fun DateToggleSection(
    uiState: EditUiState,
    onToggle: () -> Unit,
) {
    val cardShape = RoundedCornerShape(AppLayout.Radius.md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.Screen.horizontal)
            .clip(cardShape)
            .background(AppColors.Background.tertiary, cardShape)
            .border(1.dp, AppColors.Border.subtle, cardShape)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.DateRange,
            null,
            tint = AppColors.Text.secondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(AppSpacing.sm))
        Text(
            "날짜 표시",
            style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.Text.primary,
        )
        Spacer(Modifier.width(AppSpacing.xs))
        Text(
            uiState.dateString,
            style = AppTypography.caption1,
            color = AppColors.Text.tertiary,
        )
        Spacer(Modifier.weight(1f))
        PinkToggle(checked = uiState.showDate, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun PinkToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) AppColors.Accent.pink else AppColors.Background.secondary,
        animationSpec = tween(AppAnimation.Duration.fast),
        label = "trackColor",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(AppAnimation.Duration.fast),
        label = "knobOffset",
    )

    Box(
        modifier = Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .offset(x = knobOffset)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

// ── Order Section ────────────────────────────────────────────────────────────

@Composable
private fun OrderSection(
    uiState: EditUiState,
    frameStyle: FrameStyle,
    onTapCell: (Int) -> Unit,
    onMoveSlot: (from: Int, to: Int) -> Unit,
) {
    val imageCount = uiState.orderedImages.size
    if (imageCount == 0) return

    var cellBounds by remember(imageCount) { mutableStateOf<Map<Int, Rect>>(emptyMap()) }
    var cellLayouts by remember(imageCount) { mutableStateOf<Map<Int, LayoutCoordinates>>(emptyMap()) }
    var draggingIndex by remember(imageCount) { mutableStateOf<Int?>(null) }
    val cellBoundsState = rememberUpdatedState(cellBounds)
    val cellLayoutsState = rememberUpdatedState(cellLayouts)
    val activeDragSlot = remember(imageCount) { intArrayOf(-1) }

    Column(modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal)) {
        Text(
            "사진 순서",
            style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.Text.secondary,
        )
        Spacer(Modifier.height(AppSpacing.xxs))
        Text(
            "길게 눌러 드래그하면 순서를 바꿀 수 있어요 · 번호를 누르면 두 칸 맞바꿈",
            style = AppTypography.caption1,
            color = AppColors.Text.tertiary,
        )
        Spacer(Modifier.height(AppSpacing.md))

        val columns = when {
            imageCount <= 2 -> 2
            imageCount <= 4 -> 2
            else -> 3
        }
        val rows = (imageCount + columns - 1) / columns

        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            repeat(rows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    repeat(columns) { col ->
                        val index = row * columns + col
                        if (index < imageCount) {
                            OrderCell(
                                image = uiState.orderedImages[index],
                                displayNumber = index + 1,
                                isSelected = uiState.selectedSwapIndex == index,
                                isDragging = draggingIndex == index,
                                cellAspectWidthOverHeight = frameStyle.cellAspectWidthOverHeight,
                                onBadgeClick = { onTapCell(index) },
                                dragModifier = Modifier.pointerInput(index, imageCount) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            activeDragSlot[0] = index
                                            draggingIndex = index
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val lc = cellLayoutsState.value[index] ?: return@detectDragGesturesAfterLongPress
                                            val rootPos = lc.localToRoot(change.position)
                                            val map = cellBoundsState.value
                                            val target = map.entries.firstOrNull { (_, r) ->
                                                rootPos.x >= r.left && rootPos.x < r.right &&
                                                    rootPos.y >= r.top && rootPos.y < r.bottom
                                            }?.key
                                            val from = activeDragSlot[0]
                                            if (target != null && from >= 0 && target != from) {
                                                onMoveSlot(from, target)
                                                activeDragSlot[0] = target
                                                draggingIndex = target
                                            }
                                        },
                                        onDragEnd = {
                                            activeDragSlot[0] = -1
                                            draggingIndex = null
                                        },
                                        onDragCancel = {
                                            activeDragSlot[0] = -1
                                            draggingIndex = null
                                        },
                                    )
                                },
                                onBoundsInRoot = { coords ->
                                    cellBounds = cellBounds + (index to coords.boundsInRoot())
                                    cellLayouts = cellLayouts + (index to coords)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCell(
    image: Bitmap,
    displayNumber: Int,
    isSelected: Boolean,
    isDragging: Boolean,
    cellAspectWidthOverHeight: Float,
    onBadgeClick: () -> Unit,
    dragModifier: Modifier,
    onBoundsInRoot: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isDragging -> AppColors.Accent.pink
            isSelected -> AppColors.Accent.pink
            else -> Color.Transparent
        },
        animationSpec = tween(AppAnimation.Duration.fast),
        label = "orderBorder",
    )

    Box(
        modifier = modifier
            .aspectRatio(cellAspectWidthOverHeight)
            .onGloballyPositioned(onBoundsInRoot)
            .clip(RoundedCornerShape(AppLayout.Radius.sm))
            .border(
                width = if (isSelected || isDragging) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(AppLayout.Radius.sm),
            ),
    ) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(dragModifier),
        )

        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) AppColors.Accent.pink else AppColors.Background.primary.copy(alpha = 0.7f),
                )
                .align(Alignment.TopStart)
                .clickable(onClick = onBadgeClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$displayNumber",
                style = AppTypography.caption1.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
    }
}
