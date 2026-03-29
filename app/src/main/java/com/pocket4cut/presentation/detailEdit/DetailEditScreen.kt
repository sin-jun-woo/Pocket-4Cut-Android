package com.pocket4cut.presentation.detailEdit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*
import kotlinx.coroutines.launch

@Composable
fun DetailEditScreen(
    frameType: FrameType,
    sessionId: String,
    selectedIndexes: List<Int>,
    layoutId: String,
    onBack: () -> Unit,
    onCompleted: (resultPath: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailEditViewModel = viewModel(),
) {
    LaunchedEffect(frameType, sessionId, selectedIndexes, layoutId) {
        viewModel.init(frameType, sessionId, selectedIndexes, layoutId)
    }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(AppColors.Background.primary)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.xxxl, start = AppSpacing.Screen.horizontal, end = AppSpacing.Screen.horizontal, bottom = AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(Icons.Default.Close, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                }
                Text("상세 편집", style = AppTypography.title2, color = AppColors.Text.primary)
                IconCircleButton(
                    onClick = { viewModel.resetCurrent() },
                    variant = IconButtonVariant.SOLID,
                    enabled = uiState.hasChanges,
                ) {
                    Icon(Icons.Default.Refresh, null, tint = AppColors.Text.secondary, modifier = Modifier.size(20.dp))
                }
            }

            // Large preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = AppSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(AppLayout.Radius.xl))
                        .background(AppColors.Background.secondary),
                ) {
                    if (uiState.preview != null) {
                        androidx.compose.foundation.Image(
                            bitmap = uiState.preview!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else if (uiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(color = AppColors.Accent.pink)
                        }
                    }
                }
            }

            // Thumbnail selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Background.secondary)
                    .padding(vertical = AppSpacing.md, horizontal = AppSpacing.Screen.horizontal)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                uiState.orderedPaths.forEachIndexed { i, path ->
                    val isSelected = uiState.selectedSlot == i
                    val adj = uiState.slotAdjusts.getOrNull(i)
                    val hasEdits = adj != null && (adj.brightness != 0f || adj.contrast != 1f || adj.saturation != 1f || adj.rotationQuarters != 0)
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(96.dp)
                            .clip(RoundedCornerShape(AppLayout.Radius.md))
                            .border(3.dp, if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.md))
                            .clickable { viewModel.selectSlot(i) },
                    ) {
                        AsyncImage(model = path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        if (hasEdits) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(AppSpacing.xxs)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Accent.pink),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Check, null, tint = AppColors.Text.primary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary)
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(bottom = AppSpacing.Layout.ctaBottomSpace),
        ) {
            // Rotate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppLayout.Radius.md))
                    .background(AppColors.Background.tertiary)
                    .border(1.dp, AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.md))
                    .clickable { viewModel.rotateQuarter() }
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Refresh, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(AppSpacing.sm))
                Text("90° 회전", style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Text.primary)
            }
            Spacer(Modifier.height(AppSpacing.lg))

            val adj = uiState.slotAdjusts.getOrNull(uiState.selectedSlot)
            if (adj != null) {
                SliderRow("밝기", adj.brightness, -0.5f..0.5f) { viewModel.setBrightness(it) }
                SliderRow("대비", adj.contrast - 1f, -0.5f..0.5f) { viewModel.setContrast(it + 1f) }
                SliderRow("채도", adj.saturation - 1f, -1f..1f) { viewModel.setSaturation(it + 1f) }
            }

            PrimaryButton(
                text = if (isSaving) "생성 중..." else "적용",
                onClick = {
                    if (isSaving) return@PrimaryButton
                    isSaving = true
                    scope.launch {
                        runCatching { viewModel.applyAndFinish(sessionId) }
                            .onSuccess { path -> onCompleted(path) }
                        isSaving = false
                    }
                },
                enabled = !isSaving,
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Text.secondary, modifier = Modifier.width(40.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.Accent.pink,
                activeTrackColor = AppColors.Accent.pink,
                inactiveTrackColor = AppColors.Background.secondary,
            ),
        )
        Text(
            "${(value * 100).toInt()}%",
            style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.Accent.pink,
            modifier = Modifier.width(50.dp),
        )
    }
}
