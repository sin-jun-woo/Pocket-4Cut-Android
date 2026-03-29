package com.pocket4cut.presentation.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*
import java.io.File

private enum class GalleryViewMode {
    /** iOS `byDate` — 일 단위 섹션 */
    BY_DATE,
    /** iOS `byKind` — 2/4/6컷 */
    BY_KIND,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onOpen: (resultPath: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var viewMode by remember { mutableStateOf(GalleryViewMode.BY_DATE) }
    var longPressedItemId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<GalleryItem?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(viewMode) { longPressedItemId = null }

    val accentPink = AppColors.Accent.pink

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentPink.copy(alpha = 0.07f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.8f,
                    ),
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                longPressedItemId = null
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GalleryHeader(
                itemCount = uiState.items.size,
                viewMode = viewMode,
                onViewModeChanged = { viewMode = it },
                showToggle = uiState.items.isNotEmpty(),
                onClose = onBack,
            )

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Accent.pink)
                    }
                }

                uiState.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = AppColors.Text.tertiary,
                                    modifier = Modifier.size(48.dp),
                                )
                            },
                            title = "아직 저장된 콜라주가 없어요",
                            description = "촬영하고 완성하면 여기에 쌓여요.",
                            action = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                                ) {
                                    PrimaryButton(
                                        text = "촬영하러 가기",
                                        onClick = onBack,
                                        fullWidth = true,
                                    )
                                    SecondaryButton(
                                        text = "닫기",
                                        onClick = onBack,
                                        fullWidth = true,
                                    )
                                }
                            },
                        )
                    }
                }

                else -> {
                    GalleryContent(
                        uiState = uiState,
                        viewMode = viewMode,
                        longPressedItemId = longPressedItemId,
                        onItemClick = { item ->
                            if (longPressedItemId != null) {
                                longPressedItemId = null
                            } else {
                                onOpen(item.resultPath)
                            }
                        },
                        onItemLongPress = { item ->
                            longPressedItemId = item.sessionId
                        },
                        onDeleteClick = { item ->
                            deleteTarget = item
                            longPressedItemId = null
                        },
                    )
                }
            }
        }

        ConfirmDialog(
            visible = deleteTarget != null,
            title = "이 추억을 지울까요?",
            message = "저장된 사진 파일도 함께 삭제돼요.",
            confirmText = "삭제",
            cancelText = "취소",
            isDestructive = true,
            onConfirm = {
                deleteTarget?.let { viewModel.delete(it.sessionId) }
                deleteTarget = null
            },
            onCancel = { deleteTarget = null },
        )
    }
}

/* ── Header ── */

@Composable
private fun GalleryHeader(
    itemCount: Int,
    viewMode: GalleryViewMode,
    onViewModeChanged: (GalleryViewMode) -> Unit,
    showToggle: Boolean,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = AppSpacing.xxxl,
                start = AppSpacing.Screen.horizontal,
                end = AppSpacing.Screen.horizontal,
                bottom = AppSpacing.md,
            ),
    ) {
        IconCircleButton(
            onClick = onClose,
            variant = IconButtonVariant.SOLID,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = AppColors.Text.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "보관함",
                style = AppTypography.title2,
                color = AppColors.Text.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                "${itemCount}개의 추억",
                style = AppTypography.caption1,
                color = AppColors.Text.tertiary,
                textAlign = TextAlign.Center,
            )
        }
        if (showToggle) {
            GalleryViewModeToggle(
                selectedMode = viewMode,
                onModeSelected = onViewModeChanged,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/* ── View‑mode toggle (월별 / 전체) ── */

@Composable
private fun GalleryViewModeToggle(
    selectedMode: GalleryViewMode,
    onModeSelected: (GalleryViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val capsuleShape = RoundedCornerShape(AppLayout.Radius.full)
    Row(
        modifier = modifier
            .clip(capsuleShape)
            .background(AppColors.Background.tertiary)
            .border(1.dp, AppColors.Border.subtle, capsuleShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
            listOf(GalleryViewMode.BY_DATE, GalleryViewMode.BY_KIND).forEach { mode ->
            val isSelected = mode == selectedMode
            Row(
                modifier = Modifier
                    .clip(capsuleShape)
                    .background(if (isSelected) AppColors.Accent.pink else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = when (mode) {
                        GalleryViewMode.BY_DATE -> Icons.Default.CalendarMonth
                        GalleryViewMode.BY_KIND -> Icons.Default.GridView
                    },
                    contentDescription = null,
                    tint = if (isSelected) AppColors.Text.primary else AppColors.Text.tertiary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = when (mode) {
                        GalleryViewMode.BY_DATE -> "전체"
                        GalleryViewMode.BY_KIND -> "종류"
                    },
                    style = AppTypography.caption2.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isSelected) AppColors.Text.primary else AppColors.Text.tertiary,
                )
            }
        }
    }
}

/* ── Gallery content (month‑grouped / flat grid) ── */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryContent(
    uiState: GalleryUiState,
    viewMode: GalleryViewMode,
    longPressedItemId: String?,
    onItemClick: (GalleryItem) -> Unit,
    onItemLongPress: (GalleryItem) -> Unit,
    onDeleteClick: (GalleryItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.Screen.horizontal),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Component.cardGap),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Component.cardGap),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl),
    ) {
        when (viewMode) {
            GalleryViewMode.BY_DATE -> {
                uiState.dayBuckets.forEachIndexed { index, bucket ->
                    item(
                        key = "header_${bucket.key}",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SectionHeader(
                            title = bucket.label,
                            subtitle = "${bucket.items.size}장",
                            modifier = if (index > 0) Modifier.padding(top = AppSpacing.lg) else Modifier,
                        )
                    }
                    items(
                        items = bucket.items,
                        key = { it.sessionId },
                    ) { item ->
                        SessionCell(
                            item = item,
                            isLongPressed = longPressedItemId == item.sessionId,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongPress(item) },
                            onDeleteClick = { onDeleteClick(item) },
                        )
                    }
                }
            }

            GalleryViewMode.BY_KIND -> {
                uiState.kindBuckets.forEachIndexed { index, bucket ->
                    item(
                        key = "kind_${bucket.key}",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SectionHeader(
                            title = bucket.label,
                            subtitle = "${bucket.items.size}장",
                            modifier = if (index > 0) Modifier.padding(top = AppSpacing.lg) else Modifier,
                        )
                    }
                    items(
                        items = bucket.items,
                        key = { it.sessionId },
                    ) { item ->
                        SessionCell(
                            item = item,
                            isLongPressed = longPressedItemId == item.sessionId,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongPress(item) },
                            onDeleteClick = { onDeleteClick(item) },
                        )
                    }
                }
            }
        }

        item(
            key = "footer_hint",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            Text(
                text = "카드를 길게 눌러 삭제할 수 있어요.\n삭제한 내용은 복구할 수 없어요.",
                style = AppTypography.caption1,
                color = AppColors.Text.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.xl),
            )
        }
    }
}

/* ── Session cell (thumbnail card) ── */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionCell(
    item: GalleryItem,
    isLongPressed: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppLayout.Radius.lg))
            .background(AppColors.Background.secondary)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        AsyncImage(
            model = File(item.resultPath),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        ) {
            Text(
                text = item.frameKindLabel,
                style = AppTypography.caption2.copy(fontWeight = FontWeight.Bold),
                color = AppColors.Text.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(AppLayout.Radius.full))
                    .background(AppColors.Accent.pink.copy(alpha = 0.92f))
                    .padding(horizontal = AppSpacing.xs, vertical = 2.dp),
            )
            Text(
                text = item.shortDateLabel,
                style = AppTypography.caption2,
                color = AppColors.Text.primary.copy(alpha = 0.8f),
            )
        }

        AnimatedVisibility(
            visible = isLongPressed,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppSpacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppColors.Semantic.error)
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
