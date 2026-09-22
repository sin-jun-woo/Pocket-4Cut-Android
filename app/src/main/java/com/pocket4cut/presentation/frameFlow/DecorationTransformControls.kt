package com.pocket4cut.presentation.frameFlow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pocket4cut.frame.CustomFrameDecoration
import com.pocket4cut.frame.NormPoint
import com.pocket4cut.frame.StickerPalette
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import kotlin.math.roundToInt

internal fun CustomFrameDecoration.accessibilityLabel(): String = when (val value = kind) {
    is CustomFrameDecoration.Kind.Text -> "텍스트 ${value.content}"
    is CustomFrameDecoration.Kind.Emoji -> "이모지 ${value.content}"
    is CustomFrameDecoration.Kind.Sticker -> "스티커 ${StickerPalette.fromAssetId(value.assetId)?.displayName ?: "장식"}"
}

/** Single-tap and keyboard alternatives to the canvas's multi-touch gestures. */
@Composable
internal fun DecorationTransformControls(
    decoration: CustomFrameDecoration,
    onChange: (CustomFrameDecoration) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun move(dx: Float, dy: Float) = onChange(decoration.copy(position = NormPoint(
        (decoration.position.x + dx).coerceIn(0.02f, 0.98f),
        (decoration.position.y + dy).coerceIn(0.02f, 0.98f),
    )))
    fun scale(delta: Float) = onChange(decoration.copy(scale = (decoration.scale + delta).coerceIn(0.25f, 5f)))
    fun rotate(degrees: Int) = onChange(decoration.copy(
        rotationRadians = decoration.rotationRadians + Math.toRadians(degrees.toDouble()).toFloat(),
    ))
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(decoration.accessibilityLabel(), style = AppTypography.headline, color = AppColors.Text.primary)
        Text(
            "가로 ${(decoration.position.x * 100).roundToInt()}%, 세로 ${(decoration.position.y * 100).roundToInt()}% · " +
                "크기 ${(decoration.scale * 100).roundToInt()}% · 회전 ${Math.toDegrees(decoration.rotationRadians.toDouble()).roundToInt()}도",
            style = AppTypography.caption1,
            color = AppColors.Text.secondary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TransformButton("장식 왼쪽으로 이동", Icons.AutoMirrored.Filled.ArrowBack, decoration.position.x > 0.02f) { move(-0.02f, 0f) }
            TransformButton("장식 오른쪽으로 이동", Icons.AutoMirrored.Filled.ArrowForward, decoration.position.x < 0.98f) { move(0.02f, 0f) }
            TransformButton("장식 위로 이동", Icons.Default.ArrowUpward, decoration.position.y > 0.02f) { move(0f, -0.02f) }
            TransformButton("장식 아래로 이동", Icons.Default.ArrowDownward, decoration.position.y < 0.98f) { move(0f, 0.02f) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TransformButton("장식 작게", Icons.Default.ZoomOut, decoration.scale > 0.25f) { scale(-0.1f) }
            TransformButton("장식 크게", Icons.Default.ZoomIn, decoration.scale < 5f) { scale(0.1f) }
            TransformButton("장식 왼쪽으로 15도 회전", Icons.AutoMirrored.Filled.RotateLeft) { rotate(-15) }
            TransformButton("장식 오른쪽으로 15도 회전", Icons.AutoMirrored.Filled.RotateRight) { rotate(15) }
        }
        Text("이동은 2%, 크기는 10%, 회전은 15도씩 조절합니다.", style = AppTypography.caption1, color = AppColors.Text.secondary)
    }
}

@Composable
private fun TransformButton(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
    }
}
