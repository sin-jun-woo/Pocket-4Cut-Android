package com.pocket4cut.frame.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Android Canvas 포팅본: iOS `SpringFrameVectorDecor` (600×1800 ref) CGContext 드로잉과 동등.
 * 배경/슬롯/외곽선은 호출 측에서 처리; 여기서는 벡터 장식만 그린다.
 */
object SpringFrameVectorDecor {

    private const val REF_W = 600f
    private const val REF_H = 1800f

    private val pinkSoft: Int = Color.parseColor("#FFB4D6")
    private val pinkDeep: Int = Color.parseColor("#FF6B9D")
    private val yellowCream: Int = Color.parseColor("#FFF4C4")

    /** iOS fillCherryPetal 그라데이션 (1, 0.82, 0.89) → (1, 0.706, 0.839) */
    private val cherryPetalGradientStart: Int = Color.rgb(255, 209, 227)
    private val cherryPetalGradientEnd: Int = Color.rgb(255, 180, 214)

    /** iOS 작은 별 등: (0.769, 0.898, 1) */
    private val skyBlue: Int = Color.rgb(196, 228, 255)

    /** iOS 보조 핑크 (1, 0.82, 0.89) */
    private val pinkLight: Int = Color.rgb(255, 209, 227)

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        useBorderAdjacentSmallDecor: Boolean = true,
        slotRects: List<RectF>? = null,
    ) {
        val scale = min(width / REF_W, height / REF_H)
        val sx = scale
        val sy = scale
        val ls = scale
        val tx = (width - REF_W * scale) * 0.5f
        val ty = (height - REF_H * scale) * 0.5f

        canvas.save()
        canvas.translate(tx, ty)
        canvas.scale(sx, sy)

        drawCherryCluster6(
            canvas = canvas,
            opacity = 0.6f,
            centers = listOf(80f to 30f, 75f to 35f, 85f to 35f, 75f to 25f, 85f to 25f),
            core = 80f to 30f,
            coreR = 4f,
            petalR = 8f,
        )
        drawCherryCluster6(
            canvas = canvas,
            opacity = 0.6f,
            centers = listOf(520f to 30f, 515f to 35f, 525f to 35f, 515f to 25f, 525f to 25f),
            core = 520f to 30f,
            coreR = 4f,
            petalR = 8f,
        )

        if (!useBorderAdjacentSmallDecor) {
            drawButterflyGroup(canvas)
            drawSunflowerGroup(canvas)
            drawStarGroup(canvas)
        }

        drawSmallCherrySolid(
            canvas = canvas,
            opacity = 0.5f,
            petalR = 6f,
            coreR = 3f,
            core = 520f to 1760f,
            coreFill = yellowCream,
            petal = pinkSoft,
            centers = listOf(520f to 1760f, 516f to 1764f, 524f to 1764f, 516f to 1756f, 524f to 1756f),
        )
        drawSmallCherrySolid(
            canvas = canvas,
            opacity = 0.5f,
            petalR = 5f,
            coreR = 2f,
            core = 480f to 1770f,
            coreFill = yellowCream,
            petal = pinkLight,
            centers = listOf(480f to 1770f, 477f to 1773f, 483f to 1773f, 477f to 1767f, 483f to 1767f),
        )

        if (!useBorderAdjacentSmallDecor) {
            drawSparkleDots(canvas)
        }

        if (useBorderAdjacentSmallDecor) {
            val bounds = SeasonDecorAnchors.refBounds(slotRects, width, height, REF_W, REF_H)
            val anchors = SeasonDecorAnchors.tiny3(bounds, REF_W, REF_H)
            val body = SeasonDecorAnchors.bodyAccent(bounds, REF_W, REF_H)

            drawVectorSparkleStar(
                canvas = canvas,
                x = body.point.x,
                y = body.point.y,
                outer = 12f,
                fillRgb = pinkSoft,
                strokeAlpha = 0.25f,
            )
            drawVectorSparkleStar(
                canvas = canvas,
                x = anchors.top.x,
                y = anchors.top.y,
                outer = 9f,
                fillRgb = yellowCream,
                strokeAlpha = 0.35f,
            )
            drawVectorSparkleCross(
                canvas = canvas,
                x = anchors.right.x,
                y = anchors.right.y,
                arm = 10f,
                fillRgb = pinkSoft,
                alpha = 0.35f,
            )
            drawVectorSparkleStar(
                canvas = canvas,
                x = anchors.left.x,
                y = anchors.left.y,
                outer = 7f,
                fillRgb = skyBlue,
                strokeAlpha = 0.35f,
            )
        } else {
            drawVectorSparkleStar(canvas, 420f, 490f, 9f, yellowCream, 0.35f)
            drawVectorSparkleCross(canvas, 530f, 880f, 10f, pinkSoft, 0.35f)
            drawVectorSparkleStar(canvas, 70f, 1320f, 7f, skyBlue, 0.35f)
        }

        canvas.restore()

        drawFooterLine(canvas, width, height, sx, sy, ls, tx, ty, slotRects)
    }

    private fun drawCherryCluster6(
        canvas: Canvas,
        opacity: Float,
        centers: List<Pair<Float, Float>>,
        core: Pair<Float, Float>,
        coreR: Float,
        petalR: Float,
    ) {
        canvas.save()
        val p = petalPaint()
        p.alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
        for ((cx, cy) in centers) {
            fillCherryPetal(canvas, p, cx, cy, petalR)
        }
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = yellowCream
        p.alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
        val (crx, cry) = core
        canvas.drawOval(RectF(crx - coreR, cry - coreR, crx + coreR, cry + coreR), p)
        canvas.restore()
    }

    private fun fillCherryPetal(canvas: Canvas, paint: Paint, cx: Float, cy: Float, radius: Float) {
        paint.shader = RadialGradient(
            cx,
            cy,
            radius,
            intArrayOf(cherryPetalGradientStart, cherryPetalGradientEnd),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null
    }

    private fun drawSmallCherrySolid(
        canvas: Canvas,
        opacity: Float,
        petalR: Float,
        coreR: Float,
        core: Pair<Float, Float>,
        coreFill: Int,
        petal: Int,
        centers: List<Pair<Float, Float>>,
    ) {
        canvas.save()
        val p = petalPaint()
        p.alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
        p.shader = null
        for ((cx, cy) in centers) {
            p.color = petal
            canvas.drawOval(RectF(cx - petalR, cy - petalR, cx + petalR, cy + petalR), p)
        }
        p.color = coreFill
        val (crx, cry) = core
        canvas.drawOval(RectF(crx - coreR, cry - coreR, crx + coreR, cry + coreR), p)
        canvas.restore()
    }

    private fun drawButterflyGroup(canvas: Canvas) {
        canvas.save()
        canvas.translate(20f, 700f)
        val p = petalPaint()
        p.alpha = (0.7f * 255).roundToInt()

        fun fe(cx: Float, cy: Float, rx: Float, ry: Float, color: Int) {
            p.shader = null
            p.color = color
            canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), p)
        }

        fe(0f, 0f, 15f, 20f, argb(0.7f, 0.769f, 0.898f, 1f))
        fe(0f, 25f, 15f, 18f, argb(0.7f, 0.769f, 0.898f, 1f))
        fe(20f, 0f, 15f, 20f, argb(0.7f, 1f, 0.820f, 0.890f))
        fe(20f, 25f, 15f, 18f, argb(0.7f, 1f, 0.820f, 0.890f))
        fe(10f, 12f, 3f, 15f, argb(0.5f, 1f, 0.42f, 0.616f))

        val stroke = argb(0.5f, 1f, 0.42f, 0.616f)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = stroke
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            alpha = (0.7f * 255).roundToInt()
        }

        val p1 = Path().apply {
            moveTo(10f, 5f)
            quadTo(8f, 0f, 7f, -2f)
        }
        canvas.drawPath(p1, strokePaint)

        val p2 = Path().apply {
            moveTo(10f, 5f)
            quadTo(12f, 0f, 13f, -2f)
        }
        canvas.drawPath(p2, strokePaint)

        p.shader = null
        p.style = Paint.Style.FILL
        p.color = stroke
        canvas.drawOval(RectF(5f, -4f, 9f, 0f), p)
        canvas.drawOval(RectF(11f, -4f, 15f, 0f), p)

        canvas.restore()
    }

    private fun drawSunflowerGroup(canvas: Canvas) {
        canvas.save()
        canvas.translate(560f, 1100f)
        val p = petalPaint()
        p.alpha = (0.8f * 255).roundToInt()
        p.shader = null

        val petalCol = Color.rgb(255, 244, 196)
        val c1 = argb(0.9f, 1f, 0.722f, 0.471f)
        val c2 = argb(0.7f, 1f, 0.604f, 0.353f)

        for (i in 0 until 8) {
            val angle = i * Math.PI.toFloat() / 4f
            canvas.save()
            canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
            p.color = petalCol
            canvas.drawOval(RectF(-8f, -34f, 8f, -2f), p)
            canvas.restore()
        }

        p.color = c1
        canvas.drawOval(RectF(-12f, -12f, 12f, 12f), p)
        p.color = c2
        canvas.drawOval(RectF(-10f, -10f, 10f, 10f), p)

        canvas.restore()
    }

    private fun drawStarGroup(canvas: Canvas) {
        canvas.save()
        canvas.translate(60f, 1750f)
        val p = petalPaint()
        p.alpha = (0.7f * 255).roundToInt()
        p.shader = null
        p.style = Paint.Style.FILL

        val outer = Path().apply {
            moveTo(0f, -12f)
            lineTo(3f, -3f)
            lineTo(12f, -3f)
            lineTo(5f, 3f)
            lineTo(8f, 12f)
            lineTo(0f, 6f)
            lineTo(-8f, 12f)
            lineTo(-5f, 3f)
            lineTo(-12f, -3f)
            lineTo(-3f, -3f)
            close()
        }
        p.color = yellowCream
        canvas.drawPath(outer, p)

        val inner = Path().apply {
            moveTo(0f, -8f)
            lineTo(2f, -2f)
            lineTo(8f, -2f)
            lineTo(3f, 2f)
            lineTo(5f, 8f)
            lineTo(0f, 4f)
            lineTo(-5f, 8f)
            lineTo(-3f, 2f)
            lineTo(-8f, -2f)
            lineTo(-2f, -2f)
            close()
        }
        p.color = pinkLight
        canvas.drawPath(inner, p)

        canvas.restore()
    }

    private fun drawSparkleDots(canvas: Canvas) {
        val p = petalPaint()
        p.shader = null
        p.style = Paint.Style.FILL
        val dots = listOf(
            Triple(150f, 480f, withAlpha(yellowCream, 0.6f)),
            Triple(450f, 650f, withAlpha(pinkLight, 0.6f)),
            Triple(100f, 1050f, withAlpha(skyBlue, 0.6f)),
            Triple(500f, 1250f, withAlpha(yellowCream, 0.6f)),
        )
        for ((cx, cy, col) in dots) {
            p.color = col
            val r = 3f
            canvas.drawOval(RectF(cx - r, cy - r, cx + r, cy + r), p)
        }
    }

    private fun drawVectorSparkleStar(
        canvas: Canvas,
        x: Float,
        y: Float,
        outer: Float,
        fillRgb: Int,
        strokeAlpha: Float,
    ) {
        canvas.save()
        canvas.translate(x, y)
        val inner = outer * 0.45f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = i * Math.PI.toFloat() / 5f - Math.PI.toFloat() / 2f
            val px = cos(a) * r
            val py = sin(a) * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = withAlpha(fillRgb, 0.4f)
        }
        canvas.drawPath(path, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = withAlphaChannel(fillRgb, strokeAlpha)
        }
        canvas.drawPath(path, strokePaint)

        canvas.restore()
    }

    private fun drawVectorSparkleCross(
        canvas: Canvas,
        x: Float,
        y: Float,
        arm: Float,
        fillRgb: Int,
        alpha: Float,
    ) {
        canvas.save()
        canvas.translate(x, y)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillRgb
            this.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
        }
        val w = 1.5f
        for (rot in listOf(0f, Math.PI.toFloat() / 4f)) {
            canvas.save()
            canvas.rotate(Math.toDegrees(rot.toDouble()).toFloat())
            canvas.drawRect(-w / 2f, -arm, w / 2f, arm, p)
            canvas.drawRect(-arm, -w / 2f, arm, w / 2f, p)
            canvas.restore()
        }
        canvas.restore()
    }

    private fun drawFivePetalBlossom(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        petalRadius: Float,
        ringRadius: Float,
        innerDiscR: Float,
        dotR: Float,
    ) {
        val p = petalPaint()
        for (i in 0 until 5) {
            val a = i * 2f * Math.PI.toFloat() / 5f - Math.PI.toFloat() / 2f
            val px = cx + cos(a) * ringRadius
            val py = cy + sin(a) * ringRadius
            fillCherryPetal(canvas, p, px, py, petalRadius)
        }
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = withAlpha(pinkSoft, 0.95f)
        p.alpha = 255
        canvas.drawOval(RectF(cx - innerDiscR, cy - innerDiscR, cx + innerDiscR, cy + innerDiscR), p)
        p.color = yellowCream
        canvas.drawOval(RectF(cx - dotR, cy - dotR, cx + dotR, cy + dotR), p)
    }

    private fun drawFooterLine(
        canvas: Canvas,
        width: Float,
        height: Float,
        sx: Float,
        sy: Float,
        ls: Float,
        tx: Float,
        ty: Float,
        slotRects: List<RectF>?,
    ) {
        val label = "Spring Memories"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f * ls
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = withAlpha(pinkDeep, 0.6f)
        }
        val textWidth = textPaint.measureText(label)
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val gap = 8f * ls
        val flowerSlot = 28f * ls
        val totalW = textWidth + gap + flowerSlot
        val fallbackTopY = ty + 1770f * sy - textHeight
        val topY = SeasonDecorAnchors.memoriesBaselineY(
            canvasHeight = height,
            slotRects = slotRects,
            textHeight = textHeight,
            fallback = fallbackTopY,
        )
        val startX = (width - totalW) / 2f
        val textBaseline = topY - fm.ascent
        canvas.drawText(label, startX, textBaseline, textPaint)

        val flowerCx = startX + textWidth + gap + flowerSlot / 2f
        val flowerCy = topY + textHeight * 0.34f
        canvas.save()
        canvas.translate(flowerCx, flowerCy)
        canvas.scale(0.5f * ls, 0.5f * ls)
        drawFivePetalBlossom(canvas, 0f, 0f, 8f, 6f, 5f, 2.5f)
        canvas.restore()
    }

    private fun petalPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private fun argb(a: Float, r: Float, g: Float, b: Float): Int {
        return Color.argb(
            (a * 255).roundToInt().coerceIn(0, 255),
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255),
        )
    }

    private fun withAlpha(rgb: Int, alpha: Float): Int {
        val a = (alpha * 255).roundToInt().coerceIn(0, 255)
        return (rgb and 0x00FFFFFF) or (a shl 24)
    }

    private fun withAlphaChannel(rgb: Int, alpha: Float): Int {
        val base = Color.alpha(rgb) / 255f
        val a = (base * alpha * 255).roundToInt().coerceIn(0, 255)
        return (rgb and 0x00FFFFFF) or (a shl 24)
    }
}
