package com.pocket4cut

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocket4cut.core.util.FileUris
import com.pocket4cut.presentation.result.imageShareIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ResultShareIntentInstrumentedTest {
    @Test
    fun imageUriIsGrantedThroughExtraStreamAndClipData() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pocket4Cut")
        val result = File(root, "results/share_intent_probe.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }
        val uri = FileUris.contentUriForFile(context, result)

        try {
            val intent = imageShareIntent(context, uri)

            assertEquals(Intent.ACTION_SEND, intent.action)
            assertEquals("image/jpeg", intent.type)
            @Suppress("DEPRECATION")
            val extraStream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            assertEquals(uri, extraStream)
            assertNotNull(intent.clipData)
            assertEquals(1, intent.clipData?.itemCount)
            assertEquals(uri, intent.clipData?.getItemAt(0)?.uri)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        } finally {
            result.delete()
        }
    }
}
