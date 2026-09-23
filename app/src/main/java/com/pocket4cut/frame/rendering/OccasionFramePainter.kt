package com.pocket4cut.frame.rendering

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.pocket4cut.core.util.AppFontCatalog
import com.pocket4cut.frame.CollageLayoutDimensions
import com.pocket4cut.frame.occasion.OccasionMotif
import com.pocket4cut.frame.occasion.OccasionPlacementItem
import com.pocket4cut.frame.occasion.OccasionTheme
import kotlin.math.min

/**
 * Draws an Everyday Editions frame from one catalog entry and its six-motif atlas.
 *
 * The same painter is used by the Compose preview and the original-backed export. Photo
 * windows are never part of the artwork bitmap, so the active layout remains the sole source
 * of truth for slot geometry.
 */
object OccasionFramePainter {
    private const val LOGICAL_WIDTH = 390f
    internal const val BRAND_LABEL = "Pocket 4Cut"

    internal fun numberedBrand(index: Int): String =
        "$BRAND_LABEL / ${index.toString().padStart(2, '0')}"

    fun drawBackdrop(canvas: Canvas, layout: CollageLayoutDimensions, theme: OccasionTheme) {
        val unit = layout.canvasWidth / LOGICAL_WIDTH
        val ink = theme.inkColorArgb
        canvas.drawColor(theme.paperColorArgb)

        val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            alpha = (theme.profile.patternAlpha.coerceIn(0f, 1f) * 255f).toInt()
            strokeWidth = 0.32f * unit
            style = Paint.Style.STROKE
        }
        when (theme.pattern) {
            "fine-rules" -> drawRules(canvas, layout, unit, patternPaint, horizontal = true, vertical = false)
            "pinstripe" -> drawRules(canvas, layout, unit, patternPaint, horizontal = false, vertical = true)
            "micro-check" -> drawRules(canvas, layout, unit, patternPaint, horizontal = true, vertical = true)
            "dotted-paper" -> {
                patternPaint.style = Paint.Style.FILL
                var y = 9f * unit
                while (y < layout.canvasHeight) {
                    var x = 9f * unit
                    while (x < layout.canvasWidth) {
                        canvas.drawCircle(x, y, 0.55f * unit, patternPaint)
                        x += 12f * unit
                    }
                    y += 12f * unit
                }
            }
        }

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            style = Paint.Style.STROKE
            strokeWidth = 0.6f * unit
        }
        canvas.drawRect(
            1.8f * unit,
            1.8f * unit,
            layout.canvasWidth - 1.8f * unit,
            layout.canvasHeight - 1.8f * unit,
            border,
        )
        if (theme.pattern == "double-edge") {
            border.alpha = (0.48f * 255f).toInt()
            canvas.drawRect(
                4f * unit,
                4f * unit,
                layout.canvasWidth - 4f * unit,
                layout.canvasHeight - 4f * unit,
                border,
            )
        }
    }

    fun drawPhotoBorders(canvas: Canvas, layout: CollageLayoutDimensions, theme: OccasionTheme) {
        val unit = layout.canvasWidth / LOGICAL_WIDTH
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.inkColorArgb
            style = Paint.Style.STROKE
            strokeWidth = 0.45f * unit
        }
        val expand = 0.35f * unit
        layout.cells.forEach { cell ->
            canvas.drawRect(
                cell.left - expand,
                cell.top - expand,
                cell.right + expand,
                cell.bottom + expand,
                paint,
            )
        }
    }

    fun drawArtworkAndHeader(
        canvas: Canvas,
        layout: CollageLayoutDimensions,
        theme: OccasionTheme,
        artwork: OccasionArtwork.Sheet,
        context: Context?,
    ) {
        require(artwork.themeId == theme.id) {
            "Occasion artwork ${artwork.themeId} does not match theme ${theme.id}"
        }
        val unit = layout.canvasWidth / LOGICAL_WIDTH
        drawHeader(canvas, layout, theme, artwork, context, unit)
        drawSides(canvas, layout, theme, artwork, unit)
        drawGutters(canvas, layout, theme, artwork, unit)
        drawFooter(canvas, layout, theme, artwork, context, unit)
    }

    private fun drawRules(
        canvas: Canvas,
        layout: CollageLayoutDimensions,
        unit: Float,
        paint: Paint,
        horizontal: Boolean,
        vertical: Boolean,
    ) {
        if (horizontal) {
            var y = 10f * unit
            while (y < layout.canvasHeight) {
                canvas.drawLine(0f, y, layout.canvasWidth, y, paint)
                y += 10f * unit
            }
        }
        if (vertical) {
            var x = 10f * unit
            while (x < layout.canvasWidth) {
                canvas.drawLine(x, 0f, x, layout.canvasHeight, paint)
                x += 10f * unit
            }
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        layout: CollageLayoutDimensions,
        theme: OccasionTheme,
        artwork: OccasionArtwork.Sheet,
        context: Context?,
        unit: Float,
    ) {
        val header = layout.headerArea
        val headerHeight = header.height()
        val heroSide = min(headerHeight * theme.profile.hero, layout.canvasWidth * 0.18f)
        theme.placements.header.items.forEach { item ->
            drawPlacement(
                canvas = canvas,
                artwork = artwork,
                theme = theme,
                item = item,
                centerX = layout.canvasWidth * item.x,
                centerY = header.top + headerHeight * item.y,
                side = when (item.sizeRule) {
                    "hero" -> heroSide
                    else -> heroSide
                },
            )
        }

        val editorial = theme.placements.header.mode == "editorial"
        val hasKorean = theme.title.any { it in '\uAC00'..'\uD7A3' }
        val titleTypeface = when {
            hasKorean && context != null -> AppFontCatalog.typeface(context, "BMYEONSUNG_ttf")
            else -> resolveTypeface(context, theme.profile.font)
        }
        val titleSize = min(
            theme.profile.size * unit,
            headerHeight * if (editorial) 0.32f else 0.33f,
        )
        if (editorial) {
            drawFittedText(
                canvas = canvas,
                text = theme.title,
                x = layout.canvasWidth * 0.065f,
                baselineY = header.top + headerHeight * 0.51f,
                initialSize = titleSize,
                maxWidth = layout.canvasWidth * 0.70f,
                color = theme.inkColorArgb,
                typeface = titleTypeface,
                align = Paint.Align.LEFT,
            )
            drawFittedText(
                canvas,
                numberedBrand(theme.index),
                layout.canvasWidth * 0.068f,
                header.top + headerHeight * 0.76f,
                min(6.6f * unit, headerHeight * 0.12f),
                layout.canvasWidth * 0.65f,
                theme.inkColorArgb,
                Typeface.MONOSPACE,
                Paint.Align.LEFT,
            )
        } else {
            drawFittedText(
                canvas,
                theme.title,
                layout.canvasWidth * 0.5f,
                header.top + headerHeight * 0.51f,
                titleSize,
                layout.canvasWidth * 0.54f,
                theme.inkColorArgb,
                titleTypeface,
                Paint.Align.CENTER,
            )
            val subtitle = if (theme.id == "hangeul-day") {
                "ㄱ  ㄴ  ㄷ / $BRAND_LABEL"
            } else {
                numberedBrand(theme.index)
            }
            drawFittedText(
                canvas,
                subtitle,
                layout.canvasWidth * 0.5f,
                header.top + headerHeight * 0.75f,
                min(6.8f * unit, headerHeight * 0.13f),
                layout.canvasWidth * 0.46f,
                theme.inkColorArgb,
                Typeface.create("sans-serif", Typeface.NORMAL),
                Paint.Align.CENTER,
            )
            if (headerHeight / unit >= 52f) {
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = theme.inkColorArgb
                    alpha = (0.35f * 255f).toInt()
                    strokeWidth = 0.4f * unit
                }
                canvas.drawLine(
                    layout.canvasWidth * 0.285f,
                    header.top + headerHeight * 0.85f,
                    layout.canvasWidth * 0.715f,
                    header.top + headerHeight * 0.85f,
                    line,
                )
            }
        }
    }

    private fun drawSides(
        canvas: Canvas,
        layout: CollageLayoutDimensions,
        theme: OccasionTheme,
        artwork: OccasionArtwork.Sheet,
        unit: Float,
    ) {
        val cells = layout.cells
        if (cells.isEmpty()) return
        val minX = cells.minOf { it.left }
        val maxX = cells.maxOf { it.right }
        val minY = cells.minOf { it.top }
        val maxY = cells.maxOf { it.bottom }
        val leftSize = min(25f * unit, ((minX - 4f * unit) / 1.2f).coerceAtLeast(0f))
        val rightSize = min(25f * unit, ((layout.canvasWidth - maxX - 4f * unit) / 1.2f).coerceAtLeast(0f))
        val variation = (theme.index % 3) * 0.035f

        theme.placements.sides
            .filter { theme.profile.sideMotifs >= it.minSideMotifs }
            .forEach { side ->
                val yFraction = side.yBase + side.yVariationMultiplier * variation
                drawMotif(
                    canvas = canvas,
                    artwork = artwork,
                    motif = theme.atlas.motifs.first { it.index == side.motifIndex },
                    centerX = if (side.edge == "left") minX / 2f else (maxX + layout.canvasWidth) / 2f,
                    centerY = minY + (maxY - minY) * yFraction,
                    side = if (side.edge == "left") leftSize else rightSize,
                    rotationDegrees = side.rotationDegrees,
                )
            }
    }

    private fun drawGutters(
        canvas: Canvas,
        layout: CollageLayoutDimensions,
        theme: OccasionTheme,
        artwork: OccasionArtwork.Sheet,
        unit: Float,
    ) {
        val rule = theme.placements.gutter
        if (!rule.enabled || !theme.profile.gutterMotifs || layout.cells.size < 2) return
        val sorted = layout.cells.sortedBy { it.top }
        var bottom = sorted.first().bottom
        var gapCount = 0
        sorted.drop(1).forEach { cell ->
            if (cell.top > bottom + 4f * unit) {
                val side = min(17f * unit, cell.top - bottom - 2f * unit)
                val motifIndex = (rule.motifStartIndex + gapCount) % rule.motifModulo
                val xFraction = rule.xCycle[gapCount % rule.xCycle.size]
                drawMotif(
                    canvas,
                    artwork,
                    theme.atlas.motifs.first { it.index == motifIndex },
                    layout.canvasWidth * xFraction,
                    (bottom + cell.top) / 2f,
                    side,
                    0f,
                )
                gapCount++
            }
            bottom = maxOf(bottom, cell.bottom)
        }
    }

    private fun drawFooter(
        canvas: Canvas,
        layout: CollageLayoutDimensions,
        theme: OccasionTheme,
        artwork: OccasionArtwork.Sheet,
        context: Context?,
        unit: Float,
    ) {
        // The full caption band belongs to user text and date. Catalog footer motifs can
        // otherwise overlap long text near either edge.
        if (layout.textArea != null) return

        val maxPhotoBottom = layout.cells.maxOfOrNull { it.bottom } ?: return
        val footerTop = maxPhotoBottom
        val footerHeight = layout.canvasHeight - footerTop
        if (footerHeight <= 0f) return

        if (footerHeight > 8f * unit) {
            if (theme.placements.footer.enabled && theme.profile.footerMotifs) {
                val requestedSide = min(footerHeight * 0.58f, layout.canvasWidth * 0.10f)
                theme.placements.footer.items.forEach { item ->
                    val centerX = layout.canvasWidth * item.x
                    val centerY = footerTop + footerHeight * item.y
                    // Keep the complete motif inside the footer and canvas even if future
                    // catalog values move it closer to an edge. Caption layouts return above,
                    // so these decorations can never consume the user text/date safe area.
                    val edgeClearance = minOf(
                        centerX,
                        layout.canvasWidth - centerX,
                        centerY - footerTop,
                        layout.canvasHeight - centerY,
                    ).coerceAtLeast(0f)
                    val side = min(requestedSide, edgeClearance * 2f)
                    if (side > 0f) {
                        drawPlacement(canvas, artwork, theme, item, centerX, centerY, side)
                    }
                }
            }
            drawFittedText(
                canvas,
                BRAND_LABEL,
                layout.canvasWidth * 0.5f,
                footerTop + footerHeight * 0.63f,
                min(8f * unit, footerHeight * 0.35f),
                layout.canvasWidth * 0.55f,
                theme.inkColorArgb,
                resolveTypeface(context, "Pocket Serif Italic"),
                Paint.Align.CENTER,
            )
        }
    }

    private fun drawPlacement(
        canvas: Canvas,
        artwork: OccasionArtwork.Sheet,
        theme: OccasionTheme,
        item: OccasionPlacementItem,
        centerX: Float,
        centerY: Float,
        side: Float,
    ) {
        drawMotif(
            canvas,
            artwork,
            theme.atlas.motifs.first { it.index == item.motifIndex },
            centerX,
            centerY,
            side,
            item.rotationDegrees,
        )
    }

    private fun drawMotif(
        canvas: Canvas,
        artwork: OccasionArtwork.Sheet,
        motif: OccasionMotif,
        centerX: Float,
        centerY: Float,
        side: Float,
        rotationDegrees: Float,
    ) {
        if (side <= 0f) return
        val cell = motif.cellRectPx
        val content = motif.contentSizePx
        val sourceLeft = cell.x + (cell.width - content.width) / 2
        val sourceTop = cell.y + (cell.height - content.height) / 2
        val source = Rect(sourceLeft, sourceTop, sourceLeft + content.width, sourceTop + content.height)
        val scale = min(side / content.width, side / content.height)
        val drawWidth = content.width * scale
        val drawHeight = content.height * scale
        val destination = RectF(
            centerX - drawWidth / 2f,
            centerY - drawHeight / 2f,
            centerX + drawWidth / 2f,
            centerY + drawHeight / 2f,
        )
        canvas.save()
        canvas.rotate(rotationDegrees, centerX, centerY)
        canvas.drawBitmap(
            artwork.bitmap,
            source,
            destination,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        canvas.restore()
    }

    private fun drawFittedText(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        initialSize: Float,
        maxWidth: Float,
        color: Int,
        typeface: Typeface,
        align: Paint.Align,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = initialSize.coerceAtLeast(1f)
            this.typeface = typeface
            textAlign = align
        }
        while (paint.measureText(text) > maxWidth && paint.textSize > 8f) {
            paint.textSize -= 0.5f
        }
        canvas.drawText(text, x, baselineY, paint)
    }

    private fun resolveTypeface(context: Context?, name: String): Typeface = when (name) {
        "Pocket Display" -> context?.let { AppFontCatalog.typeface(it, "BMHANNAPro") }
            ?: Typeface.create("sans-serif-condensed", Typeface.BOLD)
        "Pocket Hand" -> context?.let { AppFontCatalog.typeface(it, "BMYEONSUNG_ttf") }
            ?: Typeface.create("sans-serif", Typeface.NORMAL)
        "Pocket Serif" -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        "Pocket Serif Italic" -> Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        "Pocket Mono" -> Typeface.MONOSPACE
        else -> Typeface.create("sans-serif", Typeface.NORMAL)
    }
}
