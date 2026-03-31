package com.pocket4cut.frame.rendering

import android.graphics.RectF

data class RefBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val midX: Float,
    val midY: Float,
)

enum class BodyEdge { RIGHT, LEFT, BOTTOM }

data class PointF2(val x: Float, val y: Float)

data class Tiny3(val top: PointF2, val right: PointF2, val left: PointF2)
data class Tiny4(val top: PointF2, val right: PointF2, val left: PointF2, val bottom: PointF2)
data class BodyAccent(val point: PointF2, val edge: BodyEdge)

object SeasonDecorAnchors {

    fun refBounds(
        slotRects: List<RectF>?,
        canvasWidth: Float,
        canvasHeight: Float,
        refWidth: Float,
        refHeight: Float,
    ): RefBounds {
        if (slotRects.isNullOrEmpty() || canvasWidth <= 0 || canvasHeight <= 0) {
            val minX = refWidth * 0.1f
            val maxX = refWidth * 0.9f
            val minY = refHeight * 0.15f
            val maxY = refHeight * 0.86f
            return RefBounds(minX, maxX, minY, maxY, (minX + maxX) * 0.5f, (minY + maxY) * 0.5f)
        }
        val sx = refWidth / canvasWidth
        val sy = refHeight / canvasHeight
        val refRects = slotRects.map { r ->
            RectF(r.left * sx, r.top * sy, r.right * sx, r.bottom * sy)
        }
        val minX = maxOf(0f, refRects.minOf { it.left })
        val maxX = minOf(refWidth, refRects.maxOf { it.right })
        val minY = maxOf(0f, refRects.minOf { it.top })
        val maxY = minOf(refHeight, refRects.maxOf { it.bottom })
        return RefBounds(minX, maxX, minY, maxY, (minX + maxX) * 0.5f, (minY + maxY) * 0.5f)
    }

    fun tiny3(b: RefBounds, refWidth: Float, refHeight: Float): Tiny3 = Tiny3(
        top = PointF2(b.midX, maxOf(120f, b.minY - 16f)),
        right = PointF2(
            minOf(refWidth - 20f, b.maxX + 10f),
            b.minY + (b.maxY - b.minY) * 0.45f,
        ),
        left = PointF2(
            maxOf(20f, b.minX - 10f),
            minOf(refHeight - 150f, b.maxY + 20f),
        ),
    )

    fun tiny4(b: RefBounds, refWidth: Float, refHeight: Float): Tiny4 = Tiny4(
        top = PointF2(b.midX, maxOf(120f, b.minY - 16f)),
        right = PointF2(
            minOf(refWidth - 20f, b.maxX + 10f),
            b.minY + (b.maxY - b.minY) * 0.45f,
        ),
        left = PointF2(
            maxOf(20f, b.minX - 10f),
            minOf(refHeight - 150f, b.maxY - 80f),
        ),
        bottom = PointF2(b.midX, minOf(refHeight - 160f, b.maxY + 24f)),
    )

    fun bodyAccent(b: RefBounds, refWidth: Float, refHeight: Float): BodyAccent {
        if (b.maxX + 14f <= refWidth - 20f) {
            return BodyAccent(
                PointF2(b.maxX + 14f, b.minY + (b.maxY - b.minY) * 0.72f),
                BodyEdge.RIGHT,
            )
        }
        if (b.minX - 14f >= 20f) {
            return BodyAccent(
                PointF2(b.minX - 14f, b.minY + (b.maxY - b.minY) * 0.72f),
                BodyEdge.LEFT,
            )
        }
        return BodyAccent(
            PointF2(b.midX, minOf(refHeight - 180f, b.maxY + 26f)),
            BodyEdge.BOTTOM,
        )
    }

    fun memoriesBaselineY(
        canvasHeight: Float,
        slotRects: List<RectF>?,
        textHeight: Float,
        fallback: Float,
    ): Float {
        if (slotRects.isNullOrEmpty()) return fallback
        val slotBottom = slotRects.maxOf { it.bottom }
        val preferred = slotBottom + 10f
        val maxY = canvasHeight - textHeight - 8f
        return minOf(preferred, maxY)
    }
}
