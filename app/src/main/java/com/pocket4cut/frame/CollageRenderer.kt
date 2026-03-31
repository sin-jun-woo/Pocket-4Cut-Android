package com.pocket4cut.frame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color as AColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.pocket4cut.core.util.AppFontCatalog
import com.pocket4cut.core.util.CollageExportMetrics
import com.pocket4cut.core.util.ColorRGB
import com.pocket4cut.frame.rendering.AutumnFrameVectorDecor
import com.pocket4cut.frame.rendering.SpringFrameVectorDecor
import com.pocket4cut.frame.rendering.SummerFrameVectorDecor
import com.pocket4cut.frame.rendering.WinterFrameVectorDecor
import com.pocket4cut.ui.designsystem.theme.Season
import kotlin.math.min
import kotlin.math.roundToInt

object CollageRenderer {

    private const val BRAND_TITLE = "Pocket 4Cut"
    private val brandTypeface: Typeface = Typeface.create("serif", Typeface.ITALIC)

    data class Input(
        val images: List<Bitmap>,
        val frameStyle: FrameStyle,
        val theme: FrameTheme,
        val overrideBackground: Color? = null,
        val overrideBackgroundImage: Bitmap? = null,
        val customDecorations: List<CustomFrameDecoration> = emptyList(),
        val customFrameDesign: CustomFrameDesign? = null,
        val filterId: FilterId = FilterId.ORIGINAL,
        val text: String? = null,
        val dateString: String? = null,
        val textFontSize: Float = 16f,
        val dateFontSize: Float = 16f,
        val textColorRGB: Long? = null,
        val captionFontName: String? = null,
        val context: Context? = null,
    )

    fun render(input: Input): Bitmap {
        val preferredOutputWidth = if (input.frameStyle.id == FrameLayoutId.FOUR_VERTICAL) {
            COLLAGE_CLASSIC_WIDTH_PX.toInt()
        } else {
            CollageExportMetrics.preferredOutputWidth.roundToInt()
        }
        val layout = collageLayoutForRender(
            input.frameStyle, input.theme,
            input.text, input.dateString,
            preferredOutputWidth,
        )
        return render(input, layout)
    }

    fun render(
        images: List<Bitmap>,
        frameStyle: FrameStyle,
        theme: FrameTheme,
        overrideBackground: Color?,
        filterId: FilterId,
        text: String?,
        dateString: String?,
        outputWidth: Int,
    ): Bitmap {
        val input = Input(
            images = images,
            frameStyle = frameStyle,
            theme = theme,
            overrideBackground = overrideBackground,
            filterId = filterId,
            text = text,
            dateString = dateString,
        )
        val layout = collageLayoutForRender(frameStyle, theme, text, dateString, outputWidth)
        return render(input, layout)
    }

    private fun render(input: Input, layout: CollageLayoutDimensions): Bitmap {
        val bitmap = Bitmap.createBitmap(
            layout.canvasWidth.roundToInt(),
            layout.canvasHeight.roundToInt(),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)

        val seasonHTML = input.customFrameDesign?.resolvedSeason
        val useSeasonBackdrop = seasonHTML != null && input.overrideBackgroundImage == null

        val outerCorner = if (useSeasonBackdrop) 0f else input.theme.cornerRadius * layout.scale
        val canvasRect = RectF(0f, 0f, layout.canvasWidth, layout.canvasHeight)

        // 1. Background
        canvas.save()
        if (outerCorner > 0f) {
            val path = Path().apply { addRoundRect(canvasRect, outerCorner, outerCorner, Path.Direction.CW) }
            canvas.clipPath(path)
        }
        if (input.overrideBackgroundImage != null) {
            drawAspectFill(canvas, input.overrideBackgroundImage, canvasRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else if (seasonHTML != null) {
            SeasonHTMLFrameStyle.drawHTMLBackdrop(seasonHTML, canvas, layout.canvasWidth, layout.canvasHeight)
        } else {
            val bgColor = input.overrideBackground ?: input.theme.background
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor.toArgb() }
            canvas.drawRect(canvasRect, bgPaint)
        }
        canvas.restore()

        // 2. Outer border
        val borderColor = if (seasonHTML != null) {
            (0xFF000000 or SeasonHTMLFrameStyle.outerStrokeHex(seasonHTML)).toInt()
        } else {
            input.theme.border.toArgb()
        }
        val borderW = if (seasonHTML != null) {
            SeasonHTMLFrameStyle.outerBorderWidthPoints(seasonHTML) * layout.scale
        } else {
            input.theme.borderWidth * layout.scale
        }
        if (borderW > 0f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderColor
                style = Paint.Style.STROKE
                strokeWidth = borderW
            }
            val half = borderW / 2f
            val borderRect = RectF(half, half, layout.canvasWidth - half, layout.canvasHeight - half)
            val r = (outerCorner - half).coerceAtLeast(0f)
            if (r > 0f) canvas.drawRoundRect(borderRect, r, r, borderPaint)
            else canvas.drawRect(borderRect, borderPaint)
        }

        // 3. Brand title
        val effectiveBgColor = when {
            seasonHTML != null -> Color((0xFF000000 or SeasonHTMLFrameStyle.baseHex(seasonHTML)).toInt())
            input.overrideBackground != null -> input.overrideBackground
            input.customFrameDesign != null -> input.customFrameDesign.resolvedFillColor
            else -> input.theme.background
        }
        drawBrandTitle(
            canvas = canvas,
            scale = layout.scale,
            headerArea = layout.headerArea,
            bgColor = effectiveBgColor,
            hasBackgroundImage = input.overrideBackgroundImage != null,
            isSeason = seasonHTML != null,
        )

        // 4. Photo slots
        val colorFilter = FilterDefs.colorFilter(input.filterId)
        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.colorFilter = colorFilter
        }
        val slotBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.argb(28, 0, 0, 0) }

        for ((i, cell) in layout.cells.withIndex()) {
            val cellCorner = if (useSeasonBackdrop) {
                cell.width() * SeasonHTMLFrameStyle.CELL_CORNER_RATIO
            } else 0f

            canvas.save()
            if (cellCorner > 0f) {
                val clipPath = Path().apply { addRoundRect(cell, cellCorner, cellCorner, Path.Direction.CW) }
                canvas.clipPath(clipPath)
            }
            canvas.drawRect(cell, slotBg)
            input.images.getOrNull(i)?.let { drawAspectFill(canvas, it, cell, imgPaint) }
            canvas.restore()
        }

        // 5. Season cell borders (dashed)
        if (seasonHTML != null) {
            for (cell in layout.cells) {
                strokeSeasonCellBorder(canvas, cell, seasonHTML, layout.scale)
            }
        }

        // 6. Season vector decorations
        if (seasonHTML != null) {
            val slotRects = layout.cells.map { RectF(it) }
            when (seasonHTML) {
                Season.SPRING -> SpringFrameVectorDecor.draw(canvas, layout.canvasWidth, layout.canvasHeight, true, slotRects)
                Season.SUMMER -> SummerFrameVectorDecor.draw(canvas, layout.canvasWidth, layout.canvasHeight, true, slotRects)
                Season.AUTUMN -> AutumnFrameVectorDecor.draw(canvas, layout.canvasWidth, layout.canvasHeight, true, slotRects)
                Season.WINTER -> WinterFrameVectorDecor.draw(canvas, layout.canvasWidth, layout.canvasHeight, true, slotRects)
            }
        }

        // 7. Custom decorations
        val deco = input.customFrameDesign?.decorations ?: input.customDecorations
        if (deco.isNotEmpty()) {
            drawCustomDecorations(canvas, deco, layout.canvasWidth, layout.canvasHeight, input.context)
        }

        // 8. Overlay text
        layout.textArea?.let { textArea ->
            drawOverlayText(canvas, textArea, layout.scale, effectiveBgColor, input)
        }

        return bitmap
    }

    private fun strokeSeasonCellBorder(canvas: Canvas, cell: RectF, season: Season, scale: Float) {
        val radius = cell.width() * SeasonHTMLFrameStyle.CELL_CORNER_RATIO
        val strokeHex = SeasonHTMLFrameStyle.cellStrokeHex(season)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (0xFF000000 or strokeHex).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
            strokeCap = Paint.Cap.ROUND
            pathEffect = DashPathEffect(floatArrayOf(8f * scale, 6f * scale), 0f)
        }
        canvas.drawRoundRect(cell, radius, radius, paint)
    }

    private fun drawCustomDecorations(
        canvas: Canvas,
        decorations: List<CustomFrameDecoration>,
        canvasW: Float,
        canvasH: Float,
        context: Context?,
    ) {
        val base = min(canvasW, canvasH)
        for (dec in decorations) {
            val cx = dec.position.x * canvasW
            val cy = dec.position.y * canvasH
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(Math.toDegrees(dec.rotationRadians.toDouble()).toFloat())
            when (val kind = dec.kind) {
                is CustomFrameDecoration.Kind.Text -> {
                    val pt = maxOf(base * kind.fontScale * dec.scale, 4f)
                    val tf = if (context != null) AppFontCatalog.typeface(context, dec.fontName) else Typeface.DEFAULT
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = (0xFF000000 or (kind.textColorARGB and 0xFFFFFF)).toInt()
                        textSize = pt
                        typeface = tf
                        textAlign = Paint.Align.CENTER
                    }
                    val fm = paint.fontMetrics
                    val y = -(fm.ascent + fm.descent) / 2f
                    canvas.drawText(kind.content, 0f, y, paint)
                }
                is CustomFrameDecoration.Kind.Emoji -> {
                    val pt = maxOf(base * 0.11f * dec.scale, 10f)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = pt
                        textAlign = Paint.Align.CENTER
                    }
                    val fm = paint.fontMetrics
                    val y = -(fm.ascent + fm.descent) / 2f
                    canvas.drawText(kind.content, 0f, y, paint)
                }
                is CustomFrameDecoration.Kind.Sticker -> {
                    val palette = StickerPalette.fromAssetId(kind.assetId)
                    if (palette != null) {
                        val pt = maxOf(base * 0.12f * dec.scale, 12f)
                        val stickerColor = (0xFF000000 or (kind.colorRGB and 0xFFFFFF)).toInt()
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = pt
                            color = stickerColor
                            textAlign = Paint.Align.CENTER
                        }
                        val fm = paint.fontMetrics
                        val y = -(fm.ascent + fm.descent) / 2f
                        canvas.drawText(palette.displayName, 0f, y, paint)
                    }
                }
            }
            canvas.restore()
        }
    }

    private fun drawBrandTitle(
        canvas: Canvas,
        scale: Float,
        headerArea: RectF,
        bgColor: Color,
        hasBackgroundImage: Boolean,
        isSeason: Boolean,
    ) {
        val textColor = when {
            hasBackgroundImage -> AColor.WHITE
            isSeason -> AColor.argb((0.88f * 255).toInt(), 0, 0, 0)
            isDark(bgColor) -> AColor.WHITE
            else -> AColor.BLACK
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = BRAND_TITLE_TEXT_PT * scale
            typeface = brandTypeface
            textAlign = Paint.Align.CENTER
        }
        val fm = paint.fontMetrics
        val y = headerArea.top + (headerArea.height() - fm.ascent - fm.descent) / 2f
        canvas.drawText(BRAND_TITLE, headerArea.centerX(), y, paint)
    }

    private fun drawOverlayText(
        canvas: Canvas,
        area: RectF,
        scale: Float,
        bgColor: Color,
        input: Input,
    ) {
        val text = input.text
        val dateString = input.dateString
        val hasText = !text.isNullOrBlank() || !dateString.isNullOrBlank()
        if (!hasText) return

        val textColor = if (input.textColorRGB != null) {
            (0xFF000000 or (input.textColorRGB and 0xFFFFFF)).toInt()
        } else if (input.overrideBackgroundImage != null) {
            AColor.WHITE
        } else if (isDark(bgColor)) {
            AColor.WHITE
        } else {
            AColor.BLACK
        }

        val captionTf = if (input.context != null && input.captionFontName != null) {
            AppFontCatalog.typeface(input.context, input.captionFontName)
        } else {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = maxOf(1f, input.textFontSize) * scale
            typeface = captionTf
            textAlign = Paint.Align.LEFT
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = maxOf(1f, input.dateFontSize) * scale
            typeface = captionTf
            textAlign = Paint.Align.LEFT
        }
        val sepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = minOf(textPaint.textSize, datePaint.textSize)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val separator = "  ·  "
        data class Chunk(val str: String, val paint: Paint, val width: Float, val height: Float)

        val chunks = mutableListOf<Chunk>()
        if (!text.isNullOrBlank()) {
            val w = textPaint.measureText(text)
            val fm = textPaint.fontMetrics
            chunks.add(Chunk(text, textPaint, w, fm.descent - fm.ascent))
        }
        if (!dateString.isNullOrBlank()) {
            if (chunks.isNotEmpty()) {
                val w = sepPaint.measureText(separator)
                val fm = sepPaint.fontMetrics
                chunks.add(Chunk(separator, sepPaint, w, fm.descent - fm.ascent))
            }
            val w = datePaint.measureText(dateString)
            val fm = datePaint.fontMetrics
            chunks.add(Chunk(dateString, datePaint, w, fm.descent - fm.ascent))
        }

        val totalW = chunks.sumOf { it.width.toDouble() }.toFloat()
        val maxH = chunks.maxOfOrNull { it.height } ?: 0f
        var drawX = area.centerX() - totalW / 2f
        val baseY = area.centerY()

        for (chunk in chunks) {
            val fm = chunk.paint.fontMetrics
            val y = baseY - (fm.ascent + fm.descent) / 2f
            canvas.drawText(chunk.str, drawX, y, chunk.paint)
            drawX += chunk.width
        }
    }

    private fun drawAspectFill(canvas: Canvas, bitmap: Bitmap, dst: RectF, paint: Paint) {
        canvas.save()
        canvas.clipRect(dst)
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val s = maxOf(dst.width() / bw, dst.height() / bh)
        val sw = bw * s
        val sh = bh * s
        val l = dst.left + (dst.width() - sw) / 2f
        val t = dst.top + (dst.height() - sh) / 2f
        canvas.drawBitmap(bitmap, null, RectF(l, t, l + sw, t + sh), paint)
        canvas.restore()
    }

    private fun isDark(color: Color): Boolean {
        val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
        return luminance <= 0.5f
    }
}
