package com.pocket4cut

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.frame.CollageLayoutDimensions
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.CollageOutputSize
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.SeasonBackgroundFrameFactory
import com.pocket4cut.frame.rendering.SeasonalStickerArt
import com.pocket4cut.presentation.edit.PendingCollageStore
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.theme.Season
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Exercises production Canvas painting with synthetic photos, never user sessions or MediaStore.
 * Large downloadable assets are an explicit opt-in and are written only inside the QA package.
 */
@RunWith(AndroidJUnit4::class)
class SeasonalFrameContractInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val colors = listOf(
        Color.rgb(185, 32, 53), Color.rgb(26, 137, 77), Color.rgb(43, 70, 190),
        Color.rgb(220, 159, 23), Color.rgb(164, 50, 175), Color.rgb(25, 157, 183),
    )
    private val captions = listOf(
        null to null,
        "우리의 계절" to null,
        null to "2026.09.22",
        "우리의 계절" to "2026.09.22",
    )
    private data class Variant(val style: FrameStyle, val version: Int = 2) {
        val id: String get() = style.id.name.lowercase(Locale.ROOT) + if (version == 1) "_legacy_v1" else ""
    }
    private val variants get() = FrameLayouts.all.map { Variant(it) } +
        Variant(FrameLayouts.byId(FrameLayoutId.SIX_COLLAGE), 1)

    @Test fun seasonalArtPreservesEveryPhotoAndReservedTextAreaAcrossAllLayouts() = runBlocking {
        val fixtures = colors.map { color ->
            Bitmap.createBitmap(60, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        }
        try {
            for (season in Season.entries) {
                val sheet = SeasonalStickerArt.load(context, season)
                for (variant in variants) {
                    for ((text, date) in captions) {
                        val input = input(variant, season, sheet, fixtures, text, date)
                        val layout = previewLayout(variant, text, date)
                        val label = "${season.name}/${variant.id}/$text/$date"
                        val result = drawAtPreviewSize(input, layout)
                        val art = bitmapFor(layout)
                        try {
                            assertPhotosUntouched(label, result, layout)
                            SeasonalStickerArt.draw(Canvas(art), sheet, layout)
                            assertProtectedAreasAreTransparent(label, art, layout)
                            assertTrue("$label must contain visible seasonal illustrations", visiblePixelCount(art) > 40)
                        } finally {
                            result.recycle()
                            art.recycle()
                        }
                    }
                }
            }
        } finally {
            fixtures.forEach(Bitmap::recycle)
        }
    }

    @Test fun previewAndFinalCoordinateSystemsPaintTheSameSeasonalComposition() = runBlocking {
        for (season in Season.entries) {
            val sheet = SeasonalStickerArt.load(context, season)
            for (variant in variants) {
                for ((text, date) in listOf(captions.first(), captions.last())) {
                    val input = input(variant, season, sheet, emptyList(), text, date)
                    val preview = previewLayout(variant, text, date)
                    val final = finalLayout(variant, text, date)
                    val label = "${season.name}/${variant.id}/$text/$date"
                    assertProportionalGeometry(label, preview, final)
                    val previewImage = drawAtPreviewSize(input, preview)
                    val projectedExport = bitmapFor(preview)
                    try {
                        val canvas = Canvas(projectedExport)
                        canvas.scale(preview.canvasWidth / final.canvasWidth, preview.canvasWidth / final.canvasWidth)
                        CollageRenderer.drawScene(canvas, input, final)
                        assertNearlyIdentical(label, previewImage, projectedExport)
                    } finally {
                        previewImage.recycle()
                        projectedExport.recycle()
                    }
                }
            }
        }
    }

    @Test fun seasonalAssetsAreBoundedAndExistingSavedSeasonIdentifiersStillRoundTrip() = runBlocking {
        for (season in Season.entries) {
            val design = SeasonBackgroundFrameFactory.design(season)
            val restored = PendingCollageStore.deserializeDesign(PendingCollageStore.serializeDesign(design))
            assertNotNull(restored)
            assertEquals(season.name.lowercase(Locale.ROOT), restored!!.sourceSeason)
            assertEquals(season, restored.resolvedSeason)
            val sheet = SeasonalStickerArt.load(context, season)
            assertEquals(season, sheet.season)
            assertTrue("Atlas dimensions stay bounded", sheet.pixelWidth <= 2048 && sheet.pixelHeight <= 2048)
            assertTrue("Atlas retains print-detail source cells", sheet.pixelWidth >= 768 && sheet.pixelHeight >= 512)
            assertEquals("Atlas is three columns by two rows", sheet.pixelWidth * 2, sheet.pixelHeight * 3)
            val pixels = IntArray(sheet.pixelWidth * sheet.pixelHeight)
            sheet.bitmap.getPixels(pixels, 0, sheet.pixelWidth, 0, 0, sheet.pixelWidth, sheet.pixelHeight)
            assertTrue("Illustrations require real transparent pixels", pixels.count { Color.alpha(it) == 0 } > pixels.size / 10)
            assertTrue("Atlas must not be empty", pixels.count { Color.alpha(it) > 16 } > pixels.size / 50)
        }
    }

    /**
     * Run only with -e seasonalExport true (or Gradle's instrumentation runner argument).
     * 72 alpha-slot templates + 32 fictional-photo composites; no normal build export side effect.
     */
    @Test fun exportDownloadableFramesAndFictionalPhotoCompositesWhenExplicitlyRequested() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Large asset export is opt-in", arguments.getString("seasonalExport") == "true")
        assertEquals("Never export into the production app", "com.pocket4cut.qa", context.packageName)
        // AGP collects this directory before UTP uninstalls the QA app. Ordinary
        // external files would disappear with that uninstall before adb can pull them.
        val outputRoot = arguments.getString("additionalTestOutputDir")?.takeIf { it.isNotBlank() }?.let { path ->
            val provided = File(path).canonicalFile
            @Suppress("DEPRECATION")
            val allowed = context.externalMediaDirs.filterNotNull().map {
                File(it, "additional_test_output").canonicalFile
            }
            require(provided in allowed) { "Test output must be the QA package's additional output directory" }
            provided
        } ?: requireNotNull(context.getExternalFilesDir(null)).canonicalFile
        val output = File(outputRoot, "seasonal-export/run-${System.currentTimeMillis()}").canonicalFile
        require(output.path.startsWith(outputRoot.path + File.separator)) { "Export escaped its QA output root" }
        assertTrue(output.mkdirs())
        val records = JSONArray()
        val photos = (1..4).map { index ->
            instrumentation.context.assets.open("seasonal-demo/demo_0$index.jpg").use { stream ->
                requireNotNull(BitmapFactory.decodeStream(stream)) { "Missing fictional demo photo $index" }
            }
        }
        try {
            for (season in Season.entries) {
                val sheet = SeasonalStickerArt.load(context, season)
                val seasonId = season.name.lowercase(Locale.ROOT)
                for (variant in variants) {
                    for (captionBand in listOf(false, true)) {
                        // Keep the actual caption-on geometry while leaving the template band blank.
                        val layout = finalLayout(variant, "reserved".takeIf { captionBand }, null)
                        val blank = input(variant, season, sheet, emptyList(), null, null)
                        val bitmap = bitmapFor(layout)
                        try {
                            CollageRenderer.drawScene(Canvas(bitmap), blank, layout)
                            clearPhotoWindows(bitmap, layout)
                            assertAlphaWindows(bitmap, layout)
                            val name = "frames/$seasonId/${variant.id}${if (captionBand) "_caption" else ""}.png"
                            writePng(bitmap, File(output, name))
                            records.put(record(name, "transparent-frame", seasonId, variant, layout, bitmap, captionBand))
                        } finally {
                            bitmap.recycle()
                        }
                    }
                    if (variant.version == 2) {
                        val photoOrder = List(variant.style.slots) { photos[it % photos.size] }
                        val demoInput = input(variant, season, sheet, photoOrder, null, null)
                        val bitmap = CollageRenderer.render(demoInput)
                        try {
                            val layout = finalLayout(variant, null, null)
                            assertEquals(layout.canvasWidth.roundToInt(), bitmap.width)
                            assertEquals(layout.canvasHeight.roundToInt(), bitmap.height)
                            val name = "examples/$seasonId/${variant.id}.jpg"
                            val file = File(output, name)
                            file.parentFile!!.mkdirs()
                            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 98, it)) }
                            records.put(record(name, "fictional-photo-composite", seasonId, variant, layout, bitmap, false))
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
            assertEquals(104, records.length())
            val manifest = JSONObject().apply {
                put("renderer", "Production Android CollageRenderer.drawScene / CollageRenderer.render")
                put("apiLevel", Build.VERSION.SDK_INT)
                put("sourceRevision", arguments.getString("seasonalSourceRevision") ?: "unspecified")
                put("templateCount", 72)
                put("exampleCount", 32)
                put("exampleFormat", "JPEG quality 98, same opaque image format as the app")
                put("photoSource", "Four fictional generated adult demo photos from existing print-booth-v1 assets; repeated for six slots")
                put("templateMethod", "Production scene with actual layout geometry, photo windows cleared to true alpha; caption variants reserve an empty caption band")
                put("appExportFormat", "JPEG; transparent PNG files are production-rendered design deliverables, not a new app export format")
                put("files", records)
            }
            File(output, "manifest.json").writeText(manifest.toString(2), Charsets.UTF_8)
            instrumentation.sendStatus(0, Bundle().apply { putString("seasonalExportPath", output.absolutePath) })
        } finally {
            photos.forEach(Bitmap::recycle)
        }
    }

    private fun input(
        variant: Variant, season: Season, sheet: SeasonalStickerArt.Sheet,
        photos: List<Bitmap>, text: String?, date: String?,
    ) = CollageRenderer.Input(
        images = photos, frameStyle = variant.style, theme = theme(variant),
        customFrameDesign = SeasonBackgroundFrameFactory.design(season),
        seasonalArt = sheet, text = text, dateString = date, context = context, layoutVersion = variant.version,
    )

    private fun theme(variant: Variant) = FrameCatalog.themes(
        FrameType.entries.first { it.selectCount == variant.style.slots },
    ).first()

    private fun previewLayout(variant: Variant, text: String?, date: String?) = CollageLayoutMath.computeForPreview(
        variant.style, theme(variant), listOfNotNull(text, date).joinToString(" · ").ifBlank { null },
        390f, variant.version,
    )

    private fun finalLayout(variant: Variant, text: String?, date: String?): CollageLayoutDimensions {
        val logical = CollageLayoutMath.compute(variant.style, theme(variant), text, date, 390f, variant.version)
        val width = if (variant.style.id == FrameLayoutId.FOUR_VERTICAL) 1650 else CollageOutputSize.forScene(logical).first
        return CollageLayoutMath.compute(variant.style, theme(variant), text, date, width.toFloat(), variant.version)
    }

    private fun bitmapFor(layout: CollageLayoutDimensions) = Bitmap.createBitmap(
        layout.canvasWidth.roundToInt(), layout.canvasHeight.roundToInt(), Bitmap.Config.ARGB_8888,
    )

    private fun drawAtPreviewSize(input: CollageRenderer.Input, layout: CollageLayoutDimensions) =
        bitmapFor(layout).also { CollageRenderer.drawScene(Canvas(it), input, layout) }

    private fun assertPhotosUntouched(label: String, bitmap: Bitmap, layout: CollageLayoutDimensions) {
        layout.cells.forEachIndexed { index, cell ->
            // Includes pixels near all four edges, not just an easy-to-pass center sample.
            for (xFraction in listOf(0.06f, 0.25f, 0.5f, 0.75f, 0.94f)) {
                for (yFraction in listOf(0.06f, 0.25f, 0.5f, 0.75f, 0.94f)) {
                    val pixel = bitmap.getPixel((cell.left + cell.width() * xFraction).toInt(), (cell.top + cell.height() * yFraction).toInt())
                    assertEquals("$label photo ${index + 1} at $xFraction/$yFraction", colors[index], pixel)
                }
            }
        }
    }

    private fun assertProtectedAreasAreTransparent(label: String, art: Bitmap, layout: CollageLayoutDimensions) {
        val pixels = IntArray(art.width * art.height)
        art.getPixels(pixels, 0, art.width, 0, 0, art.width, art.height)
        for (rect in SeasonalStickerArt.protectedAreas(layout)) {
            for (y in ceil(rect.top + 1).toInt().coerceAtLeast(0) until floor(rect.bottom - 1).toInt().coerceAtMost(art.height)) {
                for (x in ceil(rect.left + 1).toInt().coerceAtLeast(0) until floor(rect.right - 1).toInt().coerceAtMost(art.width)) {
                    if (Color.alpha(pixels[y * art.width + x]) != 0) {
                        throw AssertionError("$label illustration overlaps protected area at $x,$y: $rect")
                    }
                }
            }
        }
    }

    private fun visiblePixelCount(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { Color.alpha(it) > 16 }
    }

    private fun assertProportionalGeometry(label: String, preview: CollageLayoutDimensions, final: CollageLayoutDimensions) {
        val factor = preview.canvasWidth / final.canvasWidth
        assertEquals("$label height", preview.canvasHeight, final.canvasHeight * factor, 0.02f)
        assertEquals("$label scale", preview.scale, final.scale * factor, 0.0001f)
        val p = preview.cells + listOfNotNull(preview.headerArea, preview.textArea)
        val f = final.cells + listOfNotNull(final.headerArea, final.textArea)
        assertEquals(p.size, f.size)
        p.zip(f).forEach { (a, b) ->
            assertEquals("$label left", a.left, b.left * factor, 0.02f)
            assertEquals("$label top", a.top, b.top * factor, 0.02f)
            assertEquals("$label right", a.right, b.right * factor, 0.02f)
            assertEquals("$label bottom", a.bottom, b.bottom * factor, 0.02f)
        }
    }

    private fun assertNearlyIdentical(label: String, a: Bitmap, b: Bitmap) {
        val count = a.width * a.height
        val first = IntArray(count)
        val second = IntArray(count)
        a.getPixels(first, 0, a.width, 0, 0, a.width, a.height)
        b.getPixels(second, 0, b.width, 0, 0, b.width, b.height)
        var total = 0L
        var largeDifferences = 0
        for (i in first.indices) {
            val delta = abs(Color.red(first[i]) - Color.red(second[i])) +
                abs(Color.green(first[i]) - Color.green(second[i])) +
                abs(Color.blue(first[i]) - Color.blue(second[i]))
            total += delta
            if (delta > 120) largeDifferences++
        }
        // Same canvas scene projected from export coordinates; only subpixel rasterization differs.
        assertTrue("$label mean RGB channel delta=${total.toDouble() / (count * 3)}", total.toDouble() / (count * 3) < 2.5)
        assertTrue("$label large-difference pixel fraction=${largeDifferences.toDouble() / count}", largeDifferences.toDouble() / count < 0.03)
    }

    private fun clearPhotoWindows(bitmap: Bitmap, layout: CollageLayoutDimensions) {
        val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        val canvas = Canvas(bitmap)
        layout.cells.forEach { canvas.drawRect(it, clear) }
        clear.xfermode = null
    }

    private fun assertAlphaWindows(bitmap: Bitmap, layout: CollageLayoutDimensions) {
        assertTrue(bitmap.hasAlpha())
        layout.cells.forEach { cell -> assertEquals(0, Color.alpha(bitmap.getPixel(cell.centerX().toInt(), cell.centerY().toInt()))) }
        assertFalse("Frame outside the slots must remain visible", bitmap.getPixel(0, 0) == Color.TRANSPARENT)
    }

    private fun writePng(bitmap: Bitmap, file: File) {
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        assertTrue(bitmap.width <= 8192 && bitmap.height <= 8192)
        assertTrue(bitmap.width.toLong() * bitmap.height <= 16_000_000L)
        file.parentFile!!.mkdirs()
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private fun record(
        name: String, kind: String, season: String, variant: Variant, layout: CollageLayoutDimensions,
        bitmap: Bitmap, captionBand: Boolean,
    ) = JSONObject().apply {
        put("path", name)
        put("kind", kind)
        put("season", season)
        put("layoutId", variant.style.id.name)
        put("layoutVersion", variant.version)
        put("slotCount", variant.style.slots)
        put("width", bitmap.width)
        put("height", bitmap.height)
        put("captionBand", captionBand)
        put("captionPrinted", false)
        put("photoSlots", JSONArray().apply { layout.cells.forEach { put(rectJson(it)) } })
        put("header", rectJson(layout.headerArea))
        put("captionArea", layout.textArea?.let(::rectJson) ?: JSONObject.NULL)
    }

    private fun rectJson(rect: RectF) = JSONArray(listOf(rect.left, rect.top, rect.right, rect.bottom))
}
