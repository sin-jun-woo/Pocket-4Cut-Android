package com.pocket4cut.frame

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/** Output size depends on the frame geometry, never on display metrics. */
internal object CollageOutputSize {
    private const val MAX_PIXELS = 16_000_000L
    private const val MAX_EDGE = 8_192
    private const val TARGET_SLOT_SHORT_EDGE = 1_024

    fun forScene(scene: CollageLayoutDimensions): Pair<Int, Int> {
        require(scene.canvasWidth > 0f && scene.canvasHeight > 0f && scene.cells.isNotEmpty())
        val ratio = scene.canvasHeight.toDouble() / scene.canvasWidth.toDouble()
        val shortestSlot = scene.cells.minOf { min(it.width(), it.height()).toDouble() }
        require(shortestSlot > 0.0)
        val targetWidth = ceil(scene.canvasWidth.toDouble() * TARGET_SLOT_SHORT_EDGE / shortestSlot).toInt()
        val cap = floor(min(min(MAX_EDGE.toDouble(), MAX_EDGE / ratio), sqrt(MAX_PIXELS / ratio))).toInt()
        var width = min(targetWidth, cap).coerceAtLeast(1)
        var height = floor(width * ratio + 0.5).toInt().coerceAtLeast(1)
        while (width.toLong() * height > MAX_PIXELS || width > MAX_EDGE || height > MAX_EDGE) {
            width--
            require(width > 0)
            height = floor(width * ratio + 0.5).toInt().coerceAtLeast(1)
        }
        return width to height
    }
}
