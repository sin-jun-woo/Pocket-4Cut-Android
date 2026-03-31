package com.pocket4cut.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object ColorRGB {
    fun uint32(color: Color): Long {
        val argb = color.toArgb()
        return (argb.toLong() and 0x00FFFFFF)
    }

    fun color(rgb: Long): Color {
        return Color((0xFF000000 or (rgb and 0xFFFFFF)).toInt())
    }
}
