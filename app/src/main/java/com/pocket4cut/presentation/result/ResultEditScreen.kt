package com.pocket4cut.presentation.result

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch

@Composable
fun ResultEditScreen(
    resultPath: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultEditViewModel = viewModel(),
) {
    LaunchedEffect(resultPath) {
        viewModel.load(resultPath)
    }
    val ui by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBack, enabled = !saving) { Text("뒤로") }
            Text("결과 편집", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    scope.launch {
                        runCatching { viewModel.saveToFile() }
                            .onSuccess { onSaved() }
                        saving = false
                    }
                },
                enabled = !saving && ui.preview != null && ui.errorMessage == null,
            ) { Text(if (saving) "저장 중" else "저장") }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                ui.isLoading -> CircularProgressIndicator()
                ui.preview != null -> Image(
                    bitmap = ui.preview!!.asImageBitmap(),
                    contentDescription = "편집 미리보기",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                ui.errorMessage != null -> Text(ui.errorMessage!!, color = MaterialTheme.colorScheme.error)
                else -> Text("불러오는 중…")
            }
        }

        HorizontalDivider()

        Text("밝기", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = ui.brightness,
            onValueChange = { viewModel.setBrightness(it) },
            valueRange = -0.35f..0.35f,
        )

        Text("대비", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = ui.contrast,
            onValueChange = { viewModel.setContrast(it) },
            valueRange = 0.65f..1.5f,
        )

        OutlinedButton(
            onClick = { viewModel.rotate90() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !ui.isLoading && ui.errorMessage == null,
        ) { Text("90° 회전") }
    }
}
