package com.pocket4cut.ui.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import kotlin.math.roundToInt

private val TrackHeight = 4.dp
private val ThumbDp = 24.dp

@Composable
fun PinkGradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -50f..50f,
    label: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thumbRadiusPx = with(density) { (ThumbDp / 2).toPx() }
    val rangeSpan = valueRange.endInclusive - valueRange.start

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Text(
            text = label,
            style = AppTypography.subheadline,
            color = AppColors.Text.primary,
            modifier = Modifier.widthIn(max = 120.dp),
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val trackWidthPx = with(density) { maxWidth.toPx() }
            val thumbTravelPx = (trackWidthPx - 2 * thumbRadiusPx).coerceAtLeast(0f)

            fun fractionFromX(xPx: Float): Float {
                if (thumbTravelPx <= 0f) return 0f
                val clamped = xPx.coerceIn(thumbRadiusPx, trackWidthPx - thumbRadiusPx)
                return ((clamped - thumbRadiusPx) / thumbTravelPx).coerceIn(0f, 1f)
            }

            fun valueFromFraction(f: Float): Float {
                return valueRange.start + f * rangeSpan
            }

            val fraction = if (rangeSpan != 0f) {
                ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
            } else {
                0f
            }
            val thumbCenterX = thumbRadiusPx + fraction * thumbTravelPx
            val thumbOffsetXDp = with(density) { (thumbCenterX - thumbRadiusPx).toDp() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(valueRange, trackWidthPx, thumbRadiusPx) {
                        detectTapGestures { tapOffset ->
                            val f = fractionFromX(tapOffset.x)
                            onValueChange(valueFromFraction(f))
                        }
                    }
                    .pointerInput(valueRange, trackWidthPx, thumbRadiusPx) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val f = fractionFromX(start.x)
                                onValueChange(valueFromFraction(f))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val f = fractionFromX(change.position.x)
                                onValueChange(valueFromFraction(f))
                            },
                        )
                    },
            ) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .height(TrackHeight),
                ) {
                    val h = size.height
                    val w = size.width
                    val r = h / 2f
                    drawRoundRect(
                        color = AppColors.Border.medium,
                        topLeft = Offset.Zero,
                        size = Size(w, h),
                        cornerRadius = CornerRadius(r, r),
                    )
                    drawRoundRect(
                        color = AppColors.Accent.pink,
                        topLeft = Offset.Zero,
                        size = Size(thumbCenterX, h),
                        cornerRadius = CornerRadius(r, r),
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffsetXDp, y = 0.dp)
                        .align(Alignment.CenterStart)
                        .size(ThumbDp)
                        .clip(CircleShape)
                        .background(AppColors.Accent.pink)
                        .border(width = 3.dp, color = Color.White, shape = CircleShape),
                )
            }
        }
        Text(
            text = value.roundToInt().toString(),
            style = AppTypography.subheadline,
            color = AppColors.Text.secondary,
            modifier = Modifier.padding(start = AppSpacing.xxs),
        )
    }
}
