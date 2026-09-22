package com.pocket4cut

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.storage.FileImageStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CaptureDamageRecoveryInstrumentedTest {
    @Test fun damagedUnrecordedCaptureIsPreservedWhileItsSlotCanBeRetaken() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "capture-damage-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        try {
            val storage = FileImageStorage(context)
            val id = UUID.randomUUID().toString()
            val validBytes = ByteArrayOutputStream().use { output ->
                val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(android.graphics.Color.RED)
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
                    output.toByteArray()
                } finally { bitmap.recycle() }
            }
            val firstPending = storage.createPendingCaptureFile(id).apply { writeBytes(validBytes) }
            val first = storage.publishCaptureFile(firstPending, id, 1)
            val firstBytes = first.readBytes()
            val damaged = storage.createCaptureFile(id, 2)
            val damagedBytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
            damaged.writeBytes(damagedBytes)

            assertTrue(storage.isPublishedCaptureValid(id, 1))
            assertFalse(storage.isPublishedCaptureValid(id, 2))
            val quarantined = storage.quarantineDamagedCapture(id, 2)!!
            assertFalse(damaged.exists())
            assertArrayEquals(damagedBytes, quarantined.readBytes())
            assertArrayEquals(firstBytes, first.readBytes())
            assertEquals(listOf(first.absolutePath), storage.getCapturePaths(id))
            assertNull(storage.quarantineDamagedCapture(id, 2))

            val secondPending = storage.createPendingCaptureFile(id).apply { writeBytes(validBytes) }
            val retaken = storage.publishCaptureFile(secondPending, id, 2)
            assertTrue(storage.isPublishedCaptureValid(id, 2))
            assertArrayEquals(validBytes, retaken.readBytes())
            assertArrayEquals(damagedBytes, quarantined.readBytes())
            var rejected = false
            try { storage.quarantineDamagedCapture(id, 2) }
            catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
            assertTrue(retaken.exists())
        } finally {
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }
}
