package com.pocket4cut.frame.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.pocket4cut.frame.CollageLayoutDimensions
import com.pocket4cut.ui.designsystem.theme.Season
import java.io.IOException
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Six original printed-paper motifs per season, composed around the real photo geometry.
 *
 * The atlas is decoded off Main and shared read-only by preview and export. Dropping a
 * cache reference deliberately does not recycle its Bitmap: a displayed Compose scene
 * or an in-flight export may still own that sheet.
 */
object SeasonalStickerArt {
    private const val CELL_PX = 512
    private const val COLUMNS = 3
    private const val ROWS = 2
    private const val CACHE_SIZE = 2
    private const val BRAND_HALF_WIDTH_PT = 98f

    class Sheet internal constructor(
        val season: Season,
        internal val bitmap: Bitmap,
    ) {
        val pixelWidth: Int get() = bitmap.width
        val pixelHeight: Int get() = bitmap.height
    }

    data class Placement(
        val cellIndex: Int,
        val bounds: RectF,
        val rotationDegrees: Float = 0f,
    ) {
        /** Includes rotation; useful when checking which print bands a motif occupies. */
        val rotatedBounds: RectF
            get() {
                val radians = Math.toRadians(rotationDegrees.toDouble())
                val halfW = (abs(cos(radians)) * bounds.width() +
                    abs(sin(radians)) * bounds.height()).toFloat() / 2f
                val halfH = (abs(sin(radians)) * bounds.width() +
                    abs(cos(radians)) * bounds.height()).toFloat() / 2f
                return RectF(bounds.centerX() - halfW, bounds.centerY() - halfH,
                    bounds.centerX() + halfW, bounds.centerY() + halfH)
            }
    }

    private val cacheMutex = Mutex()
    private val cache = LinkedHashMap<Season, Sheet>(CACHE_SIZE + 1, 0.75f, true)

    suspend fun load(context: Context, season: Season): Sheet = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            cache[season]?.let { return@withLock it }
            val fileName = when (season) {
                Season.SPRING -> "spring"
                Season.SUMMER -> "summer"
                Season.AUTUMN -> "autumn"
                Season.WINTER -> "winter"
            }
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inMutable = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.applicationContext.assets.open("seasonal/$fileName.png").use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: throw IOException("Cannot decode seasonal artwork: $fileName")
            if (bitmap.width != CELL_PX * COLUMNS || bitmap.height != CELL_PX * ROWS || !bitmap.hasAlpha()) {
                // This sheet has not been published to either a scene or the cache.
                bitmap.recycle()
                throw IOException("Invalid seasonal artwork format: $fileName")
            }
            val sheet = Sheet(season, bitmap)
            cache[season] = sheet
            if (cache.size > CACHE_SIZE) {
                val oldest = cache.entries.iterator()
                oldest.next()
                oldest.remove()
            }
            sheet
        }
    }

    /**
     * These protected rectangles are never painted by a seasonal sticker. The brand
     * rectangle reserves the existing 22pt title, rather than blocking the whole header.
     * The complete optional caption band is reserved for arbitrary user text and dates.
     */
    fun protectedAreas(layout: CollageLayoutDimensions): List<RectF> = buildList {
        val clearance = layout.scale
        layout.cells.forEach { cell ->
            add(RectF(cell).apply { inset(-clearance, -clearance) })
        }
        add(RectF(
            layout.canvasWidth / 2f - BRAND_HALF_WIDTH_PT * layout.scale,
            layout.headerArea.top,
            layout.canvasWidth / 2f + BRAND_HALF_WIDTH_PT * layout.scale,
            layout.headerArea.bottom,
        ))
        layout.textArea?.let { add(RectF(it)) }
    }

    /**
     * All positions derive from live slot edges: this works for both 6-cut layout
     * versions and when a caption changes the classic strip's slot heights.
     * Edge motifs fit inside the paper margins, including their rotated bounds.
     * draw() additionally protects photo and text pixels at every output scale.
     */
    fun placements(layout: CollageLayoutDimensions): List<Placement> {
        if (layout.cells.isEmpty() || layout.scale <= 0f) return emptyList()
        val unit = layout.scale
        val width = layout.canvasWidth
        val height = layout.canvasHeight
        val photoLeft = layout.cells.minOf { it.left }
        val photoRight = layout.cells.maxOf { it.right }
        val photoTop = layout.cells.minOf { it.top }
        val photoBottom = layout.cells.maxOf { it.bottom }
        val result = mutableListOf<Placement>()

        fun add(cell: Int, x: Float, y: Float, side: Float, angle: Float = 0f) {
            if (side < 5f * unit) return
            result += Placement(cell, RectF(x - side / 2f, y - side / 2f,
                x + side / 2f, y + side / 2f), angle)
        }

        // One hero motif on each side of the title. Both remain completely visible
        // even in the compact 40pt header of the asymmetric 6-cut layout.
        val brandLeft = width / 2f - BRAND_HALF_WIDTH_PT * unit
        val headerRoom = min(brandLeft - 12f * unit, layout.headerArea.height() - 10f * unit)
        val heroSide = min(66f * unit, headerRoom / 1.14f)
        val headerY = layout.headerArea.centerY()
        add(0, brandLeft / 2f, headerY, heroSide, -7f)
        add(1, width - brandLeft / 2f, headerY, heroSide, 7f)

        val bodyHeight = photoBottom - photoTop
        val leftCenter = (photoLeft + unit) / 2f
        val rightCenter = (photoRight + width - unit) / 2f
        // 1.18 encloses the largest (11 degree) rotation; retain print bleed and
        // photo clearance without cropping recognisable bows, wings or leaves.
        val leftSide = min(32f * unit, (photoLeft - 3f * unit) / 1.18f)
        val rightSide = min(32f * unit, (width - photoRight - 3f * unit) / 1.18f)
        // Alternating full-silhouette paper-edge accents.
        add(2, leftCenter, photoTop + bodyHeight * 0.19f, leftSide, -11f)
        add(3, rightCenter, photoTop + bodyHeight * 0.42f, rightSide, 9f)
        add(4, leftCenter, photoTop + bodyHeight * 0.71f, leftSide, 8f)
        if (bodyHeight > 330f * unit) {
            add(5, rightCenter, photoTop + bodyHeight * 0.87f, rightSide, -8f)
        }

        // A motif in each genuinely empty full-width row gutter. Detecting the
        // union of vertical photo intervals avoids painting through the hero slot.
        val bands = layout.cells.sortedBy { it.top }
        var bottom = bands.first().bottom
        var gutterIndex = 0
        for (cell in bands.drop(1)) {
            if (cell.top > bottom + 4f * unit) {
                val gap = cell.top - bottom
                val side = min(22f * unit, gap - 2f * unit)
                val x = width * if (gutterIndex % 2 == 0) 0.29f else 0.71f
                add(2 + gutterIndex % 4, x, (bottom + cell.top) / 2f, side)
                gutterIndex++
            }
            bottom = maxOf(bottom, cell.bottom)
        }

        // Never share the footer with user text; asymmetric 6-cut has a separate
        // 25pt strip above its caption, while the generic caption uses the whole band.
        val footerBottom = min(height - 4f * unit, layout.textArea?.top ?: height)
        val footerTop = photoBottom + 3f * unit
        val footerSide = min(30f * unit, footerBottom - footerTop - 2f * unit)
        if (footerSide >= 8f * unit) {
            val y = (footerTop + footerBottom) / 2f
            add(4, width * 0.35f, y, footerSide / 1.08f, -4f)
            add(5, width * 0.65f, y, footerSide / 1.08f, 4f)
        }
        return result
    }

    fun draw(canvas: Canvas, sheet: Sheet, layout: CollageLayoutDimensions) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val save = canvas.save()
        try {
            canvas.clipRect(0f, 0f, layout.canvasWidth, layout.canvasHeight)
            protectedAreas(layout).forEach { canvas.clipOutRect(it) }
            for (placement in placements(layout)) {
                val sourceX = placement.cellIndex % COLUMNS * CELL_PX
                val sourceY = placement.cellIndex / COLUMNS * CELL_PX
                val source = Rect(sourceX, sourceY, sourceX + CELL_PX, sourceY + CELL_PX)
                val motifSave = canvas.save()
                try {
                    canvas.rotate(placement.rotationDegrees, placement.bounds.centerX(), placement.bounds.centerY())
                    canvas.drawBitmap(sheet.bitmap, source, placement.bounds, paint)
                } finally {
                    canvas.restoreToCount(motifSave)
                }
            }
        } finally {
            canvas.restoreToCount(save)
        }
    }
}
