package com.pocket4cut.presentation.selection

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton

@Composable
fun SelectionScreen(
    frameType: FrameType,
    sessionId: String,
    onBack: () -> Unit,
    onDone: (selectedIndexes: List<Int>) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SelectionViewModel = viewModel(),
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val uiState by viewModel.uiState.collectAsState()
    val max = frameType.selectCount
    val isDone = uiState.selectedIndexes.size == max
    val remaining = max - uiState.selectedIndexes.size

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
                Text("사진 선택", style = AppTypography.title2, color = AppColors.Text.primary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppLayout.Radius.md))
                        .background(if (isDone) AppColors.Accent.pinkSubtle else AppColors.Background.tertiary)
                        .border(1.dp, if (isDone) AppColors.Accent.pink else AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.md))
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                ) {
                    Text(
                        "${uiState.selectedIndexes.size} / $max 장",
                        style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDone) AppColors.Accent.pink else AppColors.Text.primary,
                    )
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = AppColors.Accent.pink)
                }
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.errorMessage ?: "", style = AppTypography.body, color = AppColors.Text.secondary)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AppSpacing.Screen.horizontal),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    contentPadding = PaddingValues(bottom = 160.dp),
                ) {
                    itemsIndexed(uiState.imagePaths) { index, path ->
                        val order = uiState.selectedIndexes.indexOf(index)
                        val isSelected = order >= 0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(AppLayout.Radius.lg))
                                .background(AppColors.Background.secondary)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, AppColors.Accent.pink, RoundedCornerShape(AppLayout.Radius.lg))
                                    else Modifier.border(1.dp, AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.lg))
                                )
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
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(AppSpacing.xs)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.Accent.pink),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${order + 1}", style = AppTypography.caption1.copy(fontWeight = FontWeight.Bold), color = AppColors.Text.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary)
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(bottom = AppSpacing.Layout.ctaBottomSpace, top = AppSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = AppSpacing.sm),
            ) {
                Icon(Icons.Default.Info, null, tint = AppColors.Text.tertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(AppSpacing.xxs))
                Text("순서대로 배치됩니다", style = AppTypography.caption1, color = AppColors.Text.tertiary)
            }
            PrimaryButton(
                text = if (isDone) "다음" else "${remaining}장 더 선택해주세요",
                onClick = { if (isDone) onDone(uiState.selectedIndexes) },
                enabled = isDone,
                fullWidth = true,
            )
        }
    }
}
