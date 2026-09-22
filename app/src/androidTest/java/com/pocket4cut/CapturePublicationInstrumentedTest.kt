package com.pocket4cut

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.storage.FileImageStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CapturePublicationInstrumentedTest {
    @Test fun invalidJpegNeverOccupiesCaptureSlotAndNextAttemptCanPublish() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "capture-publication-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        try {
            val storage = FileImageStorage(context)
            val id = UUID.randomUUID().toString()
            val invalid = storage.createPendingCaptureFile(id)
            invalid.writeBytes(byteArrayOf(0x12, 0x34, 0x56, 0x78))
            var rejected = false
            try { storage.publishCaptureFile(invalid, id, 1) }
            catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
            assertFalse(invalid.exists())
            assertTrue(storage.getCapturePaths(id).isEmpty())

            val bytes = ByteArrayOutputStream().use { stream ->
                val image = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                try {
                    image.eraseColor(android.graphics.Color.RED)
                    assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 95, stream))
                    stream.toByteArray()
                } finally { image.recycle() }
            }
            val valid = storage.createPendingCaptureFile(id)
            valid.writeBytes(bytes)
            val published = storage.publishCaptureFile(valid, id, 1)
            assertTrue(published.isFile)
            assertFalse(valid.exists())
            assertArrayEquals(bytes, published.readBytes())
            assertTrue(published.canonicalPath.startsWith(
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "Pocket4Cut/captures/$id").canonicalPath + File.separator,
            ))
        } finally {
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }
}
