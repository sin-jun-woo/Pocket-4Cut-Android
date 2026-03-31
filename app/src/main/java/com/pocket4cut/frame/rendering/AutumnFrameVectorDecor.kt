package com.pocket4cut.frame.rendering

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.min

/**
 * Ref-space (600×1800) autumn frame vector decor — Canvas port of iOS CGContext drawing.
 */
object AutumnFrameVectorDecor {

    private const val REF_W = 600f
    private const val REF_H = 1800f

    private val pumpkinLight = 0xFFF4A76B.toInt()
    private val pumpkinDark = 0xFFD97D54.toInt()
    private val mapleStroke = 0xFFE8956B.toInt()
    private val mapleFillLight = 0xFFF4A76B.toInt()
    private val acornCapColor = 0xFFA8B89B.toInt()
    private val acornBodyColor = 0xFFD99B87.toInt()
    private val spr1 = 0x99F4D58D.toInt()
    private val spr2 = 0x99E8956B.toInt()
    private val spr3 = 0x99F4A76B.toInt()

    private fun mapleStroke60(): Int {
        val a = (0.6f * 255f).toInt().coerceIn(0, 255)
        return (a shl 24) or (mapleStroke and 0x00FFFFFF)
    }

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        useBorderAdjacentSmallDecor: Boolean = true,
        slotRects: List<RectF>? = null,
    ) {
        if (width <= 0f || height <= 0f) return
        val sx = width / REF_W
        val sy = height / REF_H

        val bounds = SeasonDecorAnchors.refBounds(slotRects, width, height, REF_W, REF_H)
        val tiny3 = SeasonDecorAnchors.tiny3(bounds, REF_W, REF_H)

        canvas.save()
        canvas.scale(sx, sy)

        drawTopLeftGradientMaple(canvas, 50f, 78f)
        drawTopRightFlatMaple(canvas, 545f, 72f)

        if (!useBorderAdjacentSmallDecor) {
            drawFallingLeaf(canvas, 268f, 302f)
            drawPumpkin(canvas, 398f, 352f)
            drawAcorn(canvas, 178f, 328f)
            drawSprinkleDots(canvas)
            drawTiny3EmojiReplacements(canvas, tiny3)
        }

        drawBottomSmallLeaves(canvas)

        if (useBorderAdjacentSmallDecor) {
            val accent = SeasonDecorAnchors.bodyAccent(bounds, REF_W, REF_H)
            drawBodyAccentMaple(canvas, accent)
        }

        canvas.restore()

        drawFooter(canvas, width, height, slotRects)
    }

    private fun mapleLeafPath(baseX: Float, baseY: Float): Path {
        val ox = baseX - 80f
        val oy = baseY - 15f
        return Path().apply {
            moveTo(80f + ox, 15f + oy)
            lineTo(70f + ox, 25f + oy)
            lineTo(75f + ox, 35f + oy)
            lineTo(70f + ox, 40f + oy)
            lineTo(80f + ox, 45f + oy)
            lineTo(85f + ox, 40f + oy)
            lineTo(80f + ox, 35f + oy)
            lineTo(90f + ox, 25f + oy)
            close()
        }
    }

    private fun drawMapleVeins(
        canvas: Canvas,
        baseX: Float,
        baseY: Float,
        strokeW: Float,
        strokeColor: Int = mapleStroke,
    ) {
        val ox = baseX - 80f
        val oy = baseY - 15f
        val vein = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            color = strokeColor
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val p = Path()
        p.moveTo(80f + ox, 18f + oy)
        p.lineTo(80f + ox, 43f + oy)
        canvas.drawPath(p, vein)
        p.rewind()
        p.moveTo(80f + ox, 28f + oy)
        p.lineTo(72f + ox, 24f + oy)
        canvas.drawPath(p, vein)
        p.rewind()
        p.moveTo(80f + ox, 30f + oy)
        p.lineTo(88f + ox, 24f + oy)
        canvas.drawPath(p, vein)
        p.rewind()
        p.moveTo(77f + ox, 36f + oy)
        p.lineTo(72f + ox, 38f + oy)
        canvas.drawPath(p, vein)
        p.rewind()
        p.moveTo(83f + ox, 36f + oy)
        p.lineTo(88f + ox, 38f + oy)
        canvas.drawPath(p, vein)
    }

    private fun drawTopLeftGradientMaple(canvas: Canvas, baseX: Float, baseY: Float) {
        val leafPath = mapleLeafPath(baseX, baseY)
        val b = RectF()
        leafPath.computeBounds(b, true)
        val grad = LinearGradient(
            b.left,
            b.top,
            b.right,
            b.bottom,
            mapleFillLight,
            pumpkinDark,
            Shader.TileMode.CLAMP,
        )
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = grad
        }
        canvas.drawPath(leafPath, fill)
        fill.shader = null
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.25f
            color = mapleStroke
        }
        canvas.drawPath(leafPath, outline)
        drawMapleVeins(canvas, baseX, baseY, 1.15f)
    }

    private fun drawTopRightFlatMaple(canvas: Canvas, baseX: Float, baseY: Float) {
        val leafPath = mapleLeafPath(baseX, baseY)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = mapleFillLight
        }
        canvas.drawPath(leafPath, fill)
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.25f
            color = mapleStroke
        }
        canvas.drawPath(leafPath, outline)
        drawMapleVeins(canvas, baseX, baseY, 1.15f)
    }

    private fun drawFallingLeaf(canvas: Canvas, cx: Float, cy: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = pumpkinDark
        }
        val fill2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = mapleFillLight
        }
        val stem = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = pumpkinDark
            strokeCap = Paint.Cap.ROUND
        }

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(-38f)
        canvas.drawOval(RectF(-28f, -9f, 28f, 9f), fill)
        canvas.rotate(22f)
        canvas.drawOval(RectF(-20f, -7f, 20f, 7f), fill2)
        canvas.restore()

        canvas.drawLine(cx + 6f, cy + 10f, cx - 5f, cy + 42f, stem)
    }

    private fun drawPumpkin(canvas: Canvas, cx: Float, cy: Float) {
        val rx = 56f
        val ry = 48f
        val oval = RectF(cx - rx, cy - ry, cx + rx, cy + ry)

        val bodyGrad = LinearGradient(
            cx,
            cy - ry,
            cx,
            cy + ry,
            pumpkinLight,
            pumpkinDark,
            Shader.TileMode.CLAMP,
        )
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = bodyGrad
        }
        canvas.drawOval(oval, fill)
        fill.shader = null

        val stripe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.4f
            color = pumpkinDark
            alpha = 160
        }
        val sp = Path()
        val n = 5
        for (i in 0 until n) {
            val t = (i + 0.5f) / n
            val x = oval.left + oval.width() * t
            val w = oval.width() * 0.08f
            sp.rewind()
            sp.moveTo(x - w * 0.5f, oval.top + 8f)
            sp.cubicTo(
                x - w,
                cy - ry * 0.2f,
                x + w * 0.3f,
                cy + ry * 0.35f,
                x,
                oval.bottom - 6f,
            )
            canvas.drawPath(sp, stripe)
        }

        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = pumpkinDark
        }
        canvas.drawRoundRect(
            RectF(cx - 7f, oval.top - 18f, cx + 7f, oval.top + 4f),
            3f,
            3f,
            stemPaint,
        )

        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = mapleStroke
        }
        canvas.drawOval(RectF(cx - 18f, oval.top - 26f, cx + 18f, oval.top - 8f), capPaint)
    }

    private fun drawAcorn(canvas: Canvas, cx: Float, cy: Float) {
        val capH = 22f
        val capW = 34f
        val capRect = RectF(cx - capW / 2f, cy - 42f, cx + capW / 2f, cy - 42f + capH)
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = acornCapColor
        }
        canvas.drawOval(capRect, capPaint)

        val scallop = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            color = pumpkinDark
            alpha = 200
        }
        val scallopPath = Path()
        val scallY = capRect.bottom - 2f
        val step = capW / 5f
        var sx = capRect.left + step * 0.6f
        while (sx < capRect.right - step * 0.3f) {
            scallopPath.rewind()
            scallopPath.moveTo(sx, scallY)
            scallopPath.quadTo(sx + step * 0.35f, scallY + 7f, sx + step * 0.7f, scallY)
            canvas.drawPath(scallopPath, scallop)
            sx += step * 0.85f
        }

        val bodyRect = RectF(cx - 26f, cy - 22f, cx + 26f, cy + 28f)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = acornBodyColor
        }
        canvas.drawOval(bodyRect, bodyPaint)

        val hi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0x66FFFFFF.toInt()
        }
        canvas.drawOval(RectF(bodyRect.left + 6f, bodyRect.top + 8f, bodyRect.left + 18f, bodyRect.top + 22f), hi)

        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.1f
            color = mapleStroke
            alpha = 140
        }
        canvas.drawOval(bodyRect, outline)
    }

    private fun smallMaplePath(baseX: Float, baseY: Float, scale: Float): Path {
        val src = mapleLeafPath(baseX, baseY)
        val m = Matrix()
        m.setScale(scale, scale, baseX, baseY)
        src.transform(m)
        return src
    }

    /** [baseX],[baseY]를 중심으로 단풍 잎 + 잎맥 (현재 canvas 변환 행렬에 맞춤). */
    private fun drawScaledMapleWithVeins(
        canvas: Canvas,
        baseX: Float,
        baseY: Float,
        scale: Float,
        useGradientFill: Boolean,
    ) {
        canvas.save()
        canvas.translate(baseX, baseY)
        canvas.scale(scale, scale)
        canvas.translate(-baseX, -baseY)
        val leafPath = mapleLeafPath(baseX, baseY)
        if (useGradientFill) {
            val b = RectF()
            leafPath.computeBounds(b, true)
            val grad = LinearGradient(
                b.left,
                b.top,
                b.right,
                b.bottom,
                mapleFillLight,
                pumpkinDark,
                Shader.TileMode.CLAMP,
            )
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = grad
            }
            canvas.drawPath(leafPath, fill)
        } else {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = mapleFillLight
            }
            canvas.drawPath(leafPath, fill)
        }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.25f / scale.coerceAtLeast(0.01f)
            color = mapleStroke
        }
        canvas.drawPath(leafPath, outline)
        drawMapleVeins(
            canvas,
            baseX,
            baseY,
            1.15f / scale.coerceAtLeast(0.01f),
        )
        canvas.restore()
    }

    private fun drawBottomSmallLeaves(canvas: Canvas) {
        val s1 = 0.34f
        val p1 = smallMaplePath(95f, 1705f, s1)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = pumpkinDark
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = mapleStroke
        }
        canvas.drawPath(p1, fill)
        canvas.drawPath(p1, stroke)

        canvas.save()
        canvas.scale(-1f, 1f, 512f, 1702f)
        val p2 = smallMaplePath(512f, 1702f, 0.36f)
        canvas.drawPath(p2, fill)
        canvas.drawPath(p2, stroke)
        canvas.restore()
    }

    private fun drawSprinkleDots(canvas: Canvas) {
        val r = 4.2f
        val pts = arrayOf(
            Triple(120f, 195f, spr1),
            Triple(480f, 220f, spr2),
            Triple(310f, 155f, spr3),
            Triple(520f, 420f, spr1),
            Triple(95f, 510f, spr2),
            Triple(250f, 620f, spr3),
            Triple(410f, 580f, spr1),
            Triple(155f, 780f, spr2),
            Triple(445f, 890f, spr3),
            Triple(200f, 1020f, spr1),
            Triple(360f, 980f, spr2),
            Triple(520f, 1150f, spr3),
            Triple(78f, 1280f, spr1),
            Triple(290f, 1320f, spr2),
            Triple(470f, 1380f, spr3),
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for ((x, y, c) in pts) {
            p.color = c
            canvas.drawCircle(x, y, r, p)
        }
    }

    private fun drawMiniAcorn(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)
        canvas.translate(-cx, -cy)
        drawAcorn(canvas, cx, cy - 6f)
        canvas.restore()
    }

    private fun drawTiny3EmojiReplacements(canvas: Canvas, t: Tiny3) {
        val mapleScale = 0.22f
        val acornScale = 0.28f
        drawScaledMapleWithVeins(canvas, t.top.x, t.top.y, mapleScale, useGradientFill = false)
        drawMiniAcorn(canvas, t.right.x, t.right.y + 8f, acornScale)
        drawScaledMapleWithVeins(canvas, t.left.x, t.left.y, mapleScale, useGradientFill = true)
        drawMiniAcorn(canvas, t.left.x + 18f, t.left.y + 22f, acornScale * 0.85f)
    }

    private fun drawBodyAccentMaple(canvas: Canvas, accent: BodyAccent) {
        val px = accent.point.x
        val py = accent.point.y
        val scale = 0.28f
        canvas.save()
        canvas.translate(px, py)
        when (accent.edge) {
            BodyEdge.RIGHT -> canvas.rotate(15f)
            BodyEdge.LEFT -> canvas.rotate(-15f)
            BodyEdge.BOTTOM -> canvas.rotate(78f)
        }
        canvas.scale(scale, scale)
        canvas.translate(-px, -py)
        val leafPath = mapleLeafPath(px, py)
        val b = RectF()
        leafPath.computeBounds(b, true)
        val grad = LinearGradient(
            b.left,
            b.top,
            b.right,
            b.bottom,
            mapleFillLight,
            pumpkinDark,
            Shader.TileMode.CLAMP,
        )
        val f = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = grad
        }
        canvas.drawPath(leafPath, f)
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.25f / scale.coerceAtLeast(0.01f)
            color = mapleStroke
        }
        canvas.drawPath(leafPath, outline)
        drawMapleVeins(canvas, px, py, 1.15f / scale.coerceAtLeast(0.01f))
        canvas.restore()
    }

    private fun drawFooter(canvas: Canvas, width: Float, height: Float, slotRects: List<RectF>?) {
        val text = "Autumn Memories"
        val refScale = min(width / REF_W, height / REF_H)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mapleStroke60()
            textSize = 22f * refScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val fm = paint.fontMetrics
        val textH = fm.descent - fm.ascent
        val baselineY = SeasonDecorAnchors.memoriesBaselineY(
            height,
            slotRects,
            textH,
            height * 0.954f,
        )
        val textWidth = paint.measureText(text)
        val gap = 10f * refScale
        val iconScale = 0.52f * refScale
        val iconApprox = 22f * iconScale
        val totalW = textWidth + gap + iconApprox
        val startX = (width - totalW) / 2f
        canvas.drawText(text, startX, baselineY, paint)

        val iconCx = startX + textWidth + gap + 6f * refScale
        val iconCy = baselineY + (fm.ascent + fm.descent) * 0.5f - 3f * refScale
        canvas.save()
        canvas.translate(iconCx, iconCy)
        canvas.scale(iconScale, iconScale)
        val miniPath = mapleLeafPath(0f, 0f)
        val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = mapleFillLight
        }
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.1f / iconScale.coerceAtLeast(0.01f)
            color = mapleStroke60()
        }
        canvas.drawPath(miniPath, fp)
        canvas.drawPath(miniPath, sp)
        drawMapleVeins(canvas, 0f, 0f, 1.15f / iconScale.coerceAtLeast(0.01f), mapleStroke60())
        canvas.restore()
    }
}
