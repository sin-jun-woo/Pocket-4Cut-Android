package com.pocket4cut.presentation.gallery

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary)
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
                onClose = onBack,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = AppSpacing.md),
                horizontalArrangement = Arrangement.Center,
            ) {
                GalleryViewModeToggle(
                    selectedMode = viewMode,
                    onModeSelected = { viewMode = it },
                )
            }
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
                                    Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = AppColors.Text.tertiary,
                                    modifier = Modifier.size(60.dp),
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
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = AppSpacing.md,
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
    }
}

/* ── View‑mode toggle (종류 / 전체) ── */

@Composable
private fun GalleryViewModeToggle(
    selectedMode: GalleryViewMode,
    onModeSelected: (GalleryViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabShape = RoundedCornerShape(AppLayout.Radius.sm)
    val segments = listOf(
        GalleryViewMode.BY_KIND to "종류",
        GalleryViewMode.BY_DATE to "전체",
    )
    Row(
        modifier = modifier
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        segments.forEach { (mode, label) ->
            val isSelected = mode == selectedMode
            Box(
                modifier = Modifier
                    .clip(tabShape)
                    .background(Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        style = AppTypography.caption2.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (isSelected) AppColors.Accent.pink else AppColors.Text.secondary,
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(AppColors.Accent.pink),
                        )
                    }
                }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(AppLayout.Radius.xs))
                .background(AppColors.Background.secondary),
        ) {
            AsyncImage(
                model = File(item.resultPath),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            androidx.compose.animation.AnimatedVisibility(
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
                        .clip(RoundedCornerShape(AppLayout.Radius.xs))
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.frameKindLabel,
                style = AppTypography.caption2.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Text.primary,
            )
            Text(
                text = item.shortDateLabel,
                style = AppTypography.caption2,
                color = AppColors.Text.tertiary,
            )
        }
    }
}
