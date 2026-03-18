package com.pocket4cut.presentation.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.presentation.navigation.FrameType

@Composable
fun SelectionScreen(
    frameType: FrameType,
    sessionId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var captureCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(sessionId) {
        val storage = FileImageStorage(context)
        captureCount = storage.getCapturePaths(sessionId).size
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Selection (Phase 2에서 본격 구현)",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "frameType=${frameType.id}, sessionId=$sessionId",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "촬영된 사진: ${captureCount ?: "-"}장",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Button(onClick = { /* Phase 2에서 구현 */ }, enabled = false) {
                Text("${frameType.selectCount}장 선택하러 가기")
            }
        }
    }
}

