package com.pocket4cut.presentation.detailEdit

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.domain.model.PhotoCrop
import com.pocket4cut.frame.CropMath
import com.pocket4cut.frame.CropRect
import com.pocket4cut.frame.NormalizedPoint
import com.pocket4cut.frame.PhotoCropTransform
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import kotlin.math.min
import kotlin.math.roundToInt

/** Full-screen, slot-aware non-destructive crop editor. */
@Composable
internal fun CropEditorScreen(
    bitmap: Bitmap,
    initialCrop: PhotoCrop,
    quarterTurnsClockwise: Int,
    flipHorizontal: Boolean,
    slotAspectRatio: Float,
    sourceDimensions: BitmapDecoding.ImageDimensions?,
    onPreview: (PhotoCrop) -> Unit,
    onCommit: (PhotoCrop) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var crop by remember(bitmap, quarterTurnsClockwise, flipHorizontal) { mutableStateOf(initialCrop) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val latestCrop by rememberUpdatedState(crop)
    val latestOnPreview by rememberUpdatedState(onPreview)
    val latestOnCommit by rememberUpdatedState(onCommit)

    fun clamp(candidate: PhotoCrop): PhotoCrop {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) {
            return candidate.copy(
                focusX = candidate.focusX.coerceIn(0f, 1f),
                focusY = candidate.focusY.coerceIn(0f, 1f),
                zoom = candidate.zoom.coerceIn(CropMath.MIN_ZOOM, CropMath.MAX_ZOOM),
            )
        }
        return CropMath.clampCrop(
            crop = candidate,
            imageWidth = bitmap.width.toFloat(),
            imageHeight = bitmap.height.toFloat(),
            viewportWidth = viewportSize.width.toFloat(),
            viewportHeight = viewportSize.height.toFloat(),
            quarterTurnsClockwise = quarterTurnsClockwise,
            flipHorizontal = flipHorizontal,
        )
    }

    fun update(candidate: PhotoCrop, commit: Boolean) {
        val next = clamp(candidate)
        crop = next
        latestOnPreview(next)
        if (commit) latestOnCommit(next)
    }

    fun close() {
        latestOnCommit(crop)
        onDismiss()
    }

    fun moveFocus(dx: Float, dy: Float) {
        val displayed = CropMath.displayFocus(crop, quarterTurnsClockwise, flipHorizontal)
        val stored = CropMath.inverseDisplayFocus(
            NormalizedPoint(displayed.x + dx / crop.zoom, displayed.y + dy / crop.zoom),
            quarterTurnsClockwise,
            flipHorizontal,
        )
        update(crop.copy(focusX = stored.x, focusY = stored.y), commit = true)
    }

    BackHandler(onBack = ::close)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary)
            .padding(top = AppSpacing.xxl),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.Screen.horizontal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = ::close, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, contentDescription = "자르기 닫기")
            }
            Text(
                text = "사진 자르기",
                style = AppTypography.title2,
                color = AppColors.Text.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${crop.zoom.formatZoom()}×",
                style = AppTypography.callout,
                color = AppColors.Text.secondary,
            )
        }

        Text(
            text = "사진을 움직이거나 두 손가락으로 확대하세요.",
            style = AppTypography.footnote,
            color = AppColors.Text.secondary,
            modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal),
        )
        Spacer(Modifier.height(AppSpacing.md))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center,
        ) {
            val ratio = slotAspectRatio.takeIf { it.isFinite() && it > 0f } ?: (3f / 4f)
            val heightFirst = maxHeight * ratio <= maxWidth
            val viewportModifier = if (heightFirst) {
                Modifier.height(maxHeight).aspectRatio(ratio)
            } else {
                Modifier.fillMaxWidth().aspectRatio(ratio)
            }
            Canvas(
                modifier = viewportModifier
                    .clip(RoundedCornerShape(AppLayout.Radius.xs))
                    .background(Color.Black)
                    .border(2.dp, Color.White, RoundedCornerShape(AppLayout.Radius.xs))
                    .onSizeChanged { viewportSize = it }
                    .semantics {
                        contentDescription = "사진 자르기 미리보기"
                        stateDescription = crop.accessibilityState(
                            quarterTurnsClockwise,
                            flipHorizontal,
                        )
                    }
                    .pointerInput(bitmap, ratio, quarterTurnsClockwise, flipHorizontal) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var gestureCrop = latestCrop
                            do {
                                val event = awaitPointerEvent()
                                val pan = event.calculatePan()
                                val zoomChange = event.calculateZoom()
                                if (pan != Offset.Zero || zoomChange != 1f) {
                                    val current = gestureCrop
                                    val newZoom = (current.zoom * zoomChange)
                                        .coerceIn(CropMath.MIN_ZOOM, CropMath.MAX_ZOOM)
                                    val displayed = CropMath.displayFocus(
                                        current,
                                        quarterTurnsClockwise,
                                        flipHorizontal,
                                    )
                                    val baseScale = CropMath.aspectFillScale(
                                        bitmap.width.toFloat(),
                                        bitmap.height.toFloat(),
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                    )
                                    val stored = CropMath.inverseDisplayFocus(
                                        NormalizedPoint(
                                            displayed.x - pan.x / (bitmap.width * baseScale * newZoom),
                                            displayed.y - pan.y / (bitmap.height * baseScale * newZoom),
                                        ),
                                        quarterTurnsClockwise,
                                        flipHorizontal,
                                    )
                                    val next = CropMath.clampCrop(
                                        current.copy(
                                            focusX = stored.x,
                                            focusY = stored.y,
                                            zoom = newZoom,
                                        ),
                                        bitmap.width.toFloat(),
                                        bitmap.height.toFloat(),
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                        quarterTurnsClockwise,
                                        flipHorizontal,
                                    )
                                    gestureCrop = next
                                    crop = next
                                    latestOnPreview(next)
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                            latestOnCommit(gestureCrop)
                        }
                    },
            ) {
                val rect = CropMath.drawRect(
                    imageWidth = bitmap.width.toFloat(),
                    imageHeight = bitmap.height.toFloat(),
                    viewport = CropRect(0f, 0f, size.width, size.height),
                    transform = PhotoCropTransform(crop, quarterTurnsClockwise, flipHorizontal),
                )
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
                    dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
                    filterQuality = FilterQuality.High,
                )
                val gridColor = Color.White.copy(alpha = 0.58f)
                for (part in 1..2) {
                    val x = size.width * part / 3f
                    val y = size.height * part / 3f
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                drawRect(Color.White, style = Stroke(width = 2.dp.toPx()))
            }
        }

        val qualityIsLow = remember(sourceDimensions, quarterTurnsClockwise, slotAspectRatio, crop.zoom) {
            sourceDimensions?.let {
                effectiveCropShortestEdge(it, quarterTurnsClockwise, slotAspectRatio, crop.zoom) < 1_024f
            } ?: false
        }
        if (qualityIsLow) {
            Text(
                text = "확대된 영역의 해상도가 낮아 저장 이미지가 흐려질 수 있어요.",
                style = AppTypography.footnote,
                color = AppColors.Semantic.warning,
                modifier = Modifier.padding(
                    start = AppSpacing.Screen.horizontal,
                    end = AppSpacing.Screen.horizontal,
                    top = AppSpacing.sm,
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.Screen.horizontal, vertical = AppSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { update(crop.copy(zoom = (crop.zoom - 0.1f).coerceAtLeast(1f)), true) },
                    modifier = Modifier.size(48.dp),
                ) { Icon(Icons.Default.Remove, contentDescription = "축소") }
                Slider(
                    value = crop.zoom,
                    onValueChange = { update(crop.copy(zoom = it), commit = false) },
                    onValueChangeFinished = { latestOnCommit(crop) },
                    valueRange = CropMath.MIN_ZOOM..CropMath.MAX_ZOOM,
                    colors = SliderDefaults.colors(
                        thumbColor = AppColors.Accent.pink,
                        activeTrackColor = AppColors.Accent.pink,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            stateDescription = crop.accessibilityState(
                                quarterTurnsClockwise,
                                flipHorizontal,
                            )
                        },
                )
                IconButton(
                    onClick = { update(crop.copy(zoom = (crop.zoom + 0.1f).coerceAtMost(4f)), true) },
                    modifier = Modifier.size(48.dp),
                ) { Icon(Icons.Default.Add, contentDescription = "확대") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CropMoveButton("초점을 왼쪽으로 이동", Icons.AutoMirrored.Filled.ArrowBack) { moveFocus(-0.04f, 0f) }
                CropMoveButton("초점을 위로 이동", Icons.Default.ArrowUpward) { moveFocus(0f, -0.04f) }
                CropMoveButton("초점을 아래로 이동", Icons.Default.ArrowDownward) { moveFocus(0f, 0.04f) }
                CropMoveButton("초점을 오른쪽으로 이동", Icons.AutoMirrored.Filled.ArrowForward) { moveFocus(0.04f, 0f) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                androidx.compose.material3.TextButton(
                    onClick = { update(PhotoCrop(), commit = true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) { Text("가운데 맞춤") }
                PrimaryButton(
                    text = "완료",
                    onClick = ::close,
                    fullWidth = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CropMoveButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { role = Role.Button },
    ) {
        Icon(icon, contentDescription = label)
    }
}

private fun effectiveCropShortestEdge(
    source: BitmapDecoding.ImageDimensions,
    quarterTurnsClockwise: Int,
    slotAspectRatio: Float,
    zoom: Float,
): Float {
    val rotated = ((quarterTurnsClockwise % 4) + 4) % 4
    val imageWidth = if (rotated % 2 == 0) source.width.toFloat() else source.height.toFloat()
    val imageHeight = if (rotated % 2 == 0) source.height.toFloat() else source.width.toFloat()
    val viewportWidth = slotAspectRatio.coerceAtLeast(0.01f)
    val viewportHeight = 1f
    val scale = CropMath.aspectFillScale(imageWidth, imageHeight, viewportWidth, viewportHeight) * zoom
    return min(viewportWidth / scale, viewportHeight / scale)
}

private fun Float.formatZoom(): String = String.format(java.util.Locale.US, "%.1f", this)

private fun PhotoCrop.accessibilityState(
    quarterTurnsClockwise: Int,
    flipHorizontal: Boolean,
): String {
    val displayed = CropMath.displayFocus(this, quarterTurnsClockwise, flipHorizontal)
    val horizontal = when {
        displayed.x < 0.4f -> "왼쪽"
        displayed.x > 0.6f -> "오른쪽"
        else -> "가운데"
    }
    val vertical = when {
        displayed.y < 0.4f -> "위"
        displayed.y > 0.6f -> "아래"
        else -> "가운데"
    }
    return "초점 $horizontal $vertical, 확대 ${zoom.formatZoom()}배"
}
