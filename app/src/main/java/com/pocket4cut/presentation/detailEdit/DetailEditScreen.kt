package com.pocket4cut.presentation.detailEdit

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.launch

@Composable
fun DetailEditScreen(
    frameType: FrameType,
    sessionId: String,
    selectedIndexes: List<Int>,
    themeId: String,
    onBack: () -> Unit,
    onCompleted: (resultPath: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailEditViewModel = viewModel(),
) {
    LaunchedEffect(frameType, sessionId, selectedIndexes, themeId) {
        viewModel.init(frameType, sessionId, selectedIndexes, themeId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Text("상세 편집", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        runCatching { viewModel.applyAndFinish(sessionId) }
                            .onSuccess { path -> onCompleted(path) }
                        isSaving = false
                    }
                },
                enabled = !isSaving &&
                    uiState.errorMessage == null &&
                    uiState.orderedPaths.isNotEmpty() &&
                    !uiState.isLoading,
            ) {
                Text(if (isSaving) "생성 중" else "완료")
            }
        }

        if (uiState.orderedPaths.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.orderedPaths.indices.forEach { i ->
                    val label = "${i + 1}"
                    TextButton(
                        onClick = { viewModel.selectSlot(i) },
                    ) {
                        val prefix = if (i == uiState.selectedSlot) "▶ " else ""
                        Text("$prefix$label")
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading && uiState.preview == null -> CircularProgressIndicator()
                uiState.preview != null -> {
                    androidx.compose.foundation.Image(
                        bitmap = uiState.preview!!.asImageBitmap(),
                        contentDescription = "preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                uiState.errorMessage != null -> Text(uiState.errorMessage ?: "")
            }
        }

        val adj = uiState.slotAdjusts.getOrNull(uiState.selectedSlot)
        if (adj != null && uiState.orderedPaths.isNotEmpty()) {
            Text("밝기", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = adj.brightness,
                onValueChange = { viewModel.setBrightness(it) },
                valueRange = -0.35f..0.35f,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("대비", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = adj.contrast,
                onValueChange = { viewModel.setContrast(it) },
                valueRange = 0.7f..1.5f,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("채도", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = adj.saturation,
                onValueChange = { viewModel.setSaturation(it) },
                valueRange = 0f..2f,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(onClick = { viewModel.rotateQuarter() }) {
                Text("90° 회전")
            }
        }
    }
}
