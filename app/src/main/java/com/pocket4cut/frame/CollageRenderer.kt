package com.pocket4cut.frame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.graphics.Color as AColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.pocket4cut.core.util.AppFontCatalog
import com.pocket4cut.core.util.ColorRGB
import com.pocket4cut.frame.occasion.OccasionTheme
import com.pocket4cut.frame.rendering.OccasionArtwork
import com.pocket4cut.frame.rendering.OccasionFramePainter
import com.pocket4cut.frame.rendering.SeasonalStickerArt
import com.pocket4cut.ui.designsystem.theme.Season
import kotlin.math.min
import kotlin.math.roundToInt

object CollageRenderer {

    private const val BRAND_TITLE = "Pocket 4Cut"
    private val brandTypeface: Typeface = Typeface.create("serif", Typeface.ITALIC)

    data class ProvidedSlotImage(
        val bitmap: Bitmap,
        val cropTransform: PhotoCropTransform = PhotoCropTransform(),
    )

    data class Input(
        val images: List<Bitmap>,
        val imageProvider: ((Int) -> Bitmap?)? = null,
        /** Export provider that can region-decode for the concrete output slot dimensions. */
        val slotImageProvider: ((index: Int, slotWidth: Int, slotHeight: Int) -> ProvidedSlotImage?)? = null,
        val recycleProvidedImages: Boolean = false,
        /** Slot-aligned crop data. Missing entries use the legacy centered aspect-fill. */
        val cropTransforms: List<PhotoCropTransform> = emptyList(),
        val frameStyle: FrameStyle,
        val theme: FrameTheme,
        val overrideBackground: Color? = null,
        val backgroundGradient: List<Color>? = null,
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
        val layoutVersion: Int = 2,
        val seasonalArt: SeasonalStickerArt.Sheet? = null,
        val occasionTheme: OccasionTheme? = null,
        val occasionArtwork: OccasionArtwork.Sheet? = null,
    )

    fun render(input: Input): Bitmap {
        val preferredOutputWidth = if (input.frameStyle.id == FrameLayoutId.FOUR_VERTICAL) {
            COLLAGE_CLASSIC_WIDTH_PX.toInt()
        } else {
            val logical = CollageLayoutMath.compute(
                input.frameStyle, input.theme, input.text, input.dateString, 390f, input.layoutVersion,
            )
            CollageOutputSize.forScene(logical).first
        }
        val layout = collageLayoutForRender(
            input.frameStyle, input.theme,
            input.text, input.dateString,
            preferredOutputWidth,
            input.layoutVersion,
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
        try {
            drawScene(canvas, input, layout)
            return bitmap
        } catch (error: Throwable) {
            bitmap.recycle()
            throw error
        }
    }

    /** Both Compose preview and JPEG export draw the same scene and layer order. */
    fun drawScene(canvas: Canvas, input: Input, layout: CollageLayoutDimensions) {
        val seasonHTML = input.customFrameDesign?.resolvedSeason
        val occasionTheme = input.occasionTheme
        require(seasonHTML == null || occasionTheme == null) {
            "Season and occasion frames cannot be rendered together"
        }
        val seasonalArt = seasonHTML?.let { season ->
            requireNotNull(input.seasonalArt?.takeIf { it.season == season }) {
                "Season artwork must be loaded before drawing $season"
            }
        }
        val occasionArtwork = occasionTheme?.let { theme ->
            requireNotNull(input.occasionArtwork?.takeIf { it.themeId == theme.id }) {
                "Occasion artwork must be loaded before drawing ${theme.id}"
            }
        }
        val useSeasonBackdrop = seasonHTML != null && input.overrideBackgroundImage == null
        val useOccasionBackdrop = occasionTheme != null

        val outerCorner = if (useSeasonBackdrop || useOccasionBackdrop) 0f else input.theme.cornerRadius * layout.scale
        val canvasRect = RectF(0f, 0f, layout.canvasWidth, layout.canvasHeight)

        // 1. Background
        canvas.save()
        if (outerCorner > 0f) {
            val path = Path().apply { addRoundRect(canvasRect, outerCorner, outerCorner, Path.Direction.CW) }
            canvas.clipPath(path)
        }
        if (occasionTheme != null) {
            OccasionFramePainter.drawBackdrop(canvas, layout, occasionTheme)
        } else if (input.overrideBackgroundImage != null) {
            drawAspectFill(canvas, input.overrideBackgroundImage, canvasRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else if (seasonHTML != null) {
            SeasonHTMLFrameStyle.drawHTMLBackdrop(seasonHTML, canvas, layout.canvasWidth, layout.canvasHeight)
        } else {
            val bgColor = input.overrideBackground ?: input.theme.background
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgColor.toArgb()
                input.backgroundGradient?.takeIf { it.size >= 2 }?.let { colors ->
                    shader = LinearGradient(0f, 0f, layout.canvasWidth, layout.canvasHeight,
                        colors.map { it.toArgb() }.toIntArray(), null, Shader.TileMode.CLAMP)
                }
            }
            canvas.drawRect(canvasRect, bgPaint)
        }
        canvas.restore()

        // 2. Photo slots
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
            val slotProvided = input.slotImageProvider?.invoke(
                i,
                cell.width().roundToInt().coerceAtLeast(1),
                cell.height().roundToInt().coerceAtLeast(1),
            )
            val legacyProvided = if (slotProvided == null) input.imageProvider?.invoke(i) else null
            val providedBitmap = slotProvided?.bitmap ?: legacyProvided
            try {
                (providedBitmap ?: input.images.getOrNull(i))?.let { image ->
                    drawAspectFill(
                        canvas = canvas,
                        bitmap = image,
                        dst = cell,
                        paint = imgPaint,
                        cropTransform = slotProvided?.cropTransform
                            ?: input.cropTransforms.getOrNull(i)
                            ?: PhotoCropTransform(),
                    )
                }
            } finally {
                if (input.recycleProvidedImages) providedBitmap?.recycle()
            }
            canvas.restore()
        }

        // 3. Outer border and season cell borders remain visible above photos.
        val borderColor = when {
            occasionTheme != null -> occasionTheme.inkColorArgb
            seasonHTML != null -> (0xFF000000 or SeasonHTMLFrameStyle.outerStrokeHex(seasonHTML)).toInt()
            else -> input.theme.border.toArgb()
        }
        val borderW = when {
            occasionTheme != null -> 0f
            seasonHTML != null -> SeasonHTMLFrameStyle.outerBorderWidthPoints(seasonHTML) * layout.scale
            else -> input.theme.borderWidth * layout.scale
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
        if (seasonHTML != null) {
            for (cell in layout.cells) {
                strokeSeasonCellBorder(canvas, cell, seasonHTML, layout.scale)
            }
        }
        occasionTheme?.let { OccasionFramePainter.drawPhotoBorders(canvas, layout, it) }

        // 4. Generated print illustrations, placed in the real layout's free margins.
        seasonalArt?.let { SeasonalStickerArt.draw(canvas, it, layout) }
        if (occasionTheme != null && occasionArtwork != null) {
            OccasionFramePainter.drawArtworkAndHeader(
                canvas = canvas,
                layout = layout,
                theme = occasionTheme,
                artwork = occasionArtwork,
                context = input.context,
            )
        }

        // 5. Brand, caption and date
        val effectiveBgColor = when {
            occasionTheme != null -> Color(occasionTheme.paperColorArgb)
            seasonHTML != null -> Color((0xFF000000 or SeasonHTMLFrameStyle.baseHex(seasonHTML)).toInt())
            input.overrideBackground != null -> input.overrideBackground
            input.customFrameDesign != null -> input.customFrameDesign.resolvedFillColor
            else -> input.theme.background
        }
        if (occasionTheme == null) {
            drawBrandTitle(
                canvas = canvas,
                scale = layout.scale,
                headerArea = layout.headerArea,
                bgColor = effectiveBgColor,
                hasBackgroundImage = input.overrideBackgroundImage != null,
                isSeason = seasonHTML != null,
            )
        }
        layout.textArea?.let { textArea ->
            drawOverlayText(canvas, textArea, layout.scale, effectiveBgColor, input)
        }

        // 6. User decorations are the topmost layer.
        val deco = input.customFrameDesign?.decorations ?: input.customDecorations
        if (deco.isNotEmpty()) {
            drawCustomDecorations(canvas, deco, layout.canvasWidth, layout.canvasHeight, input.context)
        }

    }

    private fun strokeSeasonCellBorder(canvas: Canvas, cell: RectF, season: Season, scale: Float) {
        val radius = cell.width() * SeasonHTMLFrameStyle.CELL_CORNER_RATIO
        val strokeHex = SeasonHTMLFrameStyle.cellStrokeHex(season)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (0xFF000000 or strokeHex).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.65f * scale
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
                    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = (0xFF000000 or (kind.textColorARGB and 0xFFFFFF)).toInt()
                        textSize = pt
                        typeface = tf
                    }
                    // The editor's text handle is two lines wide. Keep the printed
                    // text proportional to the frame, without changing its source.
                    val width = (base * 0.27f * dec.scale).roundToInt().coerceAtLeast(1)
                    val lines = StaticLayout.Builder.obtain(kind.content, 0, kind.content.length, paint, width)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setMaxLines(2)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .setIncludePad(false)
                        .build()
                    canvas.translate(-width / 2f, -lines.height / 2f)
                    lines.draw(canvas)
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
                        StickerVectorPainter.draw(canvas, palette, stickerColor, pt)
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
        val maxWidth = (area.width() - 24f * scale).coerceAtLeast(1f)
        val rawDateWidth = if (dateString.isNullOrBlank()) 0f else datePaint.measureText(dateString)
        val separatorWidth = if (!text.isNullOrBlank() && !dateString.isNullOrBlank())
            sepPaint.measureText(separator) else 0f
        if (!text.isNullOrBlank()) {
            val available = if (rawDateWidth > 0f)
                (maxWidth - rawDateWidth - separatorWidth).coerceAtLeast(0f) else maxWidth
            val visible = TextUtils.ellipsize(text, TextPaint(textPaint), available,
                TextUtils.TruncateAt.END).toString()
            val w = textPaint.measureText(visible)
            val fm = textPaint.fontMetrics
            if (visible.isNotEmpty()) chunks.add(Chunk(visible, textPaint, w, fm.descent - fm.ascent))
        }
        if (!dateString.isNullOrBlank()) {
            if (chunks.isNotEmpty()) {
                val w = sepPaint.measureText(separator)
                val fm = sepPaint.fontMetrics
                chunks.add(Chunk(separator, sepPaint, w, fm.descent - fm.ascent))
            }
            val available = (maxWidth - chunks.sumOf { it.width.toDouble() }.toFloat()).coerceAtLeast(0f)
            val visible = TextUtils.ellipsize(dateString, TextPaint(datePaint), available,
                TextUtils.TruncateAt.END).toString()
            val w = datePaint.measureText(visible)
            val fm = datePaint.fontMetrics
            if (visible.isNotEmpty()) chunks.add(Chunk(visible, datePaint, w, fm.descent - fm.ascent))
            else if (chunks.lastOrNull()?.str == separator) chunks.removeAt(chunks.lastIndex)
        }

        val totalW = chunks.sumOf { it.width.toDouble() }.toFloat()
        var drawX = area.centerX() - totalW / 2f
        val baseY = area.centerY()

        for (chunk in chunks) {
            val fm = chunk.paint.fontMetrics
            val y = baseY - (fm.ascent + fm.descent) / 2f
            canvas.drawText(chunk.str, drawX, y, chunk.paint)
            drawX += chunk.width
        }
    }

    private fun drawAspectFill(
        canvas: Canvas,
        bitmap: Bitmap,
        dst: RectF,
        paint: Paint,
        cropTransform: PhotoCropTransform = PhotoCropTransform(),
    ) {
        canvas.save()
        canvas.clipRect(dst)
        val target = CropMath.drawRect(
            imageWidth = bitmap.width.toFloat(),
            imageHeight = bitmap.height.toFloat(),
            viewport = CropRect(dst.left, dst.top, dst.right, dst.bottom),
            transform = cropTransform,
        )
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(target.left, target.top, target.right, target.bottom),
            paint,
        )
        canvas.restore()
    }

    private fun isDark(color: Color): Boolean {
        val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
        return luminance <= 0.5f
    }
}
