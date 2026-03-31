package com.pocket4cut.frame

import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.Brush
import com.pocket4cut.ui.designsystem.theme.Season

object SeasonHTMLFrameStyle {
    const val OUTER_CORNER_RATIO: Float = 40f / 600f
    const val CELL_CORNER_RATIO: Float = 24f / 520f

    fun baseHex(season: Season): Long = when (season) {
        Season.SPRING -> 0xFFF9F5
        Season.SUMMER -> 0xE6F7FF
        Season.AUTUMN -> 0xFFF8F0
        Season.WINTER -> 0xF0F8FF
    }

    fun cellStrokeHex(season: Season): Long = when (season) {
        Season.SPRING -> 0xFFB4D6
        Season.SUMMER -> 0x4FC3F7
        Season.AUTUMN -> 0xE8956B
        Season.WINTER -> 0xA8D8EA
    }

    fun outerStrokeHex(season: Season): Long = when (season) {
        Season.SPRING -> 0xFFB4D6
        Season.SUMMER -> 0x4FC3F7
        Season.AUTUMN -> 0xE8956B
        Season.WINTER -> 0xA8D8EA
    }

    fun outerBorderWidthPoints(season: Season): Float =
        if (season == Season.SUMMER) 5f else 4f

    fun captionDecorTextHex(season: Season): Long = when (season) {
        Season.SPRING -> 0xFF6B9D
        Season.SUMMER -> 0x4FC3F7
        Season.AUTUMN -> 0xD97D54
        Season.WINTER -> 0x89CFF0
    }

    fun canvasGradientBrush(season: Season): Brush = when (season) {
        Season.SPRING -> Brush.verticalGradient(
            0f to androidx.compose.ui.graphics.Color(0x4DFFE5F0),
            0.5f to androidx.compose.ui.graphics.Color(0x33FFF4C4),
            1f to androidx.compose.ui.graphics.Color(0x4DFFE5E5),
        )
        Season.SUMMER -> Brush.verticalGradient(
            0f to androidx.compose.ui.graphics.Color(0xFFE6F7FF),
            0.3f to androidx.compose.ui.graphics.Color(0x26FFD93D),
            0.6f to androidx.compose.ui.graphics.Color(0x334FC3F7),
            1f to androidx.compose.ui.graphics.Color(0xFFCCF0FF),
        )
        Season.AUTUMN -> Brush.verticalGradient(
            0f to androidx.compose.ui.graphics.Color(0x4DFFE8D6),
            0.5f to androidx.compose.ui.graphics.Color(0x33F4D58D),
            1f to androidx.compose.ui.graphics.Color(0x4DE8956B),
        )
        Season.WINTER -> Brush.verticalGradient(
            0f to androidx.compose.ui.graphics.Color(0x66D6EFFF),
            0.5f to androidx.compose.ui.graphics.Color(0x4DC8E8FF),
            1f to androidx.compose.ui.graphics.Color(0x66B8E1F5),
        )
    }

    fun drawGradientOverlay(
        season: Season,
        canvas: android.graphics.Canvas,
        width: Float,
        height: Float,
    ) {
        val colors: IntArray
        val positions: FloatArray
        when (season) {
            Season.SPRING -> {
                colors = intArrayOf(
                    argb(0.3f, 1f, 0.898f, 0.941f),
                    argb(0.2f, 1f, 0.956f, 0.769f),
                    argb(0.3f, 1f, 0.898f, 0.898f),
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
            Season.SUMMER -> {
                colors = intArrayOf(
                    argb(1f, 0.902f, 0.969f, 1f),
                    argb(0.15f, 1f, 0.851f, 0.239f),
                    argb(0.2f, 0.310f, 0.765f, 0.969f),
                    argb(1f, 0.8f, 0.941f, 1f),
                )
                positions = floatArrayOf(0f, 0.3f, 0.6f, 1f)
            }
            Season.AUTUMN -> {
                colors = intArrayOf(
                    argb(0.3f, 1f, 0.910f, 0.839f),
                    argb(0.2f, 0.957f, 0.835f, 0.553f),
                    argb(0.3f, 0.910f, 0.584f, 0.420f),
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
            Season.WINTER -> {
                colors = intArrayOf(
                    argb(0.4f, 0.839f, 0.937f, 1f),
                    argb(0.3f, 0.784f, 0.910f, 1f),
                    argb(0.4f, 0.722f, 0.882f, 0.961f),
                )
                positions = floatArrayOf(0f, 0.5f, 1f)
            }
        }
        val shader = LinearGradient(0f, 0f, 0f, height, colors, positions, Shader.TileMode.CLAMP)
        val paint = android.graphics.Paint().apply { this.shader = shader }
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    fun drawHTMLBackdrop(
        season: Season,
        canvas: android.graphics.Canvas,
        width: Float,
        height: Float,
    ) {
        val hex = baseHex(season)
        val paint = android.graphics.Paint().apply {
            color = (0xFF000000 or hex).toInt()
        }
        canvas.drawRect(0f, 0f, width, height, paint)
        drawGradientOverlay(season, canvas, width, height)
    }

    private fun argb(a: Float, r: Float, g: Float, b: Float): Int {
        return ((a * 255).toInt() shl 24) or
                ((r * 255).toInt() shl 16) or
                ((g * 255).toInt() shl 8) or
                (b * 255).toInt()
    }
}
