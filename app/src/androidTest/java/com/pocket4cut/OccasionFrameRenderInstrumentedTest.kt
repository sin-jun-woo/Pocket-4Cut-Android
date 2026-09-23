package com.pocket4cut

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Debug
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
import com.pocket4cut.frame.OCCASION_PREVIEW_MAX_CONCURRENT_DECODES
import com.pocket4cut.frame.loadOccasionArtworkForPreview
import com.pocket4cut.frame.occasion.OccasionCatalogLoader
import com.pocket4cut.frame.occasion.OccasionTheme
import com.pocket4cut.frame.rendering.OccasionArtwork
import com.pocket4cut.frame.rendering.OccasionFramePainter
import com.pocket4cut.presentation.navigation.FrameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Synthetic-only verification of the production Everyday Editions render path.
 *
 * The tests read bundled application assets and allocate in-memory bitmaps only. They never
 * open a session, user storage, MediaStore, or a photo provider.
 */
@RunWith(AndroidJUnit4::class)
class OccasionFrameRenderInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val photoColors = intArrayOf(
        Color.rgb(19, 231, 161),
        Color.rgb(237, 31, 109),
        Color.rgb(37, 83, 241),
        Color.rgb(249, 193, 17),
        Color.rgb(151, 43, 227),
        Color.rgb(11, 179, 223),
    )

    private data class Variant(val style: FrameStyle, val version: Int = 2) {
        val id: String = style.id.name.lowercase(Locale.ROOT) +
            if (version == 1) "_legacy_v1" else "_v2"
    }

    private val variants: List<Variant> = FrameLayouts.all.map(::Variant) +
        Variant(FrameLayouts.byId(FrameLayoutId.SIX_COLLAGE), version = 1)

    @Test
    fun brandAndManualRecordTitlesNeverRenderExampleValues() {
        assertEquals("Pocket 4Cut", OccasionFramePainter.BRAND_LABEL)
        assertEquals("Pocket 4Cut / 01", OccasionFramePainter.numberedBrand(1))
        assertEquals("Pocket 4Cut / 88", OccasionFramePainter.numberedBrand(88))

        val themes = OccasionCatalogLoader.load(context).themes.associateBy { it.id }
        assertEquals("THIS MONTH", themes.getValue("this-month").title)
        assertEquals("THIS YEAR", themes.getValue("this-year").title)
        assertEquals("NTH FOUR CUT", themes.getValue("nth-fourcut").title)
        assertEquals("GRADUATION DAY", themes.getValue("graduation").title)
        assertEquals("GOODBYE, OLD YEAR", themes.getValue("year-end").title)
        assertEquals("THIS MONTH", themes.getValue("monthly-memory").title)
        assertEquals("THIS YEAR", themes.getValue("annual-memory").title)
        assertEquals("D-Day 기록 프레임", themes.getValue("dday").displayName)
        listOf(
            "this-month",
            "this-year",
            "nth-fourcut",
            "graduation",
            "year-end",
            "monthly-memory",
            "annual-memory",
        ).forEach { id ->
            val title = themes.getValue(id).title
            assertFalse("$id retained a dated example title: $title", title.contains("2026"))
            assertFalse("$id retained a month example title: $title", title.contains("SEPTEMBER"))
            assertFalse("$id retained an ordinal example title: $title", title.contains("10TH"))
        }
    }

    @Test
    fun captionBandIsReservedForUserTextAcrossFooterThemesAndLayouts() {
        val footerThemes = OccasionCatalogLoader.load(context).themes.filter {
            it.placements.footer.enabled && it.profile.footerMotifs
        }
        assertTrue("Catalog did not exercise footer artwork", footerThemes.isNotEmpty())
        footerThemes.forEach { occasion ->
            val artwork = loadWithHeapEvidence(occasion)
            variants.forEach { variant ->
                val layout = previewLayout(
                    variant,
                    "가장자리까지 이어지는 긴 사용자 문구로 안전 영역을 검증합니다",
                    "2026.09.24",
                )
                val bitmap = bitmapFor(layout)
                try {
                    OccasionFramePainter.drawArtworkAndHeader(
                        canvas = Canvas(bitmap),
                        layout = layout,
                        theme = occasion,
                        artwork = artwork,
                        context = context,
                    )
                    assertCaptionBandHasNoFrameArtwork(
                        "${occasion.id}/${variant.id}",
                        bitmap,
                        layout,
                    )
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    @Test
    fun captionFreeFramesRenderFooterMotifsOutsideEveryPhotoSlot() {
        val footerThemes = OccasionCatalogLoader.load(context).themes.filter {
            it.placements.footer.enabled && it.profile.footerMotifs
        }
        assertTrue("Catalog did not exercise footer artwork", footerThemes.isNotEmpty())
        footerThemes.forEach { occasion ->
            val artwork = loadWithHeapEvidence(occasion)
            variants.forEach { variant ->
                val layout = previewLayout(variant, null, null)
                val bitmap = bitmapFor(layout)
                val noFooterBaseline = bitmapFor(layout)
                val label = "${occasion.id}/${variant.id}/footer"
                try {
                    OccasionFramePainter.drawArtworkAndHeader(
                        canvas = Canvas(bitmap),
                        layout = layout,
                        theme = occasion,
                        artwork = artwork,
                        context = context,
                    )
                    OccasionFramePainter.drawArtworkAndHeader(
                        canvas = Canvas(noFooterBaseline),
                        layout = layout,
                        theme = occasion.copy(
                            profile = occasion.profile.copy(footerMotifs = false),
                            placements = occasion.placements.copy(
                                footer = occasion.placements.footer.copy(enabled = false),
                            ),
                        ),
                        artwork = artwork,
                        context = context,
                    )
                    assertTransparentPhotoSlotInteriors(label, bitmap, layout)
                    assertEveryFooterMotifDiffersFromBaseline(
                        label,
                        bitmap,
                        noFooterBaseline,
                        layout,
                        occasion,
                    )
                } finally {
                    bitmap.recycle()
                    noFooterBaseline.recycle()
                }
            }
        }
    }

    @Test
    fun previewAtlasOutOfMemoryBecomesRetryableFailureAndEvictsTheLru() {
        val theme = OccasionCatalogLoader.load(context).themes.first()
        OccasionArtwork.clearForTest()
        try {
            val before = loadWithHeapEvidence(theme)
            val failed = OccasionArtwork.loadRecoverably<OccasionArtwork.Sheet>(theme.id) {
                throw OutOfMemoryError("synthetic preview decoder pressure")
            }
            assertTrue(failed.isFailure)
            val failure = failed.exceptionOrNull()
            assertTrue(failure is OccasionArtwork.RecoverableLoadException)
            assertTrue(failure?.cause is OutOfMemoryError)

            val retry = OccasionArtwork.loadCatching(
                context = context,
                themeId = theme.id,
                assetPath = theme.atlas.assetPath,
                expectedWidth = theme.atlas.width,
                expectedHeight = theme.atlas.height,
            )
            assertTrue("Atlas retry did not recover after cache eviction", retry.isSuccess)
            assertNotSame("OOM recovery did not evict the cached atlas", before, retry.getOrThrow())
        } finally {
            OccasionArtwork.clearForTest()
        }
    }

    @Test
    fun concurrentRequestsForOneAtlasShareOnePublishedSheet() {
        val theme = OccasionCatalogLoader.load(context).themes.first()
        OccasionArtwork.clearForTest()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val start = CountDownLatch(1)
            val futures = List(4) {
                executor.submit<OccasionArtwork.Sheet> {
                    start.await()
                    OccasionArtwork.load(
                        context = context,
                        themeId = theme.id,
                        assetPath = theme.atlas.assetPath,
                        expectedWidth = theme.atlas.width,
                        expectedHeight = theme.atlas.height,
                    )
                }
            }
            start.countDown()
            val sheets = futures.map { it.get(10, TimeUnit.SECONDS) }
            assertTrue(
                "Concurrent callers received separately published atlas sheets",
                sheets.all { it === sheets.first() },
            )
        } finally {
            executor.shutdownNow()
            OccasionArtwork.clearForTest()
        }
    }

    @Test
    fun aToBToAKeepsTheLatestAWaiterWhenTheOriginalOwnerIsObsolete() {
        OccasionArtwork.clearForTest()
        val executor = Executors.newFixedThreadPool(2)
        val firstAActive = AtomicBoolean(true)
        val latestAActive = AtomicBoolean(true)
        val aDecodeStarted = CountDownLatch(1)
        val releaseADecode = CountDownLatch(1)
        val aDecodeCount = AtomicInteger(0)
        try {
            val firstA = executor.submit<OccasionArtwork.Sheet> {
                OccasionArtwork.loadForTest(
                    themeId = "theme-a",
                    cacheKey = "a-b-a",
                    expectedWidth = 16,
                    expectedHeight = 16,
                    canPublish = firstAActive::get,
                    decoder = {
                        aDecodeCount.incrementAndGet()
                        aDecodeStarted.countDown()
                        assertTrue("A decode was not released", releaseADecode.await(5, TimeUnit.SECONDS))
                        Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
                    },
                )
            }
            assertTrue("First A decode did not start", aDecodeStarted.await(5, TimeUnit.SECONDS))

            // A -> B: the first preview is canceled while another theme becomes active.
            firstAActive.set(false)
            val b = OccasionArtwork.loadForTest(
                themeId = "theme-b",
                cacheKey = "a-b-a-b",
                expectedWidth = 8,
                expectedHeight = 8,
                canPublish = { true },
                decoder = { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888) },
            )
            assertEquals("theme-b", b.themeId)

            // B -> A: the latest A joins the still-running A decode. The original owner's
            // canceled Job must not poison the shared future for this active waiter.
            val latestA = executor.submit<OccasionArtwork.Sheet> {
                OccasionArtwork.loadForTest(
                    themeId = "theme-a",
                    cacheKey = "a-b-a",
                    expectedWidth = 16,
                    expectedHeight = 16,
                    canPublish = latestAActive::get,
                    decoder = {
                        fail("Latest A started a duplicate decode instead of joining the flight")
                        Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
                    },
                )
            }
            val waiterDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (OccasionArtwork.inFlightWaiterCountForTest("theme-a", "a-b-a", 16, 16) < 2 &&
                System.nanoTime() < waiterDeadline
            ) {
                Thread.yield()
            }
            assertEquals(
                "Latest A did not join the shared in-flight decode",
                2,
                OccasionArtwork.inFlightWaiterCountForTest("theme-a", "a-b-a", 16, 16),
            )
            releaseADecode.countDown()

            val latestSheet = latestA.get(5, TimeUnit.SECONDS)
            val originalSheet = firstA.get(5, TimeUnit.SECONDS)
            assertEquals("theme-a", latestSheet.themeId)
            assertTrue("A waiters did not share the published sheet", latestSheet === originalSheet)
            assertEquals("A was decoded more than once", 1, aDecodeCount.get())
        } finally {
            releaseADecode.countDown()
            latestAActive.set(false)
            executor.shutdownNow()
            OccasionArtwork.clearForTest()
        }
    }

    @Test
    fun rapidPreviewChangesCancelBeforeDecodeAndNeverPublishAStaleResult() = runBlocking {
        var obsoleteDecodeCount = 0
        val debounceRequest = launch {
            try {
                loadOccasionArtworkForPreview(debounceMillis = 100L) {
                    obsoleteDecodeCount++
                    Result.success("obsolete-before-decode")
                }
            } catch (_: CancellationException) {
                // Expected when a user moves to another theme during the debounce window.
            }
        }
        delay(10L)
        debounceRequest.cancelAndJoin()
        assertEquals("Canceled debounce still decoded an obsolete atlas", 0, obsoleteDecodeCount)

        val published = Collections.synchronizedList(mutableListOf<String>())
        val staleDecodeStarted = CompletableDeferred<Unit>()
        val stale = launch {
            try {
                val value = loadOccasionArtworkForPreview(debounceMillis = 0L) {
                    staleDecodeStarted.complete(Unit)
                    Thread.sleep(150L) // Models a decoder that cannot be canceled mid-call.
                    Result.success("stale")
                }
                published += value.getOrThrow()
            } catch (_: CancellationException) {
                // The post-decode active check must discard this result.
            }
        }
        staleDecodeStarted.await()
        stale.cancel()
        val latest = launch {
            val value = loadOccasionArtworkForPreview(debounceMillis = 0L) {
                Result.success("latest")
            }
            published += value.getOrThrow()
        }
        latest.join()
        stale.join()
        assertEquals(listOf("latest"), published.toList())
    }

    @Test
    fun obsoleteDecodedBitmapIsRecycledBeforeCachePublication() {
        val obsolete = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        assertTrue(OccasionArtwork.recycleDecodedIfObsolete(obsolete) { false })
        assertTrue("Obsolete unpublished bitmap was retained", obsolete.isRecycled)

        val active = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            assertFalse(OccasionArtwork.recycleDecodedIfObsolete(active) { true })
            assertFalse("Active decoded bitmap was recycled", active.isRecycled)
        } finally {
            active.recycle()
        }
    }

    @Test
    fun eightyEightRapidDifferentThemeDecodesStayWithinConcurrencyAndHeapLimits() = runBlocking {
        val active = AtomicInteger(0)
        val maximumActive = AtomicInteger(0)
        val runtime = Runtime.getRuntime()
        val managedStart = runtime.totalMemory() - runtime.freeMemory()
        val nativeStart = Debug.getNativeHeapAllocatedSize()
        val managedPeak = java.util.concurrent.atomic.AtomicLong(managedStart)
        val nativePeak = java.util.concurrent.atomic.AtomicLong(nativeStart)

        val results = coroutineScope {
            List(88) {
                async {
                    loadOccasionArtworkForPreview(debounceMillis = 0L) {
                        val now = active.incrementAndGet()
                        maximumActive.updateAndGet { previous -> maxOf(previous, now) }
                        var bitmap: Bitmap? = null
                        try {
                            bitmap = Bitmap.createBitmap(1_536, 1_024, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.TRANSPARENT)
                            managedPeak.updateAndGet { previous ->
                                maxOf(previous, runtime.totalMemory() - runtime.freeMemory())
                            }
                            nativePeak.updateAndGet { previous ->
                                maxOf(previous, Debug.getNativeHeapAllocatedSize())
                            }
                            Thread.sleep(4L)
                            Result.success(Unit)
                        } finally {
                            bitmap?.recycle()
                            active.decrementAndGet()
                        }
                    }
                }
            }.awaitAll()
        }
        assertTrue(results.all { it.isSuccess })
        assertTrue(
            "Preview decode concurrency=${maximumActive.get()} exceeded " +
                OCCASION_PREVIEW_MAX_CONCURRENT_DECODES,
            maximumActive.get() <= OCCASION_PREVIEW_MAX_CONCURRENT_DECODES,
        )
        val managedGrowth = managedPeak.get() - managedStart
        val nativeGrowth = nativePeak.get() - nativeStart
        assertTrue("Rapid preview managed heap grew by $managedGrowth bytes", managedGrowth < 96L * 1024L * 1024L)
        assertTrue("Rapid preview native heap grew by $nativeGrowth bytes", nativeGrowth < 64L * 1024L * 1024L)
        println(
            "OCCASION_RAPID_PREVIEW: count=88 maxConcurrent=${maximumActive.get()} " +
                "managedGrowth=$managedGrowth nativeGrowth=$nativeGrowth",
        )
    }

    @Test
    fun everyThemeAndLayoutVariantRendersAnOpaqueNonEmptyProtectedScene() {
        val catalog = OccasionCatalogLoader.load(context)
        assertEquals(88, catalog.themes.size)
        assertEquals(9, variants.size)

        val photos = photoColors.map { color ->
            Bitmap.createBitmap(60, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        }
        var scratch = IntArray(0)
        var combinations = 0
        try {
            // Deliberately preserve source order. Reordering can hide cumulative WebP decoder
            // pressure when the final atlas is loaded after the other 87 sheets.
            for (occasion in catalog.themes) {
                val artwork = loadWithHeapEvidence(occasion)
                for (variant in variants) {
                    val frameTheme = frameTheme(variant.style)
                    for (captionOn in listOf(false, true)) {
                        val text = "우리의 기록".takeIf { captionOn }
                        val date = "2026.09.24".takeIf { captionOn }
                        val caption = listOfNotNull(text, date).joinToString(" · ").ifBlank { null }
                        val layout = CollageLayoutMath.computeForPreview(
                            frameStyle = variant.style,
                            theme = frameTheme,
                            bottomCaption = caption,
                            containerWidthPx = 390f,
                            layoutVersion = variant.version,
                        )
                        val label = "${occasion.id}/${variant.id}/caption=$captionOn"
                        val bitmap = bitmapFor(layout)
                        try {
                            CollageRenderer.drawScene(
                                Canvas(bitmap),
                                input(occasion, artwork, variant, photos, text, date),
                                layout,
                            )
                            assertEquals("$label slot count", variant.style.slots, layout.cells.size)
                            assertSlotCentersKeepPhotoOrder(label, bitmap, layout)
                            assertPhotoSlotInteriorsKeepPhotoOrder(label, bitmap, layout)

                            val required = bitmap.width * bitmap.height
                            if (scratch.size < required) scratch = IntArray(required)
                            bitmap.getPixels(scratch, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                            assertOpaqueAndNonEmpty(label, scratch, required, occasion.paperColorArgb)
                            if (captionOn) {
                                assertCaptionAreaHasNoPhotoPixels(label, scratch, bitmap.width, layout)
                            }
                            combinations++
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
        } finally {
            photos.forEach(Bitmap::recycle)
        }
        assertEquals("88 themes x (8 v2 + legacy 6-cut) x caption off/on", 1_584, combinations)
    }

    @Test
    fun previewAndScaledExportUseTheSameCompositionForEveryArtProfile() {
        val catalog = OccasionCatalogLoader.load(context)
        val representatives = catalog.themes.distinctBy { it.profile.id }
        assertEquals("Every art-direction profile needs a representative", 12, representatives.size)
        val representativeVariants = listOf(
            Variant(FrameLayouts.byId(FrameLayoutId.FOUR_GRID)),
            Variant(FrameLayouts.byId(FrameLayoutId.SIX_COLLAGE), version = 1),
        )

        for (occasion in representatives) {
            val artwork = loadWithHeapEvidence(occasion)
            for (variant in representativeVariants) {
                for (captionOn in listOf(false, true)) {
                    val text = "같은 장면".takeIf { captionOn }
                    val date = "2026.09.24".takeIf { captionOn }
                    val input = input(occasion, artwork, variant, emptyList(), text, date)
                    val preview = previewLayout(variant, text, date)
                    val export = exportLayout(variant, text, date)
                    val label = "${occasion.profile.id}/${occasion.id}/${variant.id}/caption=$captionOn"
                    assertProportionalGeometry(label, preview, export)

                    val previewBitmap = bitmapFor(preview)
                    val projectedExport = bitmapFor(preview)
                    try {
                        CollageRenderer.drawScene(Canvas(previewBitmap), input, preview)
                        Canvas(projectedExport).apply {
                            val factor = preview.canvasWidth / export.canvasWidth
                            scale(factor, factor)
                            CollageRenderer.drawScene(this, input, export)
                        }
                        assertNormalizedRendering(label, previewBitmap, projectedExport)
                    } finally {
                        previewBitmap.recycle()
                        projectedExport.recycle()
                    }
                }
            }
        }
    }

    @Test
    fun sequentialAtlasLoadingStaysBoundedAndRejectsAMismatchedTheme() {
        val catalog = OccasionCatalogLoader.load(context)
        OccasionArtwork.clearForTest()
        val runtime = Runtime.getRuntime()
        val managedStart = runtime.totalMemory() - runtime.freeMemory()
        val nativeStart = Debug.getNativeHeapAllocatedSize()
        var managedPeak = managedStart
        var nativePeak = nativeStart
        val firstTheme = catalog.themes.first()
        val firstSheet = loadWithHeapEvidence(firstTheme)

        // Keep only the first reference. All other sheets must be eligible for collection after
        // the production two-entry LRU evicts them.
        catalog.themes.drop(1).forEach { occasion ->
            val sheet = loadWithHeapEvidence(occasion)
            assertEquals("Wrong atlas returned while loading ${occasion.id}", occasion.id, sheet.themeId)
            assertEquals(1536, sheet.bitmap.width)
            assertEquals(1024, sheet.bitmap.height)
            assertTrue("${occasion.id} lost alpha", sheet.bitmap.hasAlpha())
            managedPeak = maxOf(managedPeak, runtime.totalMemory() - runtime.freeMemory())
            nativePeak = maxOf(nativePeak, Debug.getNativeHeapAllocatedSize())
        }

        val reloadedFirst = loadWithHeapEvidence(firstTheme)
        assertNotSame("The two-entry cache retained the first of 88 sheets", firstSheet, reloadedFirst)
        assertEquals(firstTheme.id, reloadedFirst.themeId)

        val wrongTheme = catalog.themes[1]
        val wrongSheet = loadWithHeapEvidence(wrongTheme)
        val variant = Variant(FrameLayouts.byId(FrameLayoutId.TWO_HORIZONTAL))
        val layout = previewLayout(variant, null, null)
        val destination = bitmapFor(layout)
        try {
            try {
                CollageRenderer.drawScene(
                    Canvas(destination),
                    input(firstTheme, wrongSheet, variant, emptyList(), null, null),
                    layout,
                )
                fail("Renderer accepted atlas ${wrongSheet.themeId} for theme ${firstTheme.id}")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains(firstTheme.id))
            }
        } finally {
            destination.recycle()
            OccasionArtwork.clearForTest()
        }
        val managedGrowth = (managedPeak - managedStart).coerceAtLeast(0L)
        val nativeGrowth = (nativePeak - nativeStart).coerceAtLeast(0L)
        val combinedGrowth = managedGrowth + nativeGrowth
        assertTrue(
            "Sequential 88-theme atlas growth exceeded the 128 MiB renderer budget: " +
                "managed=$managedGrowth native=$nativeGrowth combined=$combinedGrowth",
            combinedGrowth < 128L * 1024L * 1024L,
        )
        println(
            "OCCASION_ATLAS_SEQUENCE: count=${catalog.themes.size} " +
                "managedPeak=$managedPeak nativePeak=$nativePeak combinedGrowth=$combinedGrowth " +
                "last=${catalog.themes.last().id}",
        )
    }

    private fun input(
        occasion: OccasionTheme,
        artwork: OccasionArtwork.Sheet,
        variant: Variant,
        photos: List<Bitmap>,
        text: String?,
        date: String?,
    ) = CollageRenderer.Input(
        images = photos.take(variant.style.slots),
        frameStyle = variant.style,
        theme = frameTheme(variant.style),
        text = text,
        dateString = date,
        context = context,
        layoutVersion = variant.version,
        occasionTheme = occasion,
        occasionArtwork = artwork,
    )

    private fun loadWithHeapEvidence(theme: OccasionTheme): OccasionArtwork.Sheet = try {
        OccasionArtwork.load(
            context = context,
            themeId = theme.id,
            assetPath = theme.atlas.assetPath,
            expectedWidth = theme.atlas.width,
            expectedHeight = theme.atlas.height,
        ).also { sheet ->
            assertEquals("Catalog resolved a different atlas for ${theme.id}", theme.id, sheet.themeId)
        }
    } catch (failure: Throwable) {
        val runtime = Runtime.getRuntime()
        throw AssertionError(
            "Atlas load failed at index=${theme.index} id=${theme.id}; " +
                "managedUsed=${runtime.totalMemory() - runtime.freeMemory()} " +
                "managedMax=${runtime.maxMemory()} nativeUsed=${Debug.getNativeHeapAllocatedSize()}",
            failure,
        )
    }

    private fun frameTheme(style: FrameStyle) = FrameCatalog.themes(
        FrameType.entries.first { it.selectCount == style.slots },
    ).first()

    private fun previewLayout(variant: Variant, text: String?, date: String?) =
        CollageLayoutMath.computeForPreview(
            frameStyle = variant.style,
            theme = frameTheme(variant.style),
            bottomCaption = listOfNotNull(text, date).joinToString(" · ").ifBlank { null },
            containerWidthPx = 390f,
            layoutVersion = variant.version,
        )

    private fun exportLayout(
        variant: Variant,
        text: String?,
        date: String?,
    ): CollageLayoutDimensions {
        val theme = frameTheme(variant.style)
        val logical = CollageLayoutMath.compute(
            variant.style, theme, text, date, 390f, variant.version,
        )
        val width = if (variant.style.id == FrameLayoutId.FOUR_VERTICAL) {
            1_650
        } else {
            CollageOutputSize.forScene(logical).first
        }
        return CollageLayoutMath.compute(
            variant.style, theme, text, date, width.toFloat(), variant.version,
        )
    }

    private fun bitmapFor(layout: CollageLayoutDimensions): Bitmap = Bitmap.createBitmap(
        layout.canvasWidth.roundToInt(),
        layout.canvasHeight.roundToInt(),
        Bitmap.Config.ARGB_8888,
    )

    private fun assertSlotCentersKeepPhotoOrder(
        label: String,
        bitmap: Bitmap,
        layout: CollageLayoutDimensions,
    ) {
        layout.cells.forEachIndexed { index, cell ->
            assertEquals(
                "$label photo ${index + 1} center",
                photoColors[index],
                bitmap.getPixel(cell.centerX().toInt(), cell.centerY().toInt()),
            )
        }
    }

    private fun assertPhotoSlotInteriorsKeepPhotoOrder(
        label: String,
        bitmap: Bitmap,
        layout: CollageLayoutDimensions,
    ) {
        val inset = ceil(maxOf(2f, layout.canvasWidth / 390f * 2f)).toInt()
        layout.cells.forEachIndexed { index, cell ->
            val expected = photoColors[index]
            val left = ceil(cell.left).toInt().coerceIn(0, bitmap.width) + inset
            val right = floor(cell.right).toInt().coerceIn(left, bitmap.width) - inset
            val top = ceil(cell.top).toInt().coerceIn(0, bitmap.height) + inset
            val bottom = floor(cell.bottom).toInt().coerceIn(top, bitmap.height) - inset
            assertTrue("$label photo ${index + 1} has no interior", right > left && bottom > top)
            for (y in top until bottom) {
                for (x in left until right) {
                    val actual = bitmap.getPixel(x, y)
                    if (actual != expected) {
                        throw AssertionError(
                            "$label frame artwork entered photo ${index + 1} at $x,$y: " +
                                "expected=$expected actual=$actual",
                        )
                    }
                }
            }
        }
    }

    private fun assertTransparentPhotoSlotInteriors(
        label: String,
        bitmap: Bitmap,
        layout: CollageLayoutDimensions,
    ) {
        val inset = ceil(maxOf(2f, layout.canvasWidth / 390f * 2f)).toInt()
        layout.cells.forEachIndexed { index, cell ->
            val left = ceil(cell.left).toInt().coerceIn(0, bitmap.width) + inset
            val right = floor(cell.right).toInt().coerceIn(left, bitmap.width) - inset
            val top = ceil(cell.top).toInt().coerceIn(0, bitmap.height) + inset
            val bottom = floor(cell.bottom).toInt().coerceIn(top, bitmap.height) - inset
            for (y in top until bottom) {
                for (x in left until right) {
                    val alpha = Color.alpha(bitmap.getPixel(x, y))
                    if (alpha != 0) {
                        throw AssertionError(
                            "$label frame artwork entered photo ${index + 1} at $x,$y alpha=$alpha",
                        )
                    }
                }
            }
        }
    }

    private fun assertEveryFooterMotifDiffersFromBaseline(
        label: String,
        bitmap: Bitmap,
        baseline: Bitmap,
        layout: CollageLayoutDimensions,
        theme: OccasionTheme,
    ) {
        val footerTop = layout.cells.maxOf { it.bottom }
        val footerHeight = layout.canvasHeight - footerTop
        val requestedSide = minOf(footerHeight * 0.58f, layout.canvasWidth * 0.10f)
        theme.placements.footer.items.forEachIndexed { index, item ->
            val centerX = layout.canvasWidth * item.x
            val centerY = footerTop + footerHeight * item.y
            val edgeClearance = minOf(
                centerX,
                layout.canvasWidth - centerX,
                centerY - footerTop,
                layout.canvasHeight - centerY,
            ).coerceAtLeast(0f)
            val halfSide = minOf(requestedSide, edgeClearance * 2f) * 0.52f
            val left = floor(centerX - halfSide).toInt().coerceIn(0, bitmap.width)
            val right = ceil(centerX + halfSide).toInt().coerceIn(left, bitmap.width)
            val top = floor(centerY - halfSide).toInt().coerceIn(0, bitmap.height)
            val bottom = ceil(centerY + halfSide).toInt().coerceIn(top, bitmap.height)
            var found = false
            loop@ for (y in top until bottom) {
                for (x in left until right) {
                    if (bitmap.getPixel(x, y) != baseline.getPixel(x, y)) {
                        found = true
                        break@loop
                    }
                }
            }
            assertTrue("$label footer motif $index did not differ from no-footer baseline", found)
        }
    }

    private fun assertOpaqueAndNonEmpty(
        label: String,
        pixels: IntArray,
        count: Int,
        paperColor: Int,
    ) {
        var differsFromPaper = false
        for (index in 0 until count) {
            val pixel = pixels[index]
            if (Color.alpha(pixel) != 255) {
                throw AssertionError("$label contains non-opaque pixel at index=$index: ${Color.alpha(pixel)}")
            }
            if (pixel != paperColor) differsFromPaper = true
        }
        assertTrue("$label rendered an empty paper-only canvas", differsFromPaper)
    }

    private fun assertCaptionAreaHasNoPhotoPixels(
        label: String,
        pixels: IntArray,
        stride: Int,
        layout: CollageLayoutDimensions,
    ) {
        val area = layout.textArea ?: throw AssertionError("$label has no caption area")
        val left = ceil(area.left).toInt().coerceIn(0, stride)
        val right = floor(area.right).toInt().coerceIn(left, stride)
        val height = pixels.size / stride
        val top = ceil(area.top).toInt().coerceIn(0, height)
        val bottom = floor(area.bottom).toInt().coerceIn(top, height)
        val protectedColors = photoColors.toSet()
        for (y in top until bottom) {
            for (x in left until right) {
                if (pixels[y * stride + x] in protectedColors) {
                    throw AssertionError("$label leaked a photo pixel into caption area at $x,$y")
                }
            }
        }
    }

    private fun assertCaptionBandHasNoFrameArtwork(
        label: String,
        bitmap: Bitmap,
        layout: CollageLayoutDimensions,
    ) {
        val area = layout.textArea ?: throw AssertionError("$label has no caption area")
        val left = ceil(area.left).toInt().coerceIn(0, bitmap.width)
        val right = floor(area.right).toInt().coerceIn(left, bitmap.width)
        val top = ceil(area.top).toInt().coerceIn(0, bitmap.height)
        val bottom = floor(area.bottom).toInt().coerceIn(top, bitmap.height)
        for (y in top until bottom) {
            for (x in left until right) {
                val alpha = Color.alpha(bitmap.getPixel(x, y))
                if (alpha != 0) {
                    throw AssertionError("$label placed frame artwork in caption band at $x,$y alpha=$alpha")
                }
            }
        }
    }

    private fun assertProportionalGeometry(
        label: String,
        preview: CollageLayoutDimensions,
        export: CollageLayoutDimensions,
    ) {
        val factor = preview.canvasWidth / export.canvasWidth
        assertEquals("$label height", preview.canvasHeight, export.canvasHeight * factor, 0.03f)
        assertEquals("$label scale", preview.scale, export.scale * factor, 0.0001f)
        val previewRects = preview.cells + listOfNotNull(preview.headerArea, preview.textArea)
        val exportRects = export.cells + listOfNotNull(export.headerArea, export.textArea)
        assertEquals("$label rect count", previewRects.size, exportRects.size)
        previewRects.zip(exportRects).forEach { (small, large) ->
            assertEquals("$label left", small.left, large.left * factor, 0.03f)
            assertEquals("$label top", small.top, large.top * factor, 0.03f)
            assertEquals("$label right", small.right, large.right * factor, 0.03f)
            assertEquals("$label bottom", small.bottom, large.bottom * factor, 0.03f)
        }
    }

    private fun assertNormalizedRendering(label: String, preview: Bitmap, projected: Bitmap) {
        assertEquals(preview.width, projected.width)
        assertEquals(preview.height, projected.height)
        var totalDelta = 0L
        var largeDifferences = 0
        var samples = 0
        for (y in 0 until preview.height step 2) {
            for (x in 0 until preview.width step 2) {
                val first = preview.getPixel(x, y)
                val second = projected.getPixel(x, y)
                val delta = abs(Color.red(first) - Color.red(second)) +
                    abs(Color.green(first) - Color.green(second)) +
                    abs(Color.blue(first) - Color.blue(second))
                totalDelta += delta
                if (delta > 120) largeDifferences++
                samples++
            }
        }
        val mean = totalDelta.toDouble() / (samples * 3)
        val largeFraction = largeDifferences.toDouble() / samples
        assertTrue("$label mean RGB channel delta=$mean", mean < 3.5)
        assertTrue("$label large-difference pixel fraction=$largeFraction", largeFraction < 0.04)
        assertFalse("$label comparison sampled no pixels", samples == 0)
    }
}
