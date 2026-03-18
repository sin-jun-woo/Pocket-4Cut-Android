package com.pocket4cut.presentation.result

import android.content.Intent
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pocket4cut.core.util.FileUris
import java.io.File

@Composable
fun ResultScreen(
    resultPath: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val file = File(resultPath)
    val uri = FileUris.contentUriForFile(context, file)
    var saveMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Text("결과", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onHome) { Text("메인") }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "result",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    saveMessage = runCatching {
                        val resolver = context.contentResolver
                        val name = file.nameWithoutExtension.ifBlank { "Pocket4Cut_${System.currentTimeMillis()}" }
                        val displayName = "$name.jpg"

                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            if (Build.VERSION.SDK_INT >= 29) {
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pocket4Cut")
                                put(MediaStore.Images.Media.IS_PENDING, 1)
                            }
                        }

                        val collection =
                            if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                        val outUri = resolver.insert(collection, values) ?: error("MediaStore에 저장할 수 없습니다.")
                        resolver.openOutputStream(outUri)?.use { out ->
                            file.inputStream().use { input -> input.copyTo(out) }
                        } ?: error("저장 스트림을 열 수 없습니다.")

                        if (Build.VERSION.SDK_INT >= 29) {
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(outUri, values, null, null)
                        }

                        "갤러리에 저장되었습니다."
                    }.getOrElse { t -> "저장에 실패했습니다: ${t.message}" }
                },
                modifier = Modifier.weight(1f),
            ) { Text("저장") }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "공유"))
                },
                modifier = Modifier.weight(1f),
            ) { Text("공유") }

            OutlinedButton(
                onClick = { saveMessage = null },
                enabled = saveMessage != null,
                modifier = Modifier.weight(1f),
            ) { Text("메시지 삭제") }
        }

        saveMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

