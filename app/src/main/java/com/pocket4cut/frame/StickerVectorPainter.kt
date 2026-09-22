package com.pocket4cut.frame

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath

/** Draw the same Material vector geometry used by the Compose sticker preview. */
internal object StickerVectorPainter {
    fun draw(canvas: Canvas, sticker: StickerPalette, color: Int, size: Float) {
        if (size <= 0f) return
        val icon = sticker.icon
        canvas.save()
        canvas.translate(-size / 2f, -size / 2f)
        canvas.scale(size / icon.viewportWidth, size / icon.viewportHeight)
        drawGroup(canvas, icon.root, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        canvas.restore()
    }

    private fun drawGroup(canvas: Canvas, group: VectorGroup, paint: Paint) {
        canvas.save()
        canvas.translate(group.pivotX, group.pivotY)
        canvas.rotate(group.rotation)
        canvas.scale(group.scaleX, group.scaleY)
        canvas.translate(group.translationX - group.pivotX, group.translationY - group.pivotY)
        for (node: VectorNode in group) {
            when (node) {
                is VectorGroup -> drawGroup(canvas, node, paint)
                is VectorPath -> {
                    val path = PathParser().addPathNodes(node.pathData).toPath().asAndroidPath()
                    canvas.drawPath(path, paint)
                }
            }
        }
        canvas.restore()
    }
}
