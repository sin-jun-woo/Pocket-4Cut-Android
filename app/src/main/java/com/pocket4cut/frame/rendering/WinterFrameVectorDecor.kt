package com.pocket4cut.frame.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ref-space (600×1800) winter vector overlay, scaled to [width]×[height].
 * Port of iOS WinterFrameVectorDecor (CGContext → Canvas).
 */
object WinterFrameVectorDecor {

    private const val REF_W = 600f
    private const val REF_H = 1800f

    private val snowTop = 0xFF89CFF0.toInt()
    private val snowMid = 0xFFA8D8EA.toInt()
    private val snowEdge = 0xFFC4D7F2.toInt()

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
        val tiny = SeasonDecorAnchors.tiny4(bounds, REF_W, REF_H)

        canvas.save()
        canvas.scale(sx, sy)

        drawFrostPattern(canvas)
        drawSnowflakeDetailed(canvas, 142f, 96f, 40f)
        drawSnowflakeDetailed(canvas, 458f, 90f, 36f)

        if (!useBorderAdjacentSmallDecor) {
            drawSnowman(canvas, 302f, 498f)
            drawIceCrystal(canvas, 125f, 735f, 78f)
            drawBottomEightPointStar(canvas, 300f, 1510f, 95f)
            drawSnowflakeSimple(canvas, 478f, 1675f, 16f)
            drawSnowflakeSimple(canvas, 515f, 1702f, 13f)
            drawSprinkleDots(canvas)
        } else {
            val accent = SeasonDecorAnchors.bodyAccent(bounds, REF_W, REF_H)
            drawSnowflakeSimple(canvas, accent.point.x, accent.point.y, 24f)
        }

        drawTinyIconSnowflake(canvas, tiny.top.x, tiny.top.y, 15f)
        drawTinyIconStar(canvas, tiny.right.x, tiny.right.y, 13f)
        drawTinyIconSnowman(canvas, tiny.left.x, tiny.left.y, 17f)
        drawTinyIconSparkle(canvas, tiny.bottom.x, tiny.bottom.y, 11f)

        canvas.restore()

        drawFooter(canvas, width, height, slotRects)
    }

    /** Frost: tiled dots every 100 ref units. */
    private fun drawFrostPattern(canvas: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowEdge
            alpha = 140
            style = Paint.Style.FILL
        }
        var x = 0f
        while (x <= REF_W) {
            var y = 0f
            while (y <= REF_H) {
                canvas.drawCircle(x, y, 2.2f, p)
                y += 100f
            }
            x += 100f
        }
    }

    /** Six arms at 60°, core circle, arm lines, end dots, mid-arm branch dots. */
    private fun drawSnowflakeDetailed(canvas: Canvas, cx: Float, cy: Float, armLen: Float) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowMid
            style = Paint.Style.STROKE
            strokeWidth = 2.8f
            strokeCap = Paint.Cap.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowTop
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 5.5f, fill)
        repeat(6) { i ->
            val rad = Math.toRadians((i * 60 - 90).toDouble())
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            val ex = cx + c * armLen
            val ey = cy + s * armLen
            canvas.drawLine(cx, cy, ex, ey, stroke)
            canvas.drawCircle(ex, ey, 3.2f, fill)
            val mx = cx + c * (armLen * 0.48f)
            val my = cy + s * (armLen * 0.48f)
            val px = -s * 7f
            val py = c * 7f
            canvas.drawCircle(mx + px, my + py, 2.8f, fill)
            canvas.drawCircle(mx - px, my - py, 2.8f, fill)
        }
    }

    /** Minimal 6-arm snowflake: line + end dot per arm. */
    private fun drawSnowflakeSimple(canvas: Canvas, cx: Float, cy: Float, armLen: Float) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowMid
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowTop
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, armLen * 0.14f, fill)
        repeat(6) { i ->
            val rad = Math.toRadians((i * 60 - 90).toDouble())
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            val ex = cx + c * armLen
            val ey = cy + s * armLen
            canvas.drawLine(cx, cy, ex, ey, stroke)
            canvas.drawCircle(ex, ey, armLen * 0.09f, fill)
        }
    }

    private fun drawSnowman(canvas: Canvas, cx: Float, baseY: Float) {
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = 235
        }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowMid
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val eye = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2C3E50.toInt()
            style = Paint.Style.FILL
        }
        val carrot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF9F45.toInt()
            style = Paint.Style.FILL
        }
        val coal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF34495E.toInt()
            style = Paint.Style.FILL
        }
        val arm = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8D6E63.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
        }

        val rBot = 58f
        val rMid = 42f
        val rHead = 32f
        val yBot = baseY
        val yMid = yBot - rBot - rMid + 6f
        val yHead = yMid - rMid - rHead + 6f

        fun drawBall(x: Float, y: Float, r: Float) {
            canvas.drawCircle(x, y, r, body)
            canvas.drawCircle(x, y, r, outline)
        }
        drawBall(cx, yBot, rBot)
        drawBall(cx, yMid, rMid)
        drawBall(cx, yHead, rHead)

        val eyeOff = 11f
        canvas.drawCircle(cx - eyeOff, yHead - 4f, 4f, eye)
        canvas.drawCircle(cx + eyeOff, yHead - 4f, 4f, eye)

        val nose = Path().apply {
            moveTo(cx + rHead * 0.15f, yHead + 2f)
            lineTo(cx + rHead * 0.95f, yHead + 6f)
            lineTo(cx + rHead * 0.12f, yHead + 10f)
            close()
        }
        canvas.drawPath(nose, carrot)

        val mouthR = 14f
        repeat(5) { k ->
            val t = (k - 2) * 0.32f
            val mx = cx + sin(t.toDouble()).toFloat() * 5f
            val my = yHead + 18f + kotlin.math.abs(k - 2) * 2.5f
            canvas.drawCircle(mx, my, 2.5f, coal)
        }

        canvas.drawCircle(cx, yMid + 8f, 3.5f, coal)
        canvas.drawCircle(cx, yMid + 22f, 3.5f, coal)
        canvas.drawCircle(cx, yMid + 36f, 3.5f, coal)

        canvas.drawLine(cx - rMid, yMid, cx - rMid - 52f, yMid - 18f, arm)
        canvas.drawLine(cx + rMid, yMid, cx + rMid + 52f, yMid - 18f, arm)
        canvas.drawCircle(cx - rMid - 52f, yMid - 18f, 3f, coal)
        canvas.drawCircle(cx + rMid + 52f, yMid - 18f, 3f, coal)
    }

    /** Hexagonal crystal: gradient fill, inner spokes, sparkles. */
    private fun drawIceCrystal(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val hex = regularHexPath(cx, cy, radius)
        val shader = RadialGradient(
            cx, cy, radius * 1.05f,
            intArrayOf(snowMid, snowEdge),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
            style = Paint.Style.FILL
            alpha = 220
        }
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowTop
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowTop
            alpha = 160
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        canvas.drawPath(hex, fill)
        canvas.drawPath(hex, edge)

        repeat(6) { i ->
            val rad = Math.toRadians((i * 60 - 90).toDouble())
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            canvas.drawLine(cx, cy, cx + c * radius * 0.88f, cy + s * radius * 0.88f, inner)
        }
        repeat(6) { i ->
            val rad = Math.toRadians((i * 60 - 30).toDouble())
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            val ix = cx + c * radius * 0.45f
            val iy = cy + s * radius * 0.45f
            canvas.drawLine(ix, iy, cx + c * radius * 0.72f, cy + s * radius * 0.72f, inner)
        }

        val spark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = 200
        }
        val sparkPts = listOf(
            Pair(cx, cy - radius * 1.05f),
            Pair(cx + radius * 0.92f, cy - radius * 0.35f),
            Pair(cx - radius * 0.88f, cy + radius * 0.4f),
        )
        for ((sx, sy) in sparkPts) {
            canvas.drawCircle(sx, sy, 3.5f, spark)
            canvas.drawCircle(sx, sy, 1.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = snowTop
                style = Paint.Style.FILL
            })
        }
    }

    private fun regularHexPath(cx: Float, cy: Float, r: Float): Path {
        val p = Path()
        for (i in 0 until 6) {
            val rad = Math.toRadians((i * 60 - 90).toDouble())
            val x = cx + cos(rad).toFloat() * r
            val y = cy + sin(rad).toFloat() * r
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    /** Eight-point star: gradient outer, white inner, center dot. */
    private fun drawBottomEightPointStar(canvas: Canvas, cx: Float, cy: Float, outerR: Float) {
        val innerR = outerR * 0.42f
        val outerPath = star8Path(cx, cy, outerR, innerR)
        val lg = LinearGradient(
            cx, cy - outerR, cx, cy + outerR,
            intArrayOf(snowTop, snowMid, snowEdge),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        val pGrad = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = lg
            style = Paint.Style.FILL
            alpha = 230
        }
        val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowTop
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawPath(outerPath, pGrad)
        canvas.drawPath(outerPath, pStroke)

        val innerOuter = outerR * 0.55f
        val innerInner = innerOuter * 0.42f
        val innerPath = star8Path(cx, cy, innerOuter, innerInner)
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            alpha = 210
        }
        canvas.drawPath(innerPath, white)
        canvas.drawPath(innerPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowMid
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        })

        canvas.drawCircle(cx, cy, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowTop
            style = Paint.Style.FILL
        })
    }

    private fun star8Path(cx: Float, cy: Float, rOut: Float, rIn: Float): Path {
        val p = Path()
        val n = 16
        for (i in 0 until n) {
            val rad = Math.toRadians((i * 22.5 - 90).toDouble())
            val rr = if (i % 2 == 0) rOut else rIn
            val x = cx + cos(rad).toFloat() * rr
            val y = cy + sin(rad).toFloat() * rr
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    private fun drawSprinkleDots(canvas: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val rnd = java.util.Random(4242L)
        repeat(90) {
            val x = rnd.nextFloat() * REF_W
            val y = rnd.nextFloat() * REF_H
            val c = if (rnd.nextBoolean()) snowTop else snowMid
            p.color = c
            p.alpha = 100 + rnd.nextInt(100)
            val rad = 1.2f + rnd.nextFloat() * 2.2f
            canvas.drawCircle(x, y, rad, p)
        }
    }

    private fun drawTinyIconSnowflake(canvas: Canvas, cx: Float, cy: Float, armLen: Float) {
        drawSnowflakeSimple(canvas, cx, cy, armLen)
    }

    private fun drawTinyIconStar(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val path = star8Path(cx, cy, r, r * 0.4f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowTop
            alpha = 200
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowMid
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, stroke)
    }

    private fun drawTinyIconSnowman(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 230
            style = Paint.Style.FILL
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowMid
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val r3 = scale * 0.22f
        val r2 = scale * 0.28f
        val r1 = scale * 0.34f
        val y3 = cy + r3 * 0.4f
        val y2 = y3 - r3 - r2 + 3f
        val y1 = y2 - r2 - r1 + 3f
        canvas.drawCircle(cx, y3, r3, body)
        canvas.drawCircle(cx, y2, r2, body)
        canvas.drawCircle(cx, y1, r1, body)
        canvas.drawCircle(cx, y3, r3, line)
        canvas.drawCircle(cx, y2, r2, line)
        canvas.drawCircle(cx, y1, r1, line)
        canvas.drawLine(cx - r2, y2, cx - r2 - scale * 0.35f, y2 - scale * 0.12f, line)
        canvas.drawLine(cx + r2, y2, cx + r2 + scale * 0.35f, y2 - scale * 0.12f, line)
    }

    private fun drawTinyIconSparkle(canvas: Canvas, cx: Float, cy: Float, span: Float) {
        val main = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowTop
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }
        val dim = Paint(main).apply { alpha = 180 }
        canvas.drawLine(cx - span, cy, cx + span, cy, main)
        canvas.drawLine(cx, cy - span, cx, cy + span, main)
        canvas.drawLine(cx - span * 0.7f, cy - span * 0.7f, cx + span * 0.7f, cy + span * 0.7f, dim)
        canvas.drawLine(cx - span * 0.7f, cy + span * 0.7f, cx + span * 0.7f, cy - span * 0.7f, dim)
        canvas.drawCircle(cx, cy, 2.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = snowMid
            style = Paint.Style.FILL
        })
    }

    private fun drawFooter(canvas: Canvas, width: Float, height: Float, slotRects: List<RectF>?) {
        val label = "Winter Memories"
        val textSize = height * (28f / REF_H)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
            this.textSize = textSize
            color = Color.argb((0.7f * 255).toInt(), 0x89, 0xCF, 0xF0)
        }
        val fm = paint.fontMetrics
        val textH = fm.descent - fm.ascent
        val fallback = height - textH - 20f
        val baseline = SeasonDecorAnchors.memoriesBaselineY(height, slotRects, textH, fallback)

        val tw = paint.measureText(label)
        val startX = width * 0.5f - tw * 0.5f
        canvas.drawText(label, startX, baseline, paint)

        val flakeCx = startX + tw + textSize * 0.45f
        val flakeCy = baseline + fm.ascent + textH * 0.55f
        drawFooterMiniSnowflake(canvas, flakeCx, flakeCy, textSize * 0.35f)
    }

    private fun drawFooterMiniSnowflake(canvas: Canvas, cx: Float, cy: Float, armLen: Float) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.7f * 255).toInt(), 0x89, 0xCF, 0xF0)
            style = Paint.Style.STROKE
            strokeWidth = armLen * 0.12f
            strokeCap = Paint.Cap.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.7f * 255).toInt(), 0x89, 0xCF, 0xF0)
            style = Paint.Style.FILL
        }
        repeat(6) { i ->
            val rad = Math.toRadians((i * 60 - 90).toDouble())
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            val ex = cx + c * armLen
            val ey = cy + s * armLen
            canvas.drawLine(cx, cy, ex, ey, stroke)
            canvas.drawCircle(ex, ey, armLen * 0.12f, fill)
        }
    }
}
