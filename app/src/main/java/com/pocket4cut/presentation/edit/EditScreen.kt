package com.pocket4cut.presentation.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket4cut.frame.*
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*
import kotlinx.coroutines.launch

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
    viewModel: EditViewModel = viewModel(),
) {
    val frameLayoutId = remember(layoutId) {
        runCatching { FrameLayoutId.valueOf(layoutId) }.getOrElse { FrameLayoutId.FOUR_VERTICAL }
    }

    LaunchedEffect(frameType, sessionId, selectedIndexes, layoutId) {
        viewModel.init(frameType, sessionId, selectedIndexes, frameLayoutId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var isFinalizing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize().background(AppColors.Background.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
        ) {
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
                Text("편집", style = AppTypography.title2, color = AppColors.Text.primary)
                Spacer(Modifier.size(44.dp))
            }

            // Preview placeholder
            Box(
                modifier = Modifier
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(AppLayout.Radius.xl))
                    .background(uiState.selectedFrameColor.color),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(color = AppColors.Accent.pink)
                } else if (uiState.preview != null) {
                    androidx.compose.foundation.Image(
                        bitmap = uiState.preview!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.xl))

            // Filters
            Column(modifier = Modifier.padding(start = AppSpacing.Screen.horizontal)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = AppSpacing.Screen.horizontal),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("필터", style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Text.secondary)
                    Text(uiState.selectedFilter.displayName, style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Accent.pink)
                }
                Spacer(Modifier.height(AppSpacing.md))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    FilterId.entries.forEach { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Background.tertiary)
                                    .then(if (isSelected) Modifier.border(3.dp, AppColors.Accent.pink, CircleShape) else Modifier.border(1.dp, AppColors.Border.subtle, CircleShape))
                                    .clickable { viewModel.setFilter(filter) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(filter.displayName.first().toString(), style = AppTypography.headline, color = if (isSelected) AppColors.Accent.pink else AppColors.Text.secondary)
                            }
                            Spacer(Modifier.height(AppSpacing.xxs))
                            Text(filter.displayName, style = AppTypography.caption1.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal), color = if (isSelected) AppColors.Text.primary else AppColors.Text.secondary)
                        }
                    }
                    Spacer(Modifier.width(AppSpacing.Screen.horizontal))
                }
            }

            Spacer(Modifier.height(AppSpacing.xl))

            // Frame Colors
            Column(modifier = Modifier.padding(start = AppSpacing.Screen.horizontal)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = AppSpacing.Screen.horizontal),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("프레임 색상", style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Text.secondary)
                    Text(uiState.selectedFrameColor.name, style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Accent.pink)
                }
                Spacer(Modifier.height(AppSpacing.md))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    FrameColors.all.forEach { fc ->
                        val isSelected = uiState.selectedFrameColor.id == fc.id
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (fc.gradientBrush != null) Modifier.background(fc.gradientBrush, CircleShape)
                                        else Modifier.background(fc.color, CircleShape)
                                    )
                                    .border(3.dp, if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle, CircleShape)
                                    .clickable { viewModel.setFrameColor(fc) },
                            ) {
                                if (isSelected) {
                                    Box(Modifier.fillMaxSize().background(AppColors.Accent.pink.copy(alpha = 0.25f), CircleShape), contentAlignment = Alignment.Center) {
                                        Box(Modifier.size(20.dp).clip(CircleShape).background(AppColors.Accent.pink), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, null, tint = AppColors.Text.primary, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(AppSpacing.xxs))
                            Text(fc.name, style = AppTypography.caption2.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal), color = if (isSelected) AppColors.Text.primary else AppColors.Text.tertiary)
                        }
                    }
                    Spacer(Modifier.width(AppSpacing.Screen.horizontal))
                }
            }

            Spacer(Modifier.height(AppSpacing.xl))

            // Text Input
            Column(modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Create, null, tint = AppColors.Text.secondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text("텍스트", style = AppTypography.headline.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Text.secondary)
                }
                Spacer(Modifier.height(AppSpacing.md))
                TextField(
                    value = uiState.text,
                    onValueChange = { if (it.length <= 30) viewModel.setText(it) },
                    placeholder = { Text("문구를 입력해주세요", color = AppColors.Text.tertiary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.text.isNotEmpty()) {
                    Text("${uiState.text.length} / 30", style = AppTypography.caption1, color = AppColors.Text.tertiary, modifier = Modifier.align(Alignment.End).padding(top = AppSpacing.xs))
                }
            }

            Spacer(Modifier.height(AppSpacing.xl))

            // Date Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .clip(RoundedCornerShape(AppLayout.Radius.lg))
                    .background(AppColors.Background.tertiary)
                    .border(1.dp, AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.lg))
                    .clickable { viewModel.toggleDate() }
                    .padding(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, tint = AppColors.Text.secondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text("날짜 표시", style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Text.primary)
                    if (uiState.showDate) {
                        Spacer(Modifier.width(AppSpacing.xs))
                        Text(uiState.dateString, style = AppTypography.caption1, color = AppColors.Text.tertiary)
                    }
                }
                // Toggle
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (uiState.showDate) AppColors.Accent.pink else AppColors.Background.secondary),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(28.dp)
                            .offset(x = if (uiState.showDate) 20.dp else 0.dp)
                            .clip(CircleShape)
                            .background(AppColors.Text.primary),
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.xl))

            // Detail edit button
            SecondaryButton(
                text = "사진별 상세 편집",
                onClick = {
                    viewModel.persistPendingForDetailEdit(sessionId)
                    onContinueToDetailEdit()
                },
                fullWidth = true,
                icon = { Icon(Icons.Default.Refresh, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal),
            )
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
            PrimaryButton(
                text = if (isFinalizing) "생성 중..." else "완료",
                onClick = {
                    if (isFinalizing) return@PrimaryButton
                    isFinalizing = true
                    scope.launch {
                        runCatching { viewModel.renderFinalAndSave(sessionId) }
                            .onSuccess { path -> onComplete(path) }
                        isFinalizing = false
                    }
                },
                enabled = !isFinalizing && uiState.errorMessage == null,
                fullWidth = true,
            )
        }
    }
}
