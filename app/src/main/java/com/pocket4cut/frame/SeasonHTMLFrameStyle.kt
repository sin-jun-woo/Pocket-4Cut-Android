package com.pocket4cut.frame

import android.graphics.Canvas
import android.graphics.Paint
import com.pocket4cut.ui.designsystem.theme.Season

/**
 * Printed-paper stock and restrained geometric ink for the four seasonal editions.
 * The legacy object name is retained for existing callers; no HTML is rendered.
 * Generated illustrations live in SeasonalStickerArt, not in procedural paths here.
 */
object SeasonHTMLFrameStyle {
    const val CELL_CORNER_RATIO: Float = 0f

    fun baseHex(season: Season): Long = when (season) {
        Season.SPRING -> 0xFFF6F1
        Season.SUMMER -> 0xEFF8F7
        Season.AUTUMN -> 0xF5E9D5
        Season.WINTER -> 0xEFF2F5
    }

    fun cellStrokeHex(season: Season): Long = when (season) {
        Season.SPRING -> 0x9F3557
        Season.SUMMER -> 0x245A77
        Season.AUTUMN -> 0x78452D
        Season.WINTER -> 0x3D556B
    }

    fun outerStrokeHex(season: Season): Long = cellStrokeHex(season)

    fun outerBorderWidthPoints(season: Season): Float = when (season) {
        Season.SPRING, Season.SUMMER, Season.AUTUMN, Season.WINTER -> 0.8f
    }

    fun drawHTMLBackdrop(season: Season, canvas: Canvas, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val scale = width / 390f
        val paper = Paint().apply { color = (0xFF000000 or baseHex(season)).toInt() }
        canvas.drawRect(0f, 0f, width, height, paper)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (0xFF000000 or cellStrokeHex(season)).toInt()
            strokeWidth = 0.45f * scale
        }
        // Fine, regular print marks are intentionally quieter than the photo and
        // illustrated stickers. They are behind all slots and text in drawScene.
        when (season) {
            Season.SPRING -> {
                ink.alpha = 15
                val step = 12f * scale
                var x = 0f
                while (x < width) {
                    canvas.drawRect(x, 0f, x + 3f * scale, height, ink)
                    x += step
                }
                var y = 0f
                while (y < height) {
                    canvas.drawRect(0f, y, width, y + 3f * scale, ink)
                    y += step
                }
            }
            Season.SUMMER -> {
                ink.alpha = 18
                val step = 10f * scale
                var x = 0f
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height, ink)
                    x += step
                }
            }
            Season.AUTUMN -> {
                ink.alpha = 20
                val step = 11f * scale
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, ink)
                    y += step
                }
            }
            Season.WINTER -> {
                ink.alpha = 17
                val step = 18f * scale
                var x = 0f
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height, ink)
                    canvas.drawLine(x + 2f * scale, 0f, x + 2f * scale, height, ink)
                    x += step
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, ink)
                    canvas.drawLine(0f, y + 2f * scale, width, y + 2f * scale, ink)
                    y += step
                }
            }
        }
    }
}
