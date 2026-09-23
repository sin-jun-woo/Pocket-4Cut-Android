package com.pocket4cut

import android.content.Intent
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.core.util.FileUris
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileProviderExposureInstrumentedTest {
    @Test
    fun onlyCompletedResultsAreShareable() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pocket4Cut")
        val result = File(root, "results/provider_probe.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val imported = File(root, "imports/probe/source.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        try {
            assertNotNull(FileUris.contentUriForFile(context, result))
            try {
                FileUris.contentUriForFile(context, imported)
                fail("Imported originals must not be exposed through FileProvider")
            } catch (_: IllegalArgumentException) {
                // Expected: no configured root contains imports.
            }
        } finally {
            result.delete()
            imported.delete()
            imported.parentFile?.delete()
        }
    }

    @Test
    fun grantedCompletedResultCanBeReadByIndependentReceiverPackage() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val receiverContext = instrumentation.context
        val root = File(targetContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pocket4Cut")
        val expected = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 0xFF.toByte(), 0xD9.toByte())
        val result = File(root, "results/share_receiver_probe.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(expected)
        }
        val uri = FileUris.contentUriForFile(targetContext, result)

        targetContext.grantUriPermission(
            receiverContext.packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        try {
            val received = receiverContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw AssertionError("Granted result URI could not be opened by the receiver package")
            assertArrayEquals(expected, received)
        } finally {
            targetContext.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            result.delete()
        }
    }
}
