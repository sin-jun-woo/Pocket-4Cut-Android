package com.pocket4cut

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.domain.model.PhotoAdjustments
import com.pocket4cut.domain.model.PhotoCrop
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.frame.CollageLayoutDimensions
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.CollageOutputSize
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.RenderSnapshot
import com.pocket4cut.frame.occasion.OccasionCatalogContract
import com.pocket4cut.frame.occasion.OccasionCatalogLoader
import com.pocket4cut.frame.occasion.OccasionTheme
import com.pocket4cut.frame.rendering.OccasionArtwork
import com.pocket4cut.presentation.detailEdit.DetailEditViewModel
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Opt in with occasionFinalExports=true: 88 real final JPEGs, one per theme across eight layouts.
 * Calls the production detail export, including its region decoder, renderer and publication journal.
 * This is not a Compose/navigation, Photo Picker, MediaStore or 88-by-eight export matrix test.
 */
@RunWith(AndroidJUnit4::class)
class OccasionFinalResultInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test
    fun everyOccasionPublishesAndReopensAnOriginalBasedHighResolutionJpeg() = runBlocking {
        assumeTrue(
            "88 full-resolution JPEG exports are opt-in",
            InstrumentationRegistry.getArguments().getString("occasionFinalExports") == "true",
        )
        val target = instrumentation.targetContext
        assertEquals("Final exports must use the QA package", "com.pocket4cut.qa", target.packageName)
        val root = File(target.cacheDir, "occasion-final-${UUID.randomUUID()}").apply { check(mkdirs()) }
        val context = object : ContextWrapper(target) {
            override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(root, "cache").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(root, "external/$type").apply { mkdirs() }
            override fun getApplicationContext(): Context = this
        }
        val application = IsolatedApplication(context)
        try {
            assertTrue(context.filesDir.canonicalPath.startsWith(root.canonicalPath + File.separator))
            assertEquals(context.filesDir.canonicalFile, application.applicationContext.filesDir.canonicalFile)
            val originals = List(6) { index ->
                File(root, "originals/${index + 1}.jpg").also { writeNumberedOriginal(it, index) }
            }
            val hashes = originals.map(::sha256)
            val catalog = OccasionCatalogLoader.load(context)
            assertEquals(88, catalog.themes.size)
            assertEquals(8, FrameLayouts.all.size)
            val visitedLayouts = mutableSetOf<FrameLayoutId>()
            catalog.themes.forEachIndexed { index, occasion ->
                val style = FrameLayouts.all[index % FrameLayouts.all.size]
                try {
                    verifyFinalResult(context, application, occasion, style, index, originals, hashes)
                    visitedLayouts += style.id
                } catch (failure: Throwable) {
                    throw AssertionError("Final export failed: ${occasion.id}/${style.id}", failure)
                }
                instrumentation.sendStatus(0, Bundle().apply {
                    putString("occasion_final_export", "${index + 1}/88 ${occasion.id} ${style.id}")
                })
            }
            assertEquals(FrameLayoutId.entries.toSet(), visitedLayouts)
            assertEquals(hashes, originals.map(::sha256))
        } finally {
            // Atlas bitmaps may have other UI owners; release cache references, never recycle sheets.
            OccasionArtwork.clearForTest()
            assertTrue(root.canonicalPath.startsWith(target.cacheDir.canonicalPath + File.separator))
            assertTrue("Could not remove isolated final-export fixtures", root.deleteRecursively())
        }
    }

    private suspend fun verifyFinalResult(
        context: Context,
        application: Application,
        occasion: OccasionTheme,
        style: FrameStyle,
        themeIndex: Int,
        originals: List<File>,
        originalHashes: List<String>,
    ) {
        val sessions = SessionDocumentRepository(context)
        val sessionId = UUID.randomUUID().toString()
        val frameType = FrameType.entries.first { it.selectCount == style.slots }
        val baseTheme = FrameCatalog.themes(frameType).first()
        val sourceIndices = List(style.slots) { (themeIndex + it) % originals.size }
        val copies = sourceIndices.mapIndexed { index, sourceIndex ->
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Pocket4Cut/imports/$sessionId/photo_$index.jpg").apply {
                check(parentFile!!.isDirectory || parentFile!!.mkdirs())
                originals[sourceIndex].copyTo(this)
            }
        }
        val photos = copies.mapIndexed { index, file ->
            PhotoRef("photo-${sourceIndices[index]}", "imports/$sessionId/${file.name}", index)
        }
        val ordered = photos.reversed()
        val adjustments = ordered.associate { photo ->
            val sourceIndex = sourceIndices[photos.indexOf(photo)]
            photo.photoId to PhotoAdjustments(
                rotationDegrees = (sourceIndex % 4) * 90,
                flipHorizontal = sourceIndex % 2 == 1,
                crop = PhotoCrop(
                    focusX = if (sourceIndex % 2 == 0) 0.35f else 0.65f,
                    focusY = if (sourceIndex % 3 == 0) 0.4f else 0.6f,
                    zoom = 2f,
                ),
            )
        }
        val store = ViewModelStore()
        var created = false
        var resultFile: File? = null
        try {
            val document = sessions.create(SessionDocument(
                sessionId = sessionId,
                createdAt = 1_700_000_000_000L,
                captureCount = style.slots,
                selectedCount = style.slots,
                frameTypeId = frameType.id,
                inputSource = InputSource.ALBUM,
                stage = SessionStage.DETAIL,
                photos = photos,
                draft = SessionDraft(
                    selectedPhotoIdsInOrder = ordered.map { it.photoId },
                    adjustmentsByPhotoId = adjustments,
                    layoutId = style.id.name,
                    layoutVersion = 2,
                    themeId = baseTheme.id,
                    occasionThemeId = occasion.id,
                    occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
                    frameStep = "occasion",
                    backgroundType = "occasion",
                    caption = if (themeIndex % 2 == 0) "Final ${themeIndex + 1}" else "",
                    showDate = themeIndex % 2 == 0,
                    dateText = "2026.09.27",
                ),
            ))
            created = true
            val snapshot = RenderSnapshot.from(document, sessions, frameType)
            assertEquals(ordered.map { it.photoId }, snapshot.photoIdsInOrder)
            assertEquals(ordered.map { sessions.resolvePhotoPath(it).absolutePath }, snapshot.imagePathsInOrder)
            val layout = outputLayout(snapshot)
            lateinit var viewModel: DetailEditViewModel
            instrumentation.runOnMainSync {
                viewModel = ViewModelProvider(
                    store, ViewModelProvider.AndroidViewModelFactory(application),
                )[DetailEditViewModel::class.java]
                viewModel.initialize(
                    // No preview/proxy pixels are available: export must decode the original files.
                    baseImages = emptyList(),
                    imagePaths = snapshot.imagePathsInOrder,
                    photoIds = snapshot.photoIdsInOrder,
                    initialAdjustments = snapshot.adjustmentsByPhotoId,
                    layoutVersion = snapshot.layoutVersion,
                    dateText = snapshot.dateText.orEmpty(),
                    frameType = frameType,
                    frameStyle = snapshot.frameStyle,
                    theme = snapshot.theme,
                    frameColor = snapshot.frameColor,
                    globalFilter = snapshot.filterId,
                    customText = snapshot.caption,
                    showDate = snapshot.dateText != null,
                    sessionId = sessionId,
                    selectedIndexes = ordered.map(photos::indexOf),
                )
            }
            // Production owns/recycles the full result and every decoded slot before returning.
            val saved = File(viewModel.renderFinalCollage())
            resultFile = saved
            val reopenedRepository = SessionDocumentRepository(context)
            val reopened = requireNotNull(reopenedRepository.getById(sessionId))
            val result = reopened.results.single()
            assertEquals(SessionStage.RESULT, reopened.stage)
            assertEquals(snapshot.revision + 1, reopened.revision)
            assertEquals(snapshot.revision, result.sourceRevision)
            assertEquals(document.draft, reopened.draft)
            assertEquals(occasion.id, reopened.draft.occasionThemeId)
            assertEquals(OccasionCatalogContract.SESSION_DESIGN_VERSION, reopened.draft.occasionDesignVersion)
            assertEquals(snapshot.cropTransformsInOrder,
                RenderSnapshot.from(reopened, reopenedRepository, frameType).cropTransformsInOrder)
            assertEquals("${sessionId}_${result.resultId}.jpg", saved.name)
            assertEquals(saved.canonicalFile, reopenedRepository.resolveResultPath(result).canonicalFile)
            assertEquals(layout.canvasWidth.roundToInt(), result.width)
            assertEquals(layout.canvasHeight.roundToInt(), result.height)
            assertTrue("Expected a full-resolution result", result.width.toLong() * result.height > 1_000_000L)
            assertTrue(result.width.toLong() * result.height <= 16_000_000L)
            assertTrue(result.width <= 8192 && result.height <= 8192)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(saved.absolutePath, bounds)
            assertEquals("image/jpeg", bounds.outMimeType)
            assertEquals(result.width, bounds.outWidth)
            assertEquals(result.height, bounds.outHeight)
            assertJpegPhotoOrderAndCrop(saved, layout, ordered, photos, sourceIndices, adjustments)
            assertEquals(listOf(result), reopenedRepository.getById(sessionId)!!.results)
            assertTrue(reopenedRepository.listResultPublicationIssues(sessionId).isEmpty())
            copies.forEachIndexed { index, file -> assertEquals(originalHashes[sourceIndices[index]], sha256(file)) }
            assertEquals(originalHashes, originals.map(::sha256))
            val atlas = OccasionArtwork.diagnosticsForTest()
            assertTrue(atlas.cachedSheetCount <= 2)
            assertEquals(0, atlas.inFlightDecodeCount)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            if (created) sessions.requestDelete(sessionId)
        }
        assertNull(SessionDocumentRepository(context).getCurrentById(sessionId))
        assertFalse("Result must be removed before the next theme", requireNotNull(resultFile).exists())
        assertTrue("Owned import copies must be removed", copies.none(File::exists))
    }

    private fun outputLayout(snapshot: RenderSnapshot): CollageLayoutDimensions {
        val logical = CollageLayoutMath.compute(
            snapshot.frameStyle, snapshot.theme, snapshot.caption.takeIf(String::isNotBlank),
            snapshot.dateText, 390f, snapshot.layoutVersion,
        )
        val width = if (snapshot.frameStyle.id == FrameLayoutId.FOUR_VERTICAL) 1650
            else CollageOutputSize.forScene(logical).first
        return CollageLayoutMath.compute(
            snapshot.frameStyle, snapshot.theme, snapshot.caption.takeIf(String::isNotBlank),
            snapshot.dateText, width.toFloat(), snapshot.layoutVersion,
        )
    }

    private fun assertJpegPhotoOrderAndCrop(
        file: File,
        layout: CollageLayoutDimensions,
        ordered: List<PhotoRef>,
        photos: List<PhotoRef>,
        sourceIndices: List<Int>,
        adjustments: Map<String, PhotoAdjustments>,
    ) {
        val decoded = requireNotNull(BitmapDecoding.decodeSampled(file.absolutePath, 1024, 1_000_000L))
        try {
            layout.cells.forEachIndexed { index, cell ->
                val photo = ordered[index]
                val originalIndex = sourceIndices[photos.indexOf(photo)]
                val crop = adjustments.getValue(photo.photoId).crop
                // At zoom 2 these interior focal points are not edge-clamped. Rotation and mirror
                // must still put the same original-space focus at the output slot center.
                val expected = fixtureColor(originalIndex, crop.focusX, crop.focusY)
                val x = (cell.centerX() * decoded.width / layout.canvasWidth).roundToInt()
                    .coerceIn(0, decoded.width - 1)
                val y = (cell.centerY() * decoded.height / layout.canvasHeight).roundToInt()
                    .coerceIn(0, decoded.height - 1)
                val actual = decoded.getPixel(x, y)
                val delta = maxOf(abs(Color.red(actual) - Color.red(expected)),
                    abs(Color.green(actual) - Color.green(expected)), abs(Color.blue(actual) - Color.blue(expected)))
                assertTrue("Slot $index lost ${photo.photoId} order/crop: RGB delta=$delta", delta <= 12)
            }
        } finally {
            decoded.recycle()
        }
    }

    private fun writeNumberedOriginal(file: File, index: Int) {
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val bitmap = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888)
        try {
            val row = IntArray(bitmap.width)
            for (y in 0 until bitmap.height) {
                for (x in row.indices) {
                    row[x] = fixtureColor(index, x.toFloat() / (bitmap.width - 1), y.toFloat() / (bitmap.height - 1))
                }
                bitmap.setPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            }
            Canvas(bitmap).drawText("${index + 1}", 40f, 120f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 100f
            })
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 98, it)) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun fixtureColor(index: Int, x: Float, y: Float): Int =
        Color.rgb(30 + index * 32, (30 + x * 180).roundToInt(), (30 + y * 180).roundToInt())

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private class IsolatedApplication(context: Context) : Application() {
        init { attachBaseContext(context) }
    }
}
