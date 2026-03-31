package com.pocket4cut.frame.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Android port of iOS [SummerFrameVectorDecor] (600×1800 ref space).
 * Background / slots / outer border are drawn elsewhere; this draws only vector ornaments.
 */
object SummerFrameVectorDecor {

    private const val REF_W = 600f
    private const val REF_H = 1800f

    private val skyBlue: Int = 0xFF4FC3F7.toInt()
    private val mintGreen: Int = 0xFF6BCF9F.toInt()
    private val paleYellow: Int = 0xFFFFD93D.toInt()
    private val peachSun: Int = 0xFFFFB347.toInt()
    private val coral: Int = 0xFFFF7E67.toInt()
    private val pinkSoft: Int = 0xFFFF85A1.toInt()
    private val pinkDeep: Int = 0xFFFF6B9D.toInt()
    private val seedDark: Int = 0xFF2D2D2D.toInt()
    private val redInner: Int = 0xFFFFB4D6.toInt()
    private val creamInner: Int = 0xFFFFF4F0.toInt()
    private val trunkOrange: Int = 0xFFFFB347.toInt()

    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        useBorderAdjacentSmallDecor: Boolean = true,
        slotRects: List<RectF>? = null,
    ) {
        val scale = minOf(width / REF_W, height / REF_H)
        val sx = scale
        val sy = scale
        val ls = scale
        val tx = (width - REF_W * scale) * 0.5f
        val ty = (height - REF_H * scale) * 0.5f

        canvas.save()
        canvas.translate(tx, ty)
        canvas.scale(sx, sy)

        drawTopLeftWaves(canvas)
        drawTopRightPalm(canvas)
        if (!useBorderAdjacentSmallDecor) {
            drawBigWave(canvas)
            drawWatermelon(canvas)
            drawStarfish(canvas)
            drawShell(canvas)
            drawSprinkles(canvas)
        }

        val bounds = SeasonDecorAnchors.refBounds(slotRects, width, height, REF_W, REF_H)
        val anchors = SeasonDecorAnchors.tiny4(bounds, REF_W, REF_H)
        if (useBorderAdjacentSmallDecor) {
            val body = SeasonDecorAnchors.bodyAccent(bounds, REF_W, REF_H)
            drawSummerBodyWave(canvas, body.point.x, body.point.y)
        }
        drawSmallVectorEmojis(canvas, ls, useBorderAdjacentSmallDecor, anchors)

        canvas.restore()
        drawFooterText(canvas, width, height, sx, sy, ls, slotRects)
    }

    // MARK: - Top waves (translate(40,30) opacity 0.7)

    private fun drawTopLeftWaves(canvas: Canvas) {
        canvas.save()
        canvas.translate(40f, 30f)
        strokeWavePath(canvas, wavePath00(), skyBlue, 3f, 0.7f)
        strokeWavePath(canvas, wavePath58(), skyBlue, 2.5f, 0.7f)
        strokeWavePath(canvas, wavePath1014(), mintGreen, 2f, 0.5f)
        canvas.restore()
    }

    // MARK: - Palm (translate(550,40) opacity 0.8)

    private fun drawTopRightPalm(canvas: Canvas) {
        canvas.save()
        canvas.translate(550f, 40f)
        val ga = (255 * 0.8f).toInt()
        paintStroke.apply {
            color = trunkOrange
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            alpha = ga
        }
        canvas.drawLine(0f, 0f, 0f, 50f, paintStroke)

        paintStroke.color = mintGreen
        paintStroke.strokeWidth = 4f
        paintStroke.alpha = ga
        val palmPaths = listOf(
            palmPath(-20f, -12f, -30f, -8f),
            palmPath(-15f, -18f, -18f, -25f),
            palmPath(-8f, -20f, -5f, -28f),
            palmPath(5f, -20f, 8f, -28f),
            palmPath(12f, -18f, 15f, -25f),
            palmPath(20f, -12f, 28f, -10f),
        )
        for (p in palmPaths) {
            canvas.drawPath(p, paintStroke)
        }

        paintFill.color = trunkOrange
        paintFill.alpha = ga
        canvas.drawOval(RectF(-5f - 6f, 10f - 6f, -5f + 6f, 10f + 6f), paintFill)
        paintFill.color = trunkOrange
        paintFill.alpha = (255 * 0.8f * 0.8f).toInt()
        canvas.drawOval(RectF(5f - 5f, 12f - 5f, 5f + 5f, 12f + 5f), paintFill)
        paintStroke.alpha = 255
        paintFill.alpha = 255
        canvas.restore()
    }

    private fun palmPath(cx: Float, cy: Float, ex: Float, ey: Float): Path =
        Path().apply {
            moveTo(0f, 0f)
            quadTo(cx, cy, ex, ey)
        }

    // MARK: - Big wave (translate(30,700) opacity 0.7)

    private fun drawBigWave(canvas: Canvas) {
        canvas.save()
        canvas.translate(30f, 700f)
        strokeWavePath(canvas, wavePathBig1(), skyBlue, 4f, 0.7f)
        strokeWavePath(canvas, wavePathBig2(), skyBlue, 3.5f, 0.8f)
        strokeWavePath(canvas, wavePathBig3(), mintGreen, 3f, 0.6f)

        val dropA = (255 * 0.5f * 0.7f).toInt()
        paintFill.shader = null
        paintFill.color = Color.argb(dropA, Color.red(skyBlue), Color.green(skyBlue), Color.blue(skyBlue))
        canvas.drawOval(RectF(15f - 3f, -5f - 3f, 15f + 3f, -5f + 3f), paintFill)
        paintFill.color = Color.argb(dropA, Color.red(mintGreen), Color.green(mintGreen), Color.blue(mintGreen))
        canvas.drawOval(RectF(45f - 2.5f, -8f - 2.5f, 45f + 2.5f, -8f + 2.5f), paintFill)
        paintFill.color = Color.argb(dropA, Color.red(skyBlue), Color.green(skyBlue), Color.blue(skyBlue))
        canvas.drawOval(RectF(75f - 2f, -3f - 2f, 75f + 2f, -3f + 2f), paintFill)
        paintFill.alpha = 255
        canvas.restore()
    }

    // MARK: - Watermelon (translate(540,1100) opacity 0.85)

    private fun drawWatermelon(canvas: Canvas) {
        canvas.save()
        canvas.translate(540f, 1100f)
        paintFill.alpha = (255 * 0.85f).toInt()

        val halfPath = Path().apply {
            moveTo(-25f, 0f)
            quadTo(-25f, -25f, 0f, -25f)
            quadTo(25f, -25f, 25f, 0f)
            close()
        }

        paintFill.shader = RadialGradient(
            0f,
            -12.5f,
            32f,
            intArrayOf(pinkSoft, pinkDeep),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(halfPath, paintFill)
        paintFill.shader = null

        val redPath = Path().apply {
            moveTo(-20f, 0f)
            quadTo(-20f, -20f, 0f, -20f)
            quadTo(20f, -20f, 20f, 0f)
            close()
        }
        paintFill.color = redInner
        canvas.drawPath(redPath, paintFill)

        val creamPath = Path().apply {
            moveTo(-15f, 0f)
            quadTo(-15f, -15f, 0f, -15f)
            quadTo(15f, -15f, 15f, 0f)
            close()
        }
        paintFill.color = creamInner
        canvas.drawPath(creamPath, paintFill)

        val wa = (255 * 0.85f).toInt()
        drawEllipseRotated(canvas, -8f, -8f, 2.5f, 3.5f, -20f, seedDark, wa)
        drawEllipseRotated(canvas, 0f, -12f, 2.5f, 3.5f, 0f, seedDark, wa)
        drawEllipseRotated(canvas, 8f, -10f, 2.5f, 3.5f, 15f, seedDark, wa)
        drawEllipseRotated(canvas, -5f, -3f, 2f, 3f, -10f, seedDark, wa)
        drawEllipseRotated(canvas, 5f, -5f, 2f, 3f, 20f, seedDark, wa)

        paintFill.alpha = 255
        canvas.restore()
    }

    private fun drawEllipseRotated(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        angleDeg: Float,
        fillColor: Int,
        alpha: Int = 255,
    ) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(angleDeg)
        paintFill.shader = null
        paintFill.color = fillColor
        paintFill.alpha = alpha
        canvas.drawOval(RectF(-rx, -ry, rx, ry), paintFill)
        canvas.restore()
    }

    // MARK: - Starfish (translate(60,1740) opacity 0.75)

    private fun drawStarfish(canvas: Canvas) {
        canvas.save()
        canvas.translate(60f, 1740f)
        paintFill.alpha = (255 * 0.75f).toInt()
        val star = Path().apply {
            moveTo(0f, -20f)
            lineTo(4f, -6f)
            lineTo(18f, -6f)
            lineTo(7f, 2f)
            lineTo(12f, 16f)
            lineTo(0f, 8f)
            lineTo(-12f, 16f)
            lineTo(-7f, 2f)
            lineTo(-18f, -6f)
            lineTo(-4f, -6f)
            close()
        }
        paintFill.color = paleYellow
        canvas.drawPath(star, paintFill)

        paintFill.color = peachSun
        canvas.drawOval(RectF(-6f, -6f, 6f, 6f), paintFill)

        paintFill.color = Color.argb((255 * 0.8f).toInt(), Color.red(coral), Color.green(coral), Color.blue(coral))
        val dots = listOf(0f to -10f, -8f to -3f, 8f to -3f, -6f to 8f, 6f to 8f)
        for ((dx, dy) in dots) {
            canvas.drawOval(RectF(dx - 1.5f, dy - 1.5f, dx + 1.5f, dy + 1.5f), paintFill)
        }
        paintFill.alpha = 255
        canvas.restore()
    }

    // MARK: - Shell (translate(520,1755) opacity 0.7)

    private fun drawShell(canvas: Canvas) {
        canvas.save()
        canvas.translate(520f, 1755f)
        paintFill.alpha = (255 * 0.7f).toInt()

        val s1 = Path().apply {
            moveTo(0f, 0f)
            lineTo(-15f, -20f)
            quadTo(-10f, -25f, 0f, -22f)
            quadTo(10f, -25f, 15f, -20f)
            close()
        }
        paintFill.color = pinkSoft
        canvas.drawPath(s1, paintFill)

        val s2 = Path().apply {
            moveTo(0f, -2f)
            lineTo(-12f, -18f)
            quadTo(-8f, -22f, 0f, -20f)
            quadTo(8f, -22f, 12f, -18f)
            close()
        }
        paintFill.color = redInner
        canvas.drawPath(s2, paintFill)

        paintStroke.color = Color.argb((255 * 0.3f).toInt(), Color.red(pinkDeep), Color.green(pinkDeep), Color.blue(pinkDeep))
        paintStroke.strokeWidth = 1f
        paintStroke.style = Paint.Style.STROKE
        val ridges = listOf(
            -10f to -15f,
            -5f to -17f,
            5f to -17f,
            10f to -15f,
        )
        for ((ax, ay) in ridges) {
            canvas.drawLine(ax, ay, 0f, 0f, paintStroke)
        }

        canvas.save()
        canvas.translate(35f, 5f)
        val ss1 = Path().apply {
            moveTo(0f, 0f)
            lineTo(-10f, -15f)
            quadTo(-7f, -18f, 0f, -16f)
            quadTo(7f, -18f, 10f, -15f)
            close()
        }
        paintFill.color = Color.argb((255 * 0.8f).toInt(), Color.red(mintGreen), Color.green(mintGreen), Color.blue(mintGreen))
        canvas.drawPath(ss1, paintFill)

        val ss2 = Path().apply {
            moveTo(0f, -1f)
            lineTo(-8f, -13f)
            quadTo(-5f, -15f, 0f, -14f)
            quadTo(5f, -15f, 8f, -13f)
            close()
        }
        paintFill.color = Color.argb((255 * 0.6f).toInt(), Color.red(skyBlue), Color.green(skyBlue), Color.blue(skyBlue))
        canvas.drawPath(ss2, paintFill)
        canvas.restore()

        paintFill.alpha = 255
        paintStroke.alpha = 255
        canvas.restore()
    }

    // MARK: - Sprinkles

    private fun drawSprinkles(canvas: Canvas) {
        canvas.save()
        data class Sprinkle(val x: Float, val y: Float, val r: Float, val c: Int, val a: Float)
        val circles = listOf(
            Sprinkle(150f, 480f, 3f, skyBlue, 0.5f),
            Sprinkle(450f, 650f, 3f, mintGreen, 0.5f),
            Sprinkle(100f, 1050f, 3f, paleYellow, 0.5f),
            Sprinkle(500f, 1250f, 3f, pinkSoft, 0.5f),
        )
        for ((x, y, r, c, a) in circles) {
            paintFill.shader = null
            paintFill.color = Color.argb((255 * a).toInt(), Color.red(c), Color.green(c), Color.blue(c))
            canvas.drawOval(RectF(x - r, y - r, x + r, y + r), paintFill)
        }
        paintFill.alpha = 255
        canvas.restore()
    }

    // MARK: - Tiny vector emojis

    private fun drawSmallVectorEmojis(
        canvas: Canvas,
        ls: Float,
        useBorderAdjacentSmallDecor: Boolean,
        anchors: Tiny4,
    ) {
        val top = anchors.top
        val right = anchors.right
        val left = anchors.left
        val bottom = anchors.bottom

        if (useBorderAdjacentSmallDecor) {
            canvas.save()
            paintStroke.alpha = (255 * 0.4f).toInt()
            paintStroke.color = skyBlue
            paintStroke.strokeWidth = 2f
            paintStroke.strokeCap = Paint.Cap.ROUND
            paintStroke.style = Paint.Style.STROKE
            val p = Path().apply {
                moveTo(top.x - 10f, top.y)
                quadTo(top.x - 6f, top.y - 6f, top.x, top.y - 8f)
                quadTo(top.x + 6f, top.y + 6f, top.x + 10f, top.y)
                moveTo(top.x - 15f, top.y + 12f)
                quadTo(top.x - 10f, top.y + 7f, top.x, top.y + 5f)
                quadTo(top.x + 10f, top.y + 18f, top.x + 15f, top.y + 12f)
            }
            canvas.drawPath(p, paintStroke)
            canvas.restore()

            canvas.save()
            paintFill.shader = null
            paintFill.color = paleYellow
            paintFill.alpha = (255 * 0.35f).toInt()
            canvas.drawOval(RectF(right.x - 8f, right.y - 8f, right.x + 8f, right.y + 8f), paintFill)
            paintStroke.alpha = (255 * 0.9f).toInt()
            paintStroke.color = peachSun
            paintStroke.strokeWidth = 2f
            var a = 0.0
            while (a < 2 * PI) {
                val ang = a.toFloat()
                val r1 = 8f
                val r2 = 14f
                canvas.drawLine(
                    right.x + cos(ang) * r1,
                    right.y + sin(ang) * r1,
                    right.x + cos(ang) * r2,
                    right.y + sin(ang) * r2,
                    paintStroke,
                )
                a += PI / 4
            }
            canvas.restore()

            canvas.save()
            paintStroke.alpha = (255 * 0.4f).toInt()
            paintStroke.color = skyBlue
            paintStroke.strokeWidth = 2f
            val umb = Path().apply {
                moveTo(left.x - 10f, left.y - 15f)
                quadTo(left.x, left.y - 30f, left.x + 10f, left.y - 15f)
            }
            canvas.drawPath(umb, paintStroke)
            canvas.drawLine(left.x, left.y - 15f, left.x, left.y, paintStroke)
            paintFill.color = paleYellow
            paintFill.alpha = (255 * 0.35f).toInt()
            canvas.drawOval(RectF(left.x - 10f, left.y, left.x + 10f, left.y + 10f), paintFill)
            canvas.restore()

            canvas.save()
            paintFill.color = pinkSoft
            paintFill.alpha = (255 * 0.3f).toInt()
            val flamingo = Path().apply {
                moveTo(bottom.x - 5f, bottom.y)
                quadTo(bottom.x, bottom.y - 5f, bottom.x + 5f, bottom.y)
                quadTo(bottom.x, bottom.y + 15f, bottom.x - 10f, bottom.y + 10f)
                quadTo(bottom.x - 15f, bottom.y + 15f, bottom.x - 5f, bottom.y)
                close()
            }
            canvas.drawPath(flamingo, paintFill)
            paintStroke.color = creamInner
            paintStroke.alpha = 255
            paintStroke.strokeWidth = 2f
            canvas.drawLine(bottom.x - 5f, bottom.y, bottom.x + 5f, bottom.y, paintStroke)
            canvas.restore()
        } else {
            canvas.save()
            paintStroke.alpha = (255 * 0.4f).toInt()
            paintStroke.color = skyBlue
            paintStroke.strokeWidth = 2f
            val p = Path().apply {
                moveTo(410f, 490f)
                quadTo(414f, 484f, 420f, 482f)
                quadTo(426f, 496f, 430f, 490f)
                moveTo(405f, 502f)
                quadTo(410f, 497f, 420f, 495f)
                quadTo(430f, 508f, 435f, 502f)
            }
            canvas.drawPath(p, paintStroke)
            canvas.restore()

            canvas.save()
            paintFill.color = paleYellow
            paintFill.alpha = (255 * 0.35f).toInt()
            canvas.drawOval(RectF(512f, 880f - 8f, 512f + 16f, 880f + 8f), paintFill)
            paintStroke.color = peachSun
            paintStroke.alpha = (255 * 0.9f).toInt()
            paintStroke.strokeWidth = 2f
            var a = 0.0
            while (a < 2 * PI) {
                val ang = a.toFloat()
                canvas.drawLine(
                    520f + cos(ang) * 8f,
                    880f + sin(ang) * 8f,
                    520f + cos(ang) * 14f,
                    880f + sin(ang) * 14f,
                    paintStroke,
                )
                a += PI / 4
            }
            canvas.restore()

            canvas.save()
            paintStroke.alpha = (255 * 0.4f).toInt()
            paintStroke.color = skyBlue
            val umb = Path().apply {
                moveTo(55f, 1305f)
                quadTo(65f, 1290f, 75f, 1305f)
            }
            canvas.drawPath(umb, paintStroke)
            canvas.drawLine(65f, 1305f, 65f, 1320f, paintStroke)
            paintFill.color = paleYellow
            paintFill.alpha = (255 * 0.35f).toInt()
            canvas.drawOval(RectF(55f, 1320f, 75f, 1330f), paintFill)
            canvas.restore()

            canvas.save()
            paintFill.color = pinkSoft
            paintFill.alpha = (255 * 0.3f).toInt()
            val flamingo = Path().apply {
                moveTo(175f, 1590f)
                quadTo(180f, 1585f, 185f, 1590f)
                quadTo(180f, 1605f, 170f, 1600f)
                quadTo(165f, 1605f, 175f, 1590f)
                close()
            }
            canvas.drawPath(flamingo, paintFill)
            paintStroke.color = creamInner
            paintStroke.alpha = 255
            paintStroke.strokeWidth = 2f
            canvas.drawLine(175f, 1590f, 185f, 1590f, paintStroke)
            canvas.restore()
        }
    }

    // MARK: - Body wave accent

    private fun drawSummerBodyWave(canvas: Canvas, cx: Float, cy: Float) {
        canvas.save()
        paintStroke.alpha = (255 * 0.32f).toInt()
        paintStroke.color = skyBlue
        paintStroke.strokeWidth = 2.5f
        paintStroke.strokeCap = Paint.Cap.ROUND
        paintStroke.style = Paint.Style.STROKE
        val p = Path().apply {
            moveTo(cx - 14f, cy)
            quadTo(cx - 8f, cy - 6f, cx, cy - 8f)
            quadTo(cx + 8f, cy + 6f, cx + 14f, cy)
            moveTo(cx - 12f, cy + 10f)
            quadTo(cx - 6f, cy + 5f, cx, cy + 3f)
            quadTo(cx + 6f, cy + 15f, cx + 12f, cy + 10f)
        }
        canvas.drawPath(p, paintStroke)
        paintStroke.alpha = 255
        canvas.restore()
    }

    // MARK: - Footer

    private fun drawFooterText(
        canvas: Canvas,
        width: Float,
        height: Float,
        sx: Float,
        sy: Float,
        ls: Float,
        slotRects: List<RectF>?,
    ) {
        val label = "Summer Memories"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f * ls
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = Color.argb((255 * 0.7f).toInt(), Color.red(skyBlue), Color.green(skyBlue), Color.blue(skyBlue))
            textAlign = Paint.Align.LEFT
        }
        val textWidth = textPaint.measureText(label)
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val bounds = Rect()
        textPaint.getTextBounds(label, 0, label.length, bounds)
        val measuredH = bounds.height().toFloat().coerceAtLeast(textHeight)

        val startX = (width - textWidth) / 2f
        val fallbackTopY = 1770f * sy - measuredH
        val topY = SeasonDecorAnchors.memoriesBaselineY(height, slotRects, measuredH, fallbackTopY)
        val baselineY = topY - fm.ascent

        canvas.drawText(label, startX, baselineY, textPaint)

        val waveScale = 0.85f * ls
        val waveCenterX = startX + textWidth + 6f * ls
        val waveCenterY = topY + measuredH * 0.65f

        canvas.save()
        paintStroke.alpha = (255 * 0.7f).toInt()
        paintStroke.color = skyBlue
        paintStroke.strokeWidth = 1.6f * waveScale
        paintStroke.strokeCap = Paint.Cap.ROUND
        paintStroke.style = Paint.Style.STROKE
        val wp = Path().apply {
            moveTo(waveCenterX - 6f * ls, waveCenterY)
            quadTo(waveCenterX - 3f * ls, waveCenterY, waveCenterX, waveCenterY - 4f * ls)
            quadTo(waveCenterX + 3f * ls, waveCenterY + 4f * ls, waveCenterX + 6f * ls, waveCenterY)
        }
        canvas.drawPath(wp, paintStroke)
        paintStroke.alpha = 255
        canvas.restore()
    }

    private fun strokeWavePath(canvas: Canvas, path: Path, color: Int, width: Float, opacity: Float) {
        canvas.save()
        paintStroke.color = color
        paintStroke.strokeWidth = width
        paintStroke.strokeCap = Paint.Cap.ROUND
        paintStroke.style = Paint.Style.STROKE
        paintStroke.alpha = (255 * opacity).toInt()
        canvas.drawPath(path, paintStroke)
        paintStroke.alpha = 255
        canvas.restore()
    }

    // SVG-style quadratic paths (T = reflected control)

    private fun wavePath00(): Path = Path().apply {
        moveTo(0f, 0f)
        quadTo(10f, -5f, 20f, 0f)
        quadTo(30f, 5f, 40f, 0f)
    }

    private fun wavePath58(): Path = Path().apply {
        moveTo(5f, 8f)
        quadTo(15f, 3f, 25f, 8f)
        quadTo(35f, 13f, 45f, 8f)
    }

    private fun wavePath1014(): Path = Path().apply {
        moveTo(10f, 14f)
        quadTo(20f, 9f, 30f, 14f)
        quadTo(40f, 19f, 50f, 14f)
    }

    private fun wavePathBig1(): Path = Path().apply {
        moveTo(0f, 0f)
        quadTo(15f, -8f, 30f, 0f)
        quadTo(45f, 8f, 60f, 0f)
        quadTo(75f, -8f, 90f, 0f)
    }

    private fun wavePathBig2(): Path = Path().apply {
        moveTo(5f, 12f)
        quadTo(20f, 4f, 35f, 12f)
        quadTo(50f, 20f, 65f, 12f)
        quadTo(80f, 4f, 95f, 12f)
    }

    private fun wavePathBig3(): Path = Path().apply {
        moveTo(10f, 22f)
        quadTo(25f, 14f, 40f, 22f)
        quadTo(55f, 30f, 70f, 22f)
        quadTo(85f, 14f, 100f, 22f)
    }
}
