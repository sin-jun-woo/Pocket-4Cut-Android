package com.pocket4cut.presentation.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.ConfirmDialog
import com.pocket4cut.ui.designsystem.components.ConfirmDialogVariant
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.IconCircleButtonSize
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import com.pocket4cut.ui.designsystem.components.toIconDp

@Composable
fun SelectionScreen(
    frameType: FrameType,
    sessionId: String,
    onBack: () -> Unit,
    onDone: (selectedIndexes: List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelectionViewModel = viewModel(),
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val uiState by viewModel.uiState.collectAsState()
    val max = frameType.selectCount
    val selectedCount = uiState.selectedIndexes.size
    val isDone = selectedCount == max
    val remaining = max - selectedCount

    var showExitConfirm by remember { mutableStateOf(false) }

    ConfirmDialog(
        visible = showExitConfirm,
        title = "선택을 취소하시겠어요?",
        message = "지금까지 촬영한 사진이 모두 삭제됩니다.",
        confirmText = "나가기",
        cancelText = "취소",
        onConfirm = {
            showExitConfirm = false
            onBack()
        },
        onCancel = { showExitConfirm = false },
        variant = ConfirmDialogVariant.Destructive,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: close (left) + centered title + balance spacer (right)
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
                    IconCircleButton(
                        onClick = { showExitConfirm = true },
                        variant = IconButtonVariant.SOLID,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = AppColors.Text.primary,
                            modifier = Modifier.size(IconCircleButtonSize.MD.toIconDp()),
                        )
                    }
                }
                Text(
                    text = "사진 선택",
                    style = AppTypography.title2,
                    color = AppColors.Text.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(AppLayout.Height.IconButton.md))
            }

            // Selection counter card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .clip(RoundedCornerShape(AppLayout.Radius.md))
                    .then(
                        if (isDone) {
                            Modifier
                                .background(AppColors.Accent.pinkSubtle)
                                .border(1.dp, AppColors.Accent.pink, RoundedCornerShape(AppLayout.Radius.md))
                        } else {
                            Modifier
                                .background(AppColors.Background.secondary)
                                .border(1.dp, AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.md))
                        },
                    )
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.Accent.pink,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(
                        text = "선택 완료!",
                        style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.Accent.pink,
                    )
                } else {
                    Text(
                        text = "$selectedCount",
                        style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.Accent.pink,
                    )
                    Text(
                        text = " / $max 장",
                        style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.Text.secondary,
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.md))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = AppColors.Accent.pink)
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            style = AppTypography.body,
                            color = AppColors.Text.secondary,
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.Screen.horizontal),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        contentPadding = PaddingValues(bottom = 180.dp),
                    ) {
                        itemsIndexed(uiState.imagePaths) { index, path ->
                            val order = uiState.selectedIndexes.indexOf(index)
                            val isSelected = order >= 0
                            val cellShape = RoundedCornerShape(AppLayout.Radius.lg)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .shadow(
                                                    elevation = 12.dp,
                                                    shape = cellShape,
                                                    clip = false,
                                                    ambientColor = AppColors.Accent.pink.copy(alpha = 0.45f),
                                                    spotColor = AppColors.Accent.pink.copy(alpha = 0.55f),
                                                )
                                                .clip(cellShape)
                                                .border(3.dp, AppColors.Accent.pink, cellShape)
                                        } else {
                                            Modifier
                                                .clip(cellShape)
                                                .border(1.dp, AppColors.Border.subtle, cellShape)
                                        },
                                    )
                                    .background(AppColors.Background.secondary)
                                    .clickable { viewModel.toggle(index, max) },
                            ) {
                                AsyncImage(
                                    model = path,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = if (isSelected) 1f else 0.5f,
                                )
                                if (!isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(AppSpacing.xs),
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(AppColors.Accent.pink),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "${order + 1}",
                                                style = AppTypography.caption1.copy(fontWeight = FontWeight.Bold),
                                                color = AppColors.Text.primary,
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = AppTypography.caption1.copy(fontWeight = FontWeight.Bold),
                                                color = AppColors.Text.secondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Border.subtle),
            )
            Spacer(Modifier.height(AppSpacing.md))
            PrimaryButton(
                text = if (isDone) "다음" else "${remaining}장 더 선택해주세요",
                onClick = { if (isDone) onDone(uiState.selectedIndexes) },
                enabled = isDone,
                fullWidth = true,
            )
            if (!isDone) {
                Spacer(Modifier.height(AppSpacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AppColors.Text.tertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(AppSpacing.xxs))
                    Text(
                        text = "순서대로 배치됩니다",
                        style = AppTypography.caption1,
                        color = AppColors.Text.tertiary,
                    )
                }
            }
        }
    }
}
