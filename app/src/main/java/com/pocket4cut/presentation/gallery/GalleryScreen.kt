package com.pocket4cut.presentation.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onOpen: (resultPath: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<GalleryItem?>(null) }
    var viewMode by remember { mutableStateOf("date") }

    LaunchedEffect(Unit) { viewModel.load() }

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
                    .padding(top = AppSpacing.xxxl, start = AppSpacing.Screen.horizontal, end = AppSpacing.Screen.horizontal, bottom = AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(Icons.Default.Close, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(AppSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text("보관함", style = AppTypography.title2, color = AppColors.Text.primary)
                    Text("${uiState.items.size}개의 추억", style = AppTypography.caption1, color = AppColors.Text.tertiary)
                }
                if (uiState.items.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppLayout.Radius.full))
                            .background(AppColors.Background.tertiary)
                            .border(1.dp, AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.full))
                            .padding(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (viewMode == "date") AppColors.Accent.pink else Color.Transparent)
                                .clickable { viewMode = "date" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.DateRange, null, tint = if (viewMode == "date") AppColors.Text.primary else AppColors.Text.tertiary, modifier = Modifier.size(20.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (viewMode == "grid") AppColors.Accent.pink else Color.Transparent)
                                .clickable { viewMode = "grid" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Menu, null, tint = if (viewMode == "grid") AppColors.Text.primary else AppColors.Text.tertiary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = AppColors.Accent.pink)
                }
                uiState.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = { Icon(Icons.Default.Image, null, tint = AppColors.Text.tertiary, modifier = Modifier.size(48.dp)) },
                        title = "아직 추억이 없어요",
                        description = "첫 촬영을 시작해보세요!",
                        action = {
                            SecondaryButton(text = "홈으로 가기", onClick = onBack, fullWidth = true)
                        },
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppSpacing.Screen.horizontal),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        contentPadding = PaddingValues(bottom = AppSpacing.xxxl),
                    ) {
                        items(uiState.items) { item ->
                            val file = File(item.resultPath)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(AppLayout.Radius.lg))
                                    .background(AppColors.Background.secondary)
                                    .border(1.dp, AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.lg))
                                    .combinedClickable(
                                        onClick = { onOpen(item.resultPath) },
                                        onLongClick = { deleteTarget = item },
                                    ),
                            ) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(AppSpacing.xs)
                                        .clip(RoundedCornerShape(AppLayout.Radius.sm))
                                        .background(AppColors.Overlay.medium)
                                        .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs),
                                ) {
                                    Text(
                                        item.dateLabel,
                                        style = AppTypography.caption2.copy(fontWeight = FontWeight.SemiBold),
                                        color = AppColors.Text.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ConfirmDialog(
            visible = deleteTarget != null,
            title = "삭제하시겠어요?",
            message = "이 결과물을 삭제합니다.",
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
