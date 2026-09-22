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
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*
import java.io.File

private enum class GalleryViewMode {
    /** iOS `byDate` — 일 단위 섹션 */
    BY_DATE,
    /** iOS `byKind` — 2/4/6컷 */
    BY_KIND,
}

private enum class GallerySection { COMPLETED, DRAFTS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onOpen: (resultPath: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    onOpenResult: ((sessionId: String, resultId: String, resultPath: String) -> Unit)? = null,
    onResumeDraft: (sessionId: String, stage: SessionStage) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()
    var viewMode by remember { mutableStateOf(GalleryViewMode.BY_DATE) }
    var section by remember { mutableStateOf(GallerySection.COMPLETED) }
    var longPressedItemId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<GalleryItem?>(null) }
    var discardTarget by remember { mutableStateOf<GalleryDraftItem?>(null) }

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
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.Screen.horizontal),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                listOf(
                    GallerySection.COMPLETED to "완성 ${uiState.items.size}",
                    GallerySection.DRAFTS to "진행 중 ${uiState.drafts.size}",
                ).forEach { (choice, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(AppLayout.Radius.sm))
                            .background(if (section == choice) AppColors.Accent.pink else AppColors.Background.secondary)
                            .clickable { section = choice; longPressedItemId = null },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = if (section == choice) Color.White else AppColors.Text.primary,
                            style = AppTypography.caption1)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = AppSpacing.md),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (section == GallerySection.COMPLETED) GalleryViewModeToggle(
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

                uiState.errorMessage != null -> {
                    Column(Modifier.fillMaxSize().padding(AppSpacing.Screen.horizontal),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage.orEmpty(), color = AppColors.Semantic.error,
                            style = AppTypography.body, textAlign = TextAlign.Center)
                        SecondaryButton(text = "다시 불러오기", onClick = viewModel::load, fullWidth = true)
                    }
                }

                section == GallerySection.DRAFTS -> {
                    DraftContent(
                        drafts = uiState.drafts,
                        onResume = { item ->
                            if (item.stage == SessionStage.NEEDS_RECOVERY) viewModel.reportRecoveryRequired()
                            else onResumeDraft(item.sessionId, item.stage)
                        },
                        onDiscard = { discardTarget = it },
                    )
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
                            } else if (!item.missingImage) {
                                if (onOpenResult != null) {
                                    onOpenResult(item.sessionId, item.resultId, item.resultPath)
                                } else {
                                    onOpen(item.resultPath)
                                }
                            } else {
                                viewModel.reportMissingImage()
                            }
                        },
                        onItemLongPress = { item ->
                            longPressedItemId = "${item.sessionId}:${item.resultId}"
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
            visible = deleteTarget != null || discardTarget != null,
            title = if (discardTarget != null) "이 작업을 버릴까요?" else "이 세션을 지울까요?",
            message = if (discardTarget != null) {
                "미완성 작업을 삭제합니다. 이전에 완성한 결과와 사진첩 사본은 유지됩니다."
            } else {
                "이 세션의 앱 내부 결과와 촬영 사진을 모두 삭제합니다. 사진첩에 저장한 사본은 유지됩니다."
            },
            confirmText = "삭제",
            cancelText = "취소",
            isDestructive = true,
            onConfirm = {
                deleteTarget?.let { viewModel.delete(it.sessionId) }
                discardTarget?.let { viewModel.discardDraft(it.sessionId) }
                deleteTarget = null
                discardTarget = null
            },
            onCancel = { deleteTarget = null; discardTarget = null },
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
                        key = { "${it.sessionId}:${it.resultId}" },
                    ) { item ->
                        SessionCell(
                            item = item,
                            isLongPressed = longPressedItemId == "${item.sessionId}:${item.resultId}",
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
                        key = { "${it.sessionId}:${it.resultId}" },
                    ) { item ->
                        SessionCell(
                            item = item,
                            isLongPressed = longPressedItemId == "${item.sessionId}:${item.resultId}",
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
                model = if (item.missingImage) null else File(item.resultPath),
                contentDescription = "${item.frameKindLabel} 결과, ${item.daySectionTitle}" +
                    if (item.missingImage) ", 이미지 파일 없음" else "",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (item.missingImage) {
                Text("이미지 파일 없음", color = AppColors.Semantic.error,
                    style = AppTypography.caption1,
                    modifier = Modifier.align(Alignment.Center))
            }
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
                        .size(48.dp)
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

@Composable
private fun DraftContent(
    drafts: List<GalleryDraftItem>,
    onResume: (GalleryDraftItem) -> Unit,
    onDiscard: (GalleryDraftItem) -> Unit,
) {
    if (drafts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("진행 중인 작업이 없어요", style = AppTypography.body,
                color = AppColors.Text.secondary)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = AppSpacing.Screen.horizontal),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Component.cardGap),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Component.cardGap),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl),
    ) {
        items(drafts, key = { it.sessionId }) { draft ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppLayout.Radius.xs))
                    .background(AppColors.Background.secondary),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clickable { onResume(draft) },
                    contentAlignment = Alignment.Center,
                ) {
                    val thumbnail = draft.thumbnailPath?.takeIf { File(it).isFile }
                    if (thumbnail != null) {
                        AsyncImage(
                            model = File(thumbnail),
                            contentDescription = "진행 중인 ${draft.selectedCount}컷 작업",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null,
                            tint = AppColors.Text.tertiary, modifier = Modifier.size(52.dp))
                    }
                    Text(
                        text = if (draft.stage == SessionStage.NEEDS_RECOVERY) "복구 필요" else "이어서 작업",
                        color = Color.White,
                        style = AppTypography.caption1,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(AppSpacing.xs)
                            .clip(RoundedCornerShape(AppLayout.Radius.xs))
                            .background(AppColors.Accent.pink)
                            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
                    )
                }
                Text(
                    text = if (draft.totalShots > 0) {
                        "${draft.selectedCount}컷 · 촬영 ${draft.completedShots}/${draft.totalShots}"
                    } else "${draft.selectedCount}컷 · 복구 필요",
                    color = AppColors.Text.primary,
                    style = AppTypography.caption1,
                    modifier = Modifier.padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onDiscard(draft) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("작업 버리기", color = AppColors.Semantic.error, style = AppTypography.caption1)
                }
            }
        }
    }
}
