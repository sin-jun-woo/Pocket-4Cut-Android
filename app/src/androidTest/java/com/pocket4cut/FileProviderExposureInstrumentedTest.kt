package com.pocket4cut

import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocket4cut.core.util.FileUris
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
}
