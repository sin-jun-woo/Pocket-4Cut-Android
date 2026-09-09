package com.pocket4cut.presentation.detailEdit

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket4cut.frame.CollagePreviewScaledToFit
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameColor
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.LoadingOverlay
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/* ── UI ↔ Model conversion (iOS-exact ranges) ──────────────────── */

private fun brightnessModelToUi(model: Float): Float = (model / 0.35f) * 50f
private fun brightnessUiToModel(ui: Float): Float = (ui / 50f) * 0.35f

private fun contrastModelToUi(model: Float): Float = ((model - 1f) / 0.5f) * 50f
private fun contrastUiToModel(ui: Float): Float = 1f + (ui / 50f) * 0.5f

private fun saturationModelToUi(model: Float): Float = (model - 1f) * 50f
private fun saturationUiToModel(ui: Float): Float = 1f + (ui / 50f)

private fun formatSliderValue(v: Float): String {
    val i = v.roundToInt()
    return if (i > 0) "+$i" else "$i"
}

/* ── Screen ─────────────────────────────────────────────────────── */

@Composable
fun DetailEditScreen(
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    frameColor: FrameColor,
    orderedImages: List<Bitmap>,
    imagePaths: List<String>,
    sessionId: String,
    selectedIndexes: List<Int>,
    globalFilter: FilterId,
    customText: String,
    showDate: Boolean,
    textFontSize: Float = 16f,
    dateFontSize: Float = 16f,
    captionFontName: String? = null,
    captionColorRGB: Long? = null,
    customFrameDesign: com.pocket4cut.frame.CustomFrameDesign? = null,
    onBack: () -> Unit,
    onResult: (resultPath: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailEditViewModel = viewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.initialize(
            baseImages = orderedImages,
            imagePaths = imagePaths,
            frameType = frameType,
            frameStyle = frameStyle,
            theme = theme,
            frameColor = frameColor,
            globalFilter = globalFilter,
            customText = customText,
            showDate = showDate,
            sessionId = sessionId,
            selectedIndexes = selectedIndexes,
            textFontSize = textFontSize,
            dateFontSize = dateFontSize,
            captionFontName = captionFontName,
            captionColorRGB = captionColorRGB,
            customFrameDesign = customFrameDesign,
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val dateString = remember {
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
    }
    val overlayText = remember(customText, showDate, dateString) {
        buildList {
            if (customText.isNotBlank()) add(customText)
            if (showDate) add(dateString)
        }.joinToString(" · ")
    }
    val bottomCaptionForLayout = remember(customText, showDate, dateString) {
        buildList {
            if (customText.isNotBlank()) add(customText.trim())
            if (showDate) add(dateString)
        }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
    val captionTextPart = remember(customText) { customText.trim().takeIf { it.isNotEmpty() } }
    val captionDatePart = remember(showDate, dateString) { if (showDate) dateString else null }

    Box(modifier = modifier.fillMaxSize().background(AppColors.Background.primary)) {
        Column(modifier = Modifier.fillMaxSize()) {

            /* ── Header ─────────────────────────────────────────── */
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
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(Icons.Default.Close, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                }
                Text("상세 편집", style = AppTypography.title2, color = AppColors.Text.primary)
                IconCircleButton(
                    onClick = { viewModel.resetCurrentSlot() },
                    variant = IconButtonVariant.SOLID,
                    enabled = uiState.hasChanges,
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        null,
                        tint = if (uiState.hasChanges) AppColors.Text.primary else AppColors.Text.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            /* ── Preview + Thumbnails (scrollable middle) ───────── */
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(AppSpacing.sm))

                if (uiState.collagePreviewImages.isNotEmpty()) {
                    val previewBackground = customFrameDesign?.resolvedFillColor ?: frameColor.color
                    CollagePreviewScaledToFit(
                        images = uiState.collagePreviewImages,
                        frameType = frameType,
                        frameStyle = frameStyle,
                        theme = theme,
                        overrideBackground = previewBackground,
                        customFrameDesign = customFrameDesign,
                        customDecorations = customFrameDesign?.decorations ?: emptyList(),
                        bottomCaption = bottomCaptionForLayout,
                        captionTextPart = captionTextPart,
                        captionDatePart = captionDatePart,
                        captionTextSizePt = textFontSize,
                        captionDateSizePt = dateFontSize,
                        captionFontName = captionFontName,
                        captionColorRGB = captionColorRGB,
                        modifier = Modifier
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(AppLayout.Radius.xs),
                                clip = false,
                                ambientColor = AppColors.Shadow.color,
                                spotColor = AppColors.Shadow.colorMd,
                            )
                            .widthIn(max = 360.dp)
                            .padding(horizontal = AppSpacing.Screen.horizontal),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(AppLayout.Radius.xs),
                                clip = false,
                                ambientColor = AppColors.Shadow.color,
                                spotColor = AppColors.Shadow.colorMd,
                            )
                            .widthIn(max = 360.dp)
                            .padding(horizontal = AppSpacing.Screen.horizontal)
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(AppLayout.Radius.xs))
                            .background(AppColors.Background.secondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = AppColors.Accent.pink)
                    }
                }

                if (overlayText.isNotBlank()) {
                    Text(
                        text = overlayText,
                        style = AppTypography.footnote,
                        color = AppColors.Text.secondary,
                        modifier = Modifier.padding(top = AppSpacing.sm),
                    )
                }

                Spacer(Modifier.height(AppSpacing.md))

                /* ── Thumbnail strip ────────────────────────────── */
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = AppColors.Border.light,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Background.secondary)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = AppSpacing.Screen.horizontal, vertical = AppSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    orderedImages.forEachIndexed { i, bitmap ->
                        val isSelected = i == uiState.selectedSlotIndex
                        val adj = uiState.slotAdjustments.getOrNull(i)
                        val hasEdits = adj != null && !adj.isNeutral

                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(96.dp)
                                .clip(RoundedCornerShape(AppLayout.Radius.xs))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle,
                                    shape = RoundedCornerShape(AppLayout.Radius.xs),
                                )
                                .clickable { viewModel.selectSlot(i) },
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            if (hasEdits) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(AppLayout.Radius.xs))
                                        .background(AppColors.Accent.pink),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = AppColors.Border.light,
                )

                Spacer(Modifier.height(AppSpacing.sm))
            }

            /* ── Bottom fixed controls ──────────────────────────── */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Background.primary)
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = AppSpacing.Layout.ctaBottomSpace),
            ) {
                val currentAdj = uiState.slotAdjustments.getOrNull(uiState.selectedSlotIndex)
                val isFlipped = currentAdj?.isFlippedHorizontally == true
                val flipTint = if (isFlipped) AppColors.Accent.pink else AppColors.Text.primary
                val flipBorder = if (isFlipped) AppColors.Accent.pink.copy(alpha = 0.6f) else AppColors.Border.subtle

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(AppLayout.Radius.md))
                            .background(AppColors.Background.tertiary)
                            .border(1.dp, AppColors.Border.subtle, RoundedCornerShape(AppLayout.Radius.md))
                            .clickable { viewModel.rotateCurrentSlot() }
                            .padding(vertical = AppSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = AppColors.Text.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.height(AppSpacing.xxs))
                        Text(
                            "90° 회전",
                            style = AppTypography.caption1.copy(fontWeight = FontWeight.SemiBold),
                            color = AppColors.Text.primary,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(AppLayout.Radius.md))
                            .background(AppColors.Background.tertiary)
                            .border(1.dp, flipBorder, RoundedCornerShape(AppLayout.Radius.md))
                            .clickable { viewModel.flipCurrentHorizontally() }
                            .padding(vertical = AppSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.Flip, null, tint = flipTint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.height(AppSpacing.xxs))
                        Text(
                            "좌우 반전",
                            style = AppTypography.caption1.copy(fontWeight = FontWeight.SemiBold),
                            color = flipTint,
                        )
                    }
                }

                Spacer(Modifier.height(AppSpacing.lg))

                val adj = uiState.slotAdjustments.getOrNull(uiState.selectedSlotIndex)
                    ?: PhotoSlotAdjustment.neutral

                AdjustmentSliderRow(
                    label = "밝기",
                    uiValue = brightnessModelToUi(adj.brightness),
                    onUiValueChange = { viewModel.setBrightness(brightnessUiToModel(it)) },
                )
                AdjustmentSliderRow(
                    label = "대비",
                    uiValue = contrastModelToUi(adj.contrast),
                    onUiValueChange = { viewModel.setContrast(contrastUiToModel(it)) },
                )
                AdjustmentSliderRow(
                    label = "채도",
                    uiValue = saturationModelToUi(adj.saturation),
                    onUiValueChange = { viewModel.setSaturation(saturationUiToModel(it)) },
                )

                Spacer(Modifier.height(AppSpacing.md))

                PrimaryButton(
                    text = "적용",
                    onClick = {
                        if (uiState.isRendering) return@PrimaryButton
                        scope.launch {
                            runCatching { viewModel.renderFinalCollage() }
                                .onSuccess { path -> onResult(path) }
                        }
                    },
                    enabled = !uiState.isRendering,
                    fullWidth = true,
                )
            }
        }

        LoadingOverlay(visible = uiState.isRendering)
    }
}

/* ── Adjustment slider row ──────────────────────────────────────── */

@Composable
private fun AdjustmentSliderRow(
    label: String,
    uiValue: Float,
    onUiValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Text.secondary,
            )
            Text(
                formatSliderValue(uiValue),
                style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Accent.pink,
            )
        }
        Spacer(Modifier.height(AppSpacing.xs))
        PinkGradientSlider(value = uiValue, onValueChange = onUiValueChange)
    }
}

/* ── PinkGradientSlider (pinkDark → pink → pinkLight) ───────────── */

@Composable
private fun PinkGradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbSizeDp = 28.dp
    val trackHeightDp = 6.dp
    val fraction = ((value + 50f) / 100f).coerceIn(0f, 1f)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Canvas(
        modifier = modifier
            .height(thumbSizeDp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                val thumbPx = thumbSizeDp.toPx()
                val halfThumb = thumbPx / 2f
                val trackW = size.width - thumbPx

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val f = ((down.position.x - halfThumb) / trackW).coerceIn(0f, 1f)
                    currentOnValueChange(-50f + f * 100f)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        val newF = ((change.position.x - halfThumb) / trackW).coerceIn(0f, 1f)
                        currentOnValueChange(-50f + newF * 100f)
                    }
                }
            },
    ) {
        val thumbPx = thumbSizeDp.toPx()
        val halfThumb = thumbPx / 2f
        val trackH = trackHeightDp.toPx()
        val trackW = size.width - thumbPx
        val trackTop = (size.height - trackH) / 2f

        // Gradient track
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    AppColors.Accent.pinkDark,
                    AppColors.Accent.pink,
                    AppColors.Accent.pinkLight,
                ),
                startX = halfThumb,
                endX = halfThumb + trackW,
            ),
            topLeft = Offset(halfThumb, trackTop),
            size = Size(trackW, trackH),
            cornerRadius = CornerRadius(trackH / 2f),
        )

        // Thumb
        val cx = halfThumb + fraction * trackW
        val cy = size.height / 2f

        // Shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = halfThumb + 1.dp.toPx(),
            center = Offset(cx, cy + 1.dp.toPx()),
        )
        // Fill
        drawCircle(
            color = Color.White,
            radius = halfThumb,
            center = Offset(cx, cy),
        )
        // Border
        drawCircle(
            color = Color.Black.copy(alpha = 0.08f),
            radius = halfThumb,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
