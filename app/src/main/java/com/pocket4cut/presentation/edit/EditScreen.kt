package com.pocket4cut.presentation.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.frame.RenderFilter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun EditScreen(
    frameType: FrameType,
    sessionId: String,
    selectedIndexes: List<Int>,
    themeId: String,
    onBack: () -> Unit,
    onCompleted: (resultPath: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditViewModel = viewModel(),
) {
    LaunchedEffect(frameType, sessionId, selectedIndexes, themeId) {
        viewModel.init(
            frameType = frameType,
            sessionId = sessionId,
            selectedIndexes = selectedIndexes,
            themeId = themeId,
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var isFinalizing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Text("편집", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = {
                    if (isFinalizing) return@Button
                    isFinalizing = true
                    scope.launch {
                        runCatching { viewModel.renderFinalAndSave(sessionId) }
                            .onSuccess { path -> onCompleted(path) }
                        isFinalizing = false
                    }
                },
                enabled = !isFinalizing && uiState.errorMessage == null && uiState.imagePaths.isNotEmpty(),
            ) { Text(if (isFinalizing) "생성 중입니다" else "완료") }
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
                uiState.errorMessage != null -> Text(uiState.errorMessage ?: "오류가 발생했습니다.")
                else -> Text("미리보기를 준비 중입니다.")
            }
        }

        HorizontalDivider()

        Text("필터", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                label = "Soft",
                selected = uiState.filter == RenderFilter.SOFT,
                onClick = { viewModel.setFilter(RenderFilter.SOFT, frameType, themeId) },
            )
            FilterChip(
                label = "Film",
                selected = uiState.filter == RenderFilter.FILM,
                onClick = { viewModel.setFilter(RenderFilter.FILM, frameType, themeId) },
            )
            FilterChip(
                label = "B&W",
                selected = uiState.filter == RenderFilter.BW,
                onClick = { viewModel.setFilter(RenderFilter.BW, frameType, themeId) },
            )
        }

        TextField(
            value = uiState.text,
            onValueChange = { viewModel.setText(it, frameType, themeId) },
            label = { Text("텍스트") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("날짜 표시", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = uiState.showDate,
                onCheckedChange = { viewModel.toggleDate(frameType, themeId) },
            )
        }

        HorizontalDivider()

        Text("순서 변경(간단 스왑)", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { viewModel.swap(0, 1, frameType, themeId) }, enabled = uiState.order.size >= 2) {
                Text("1↔2")
            }
            TextButton(onClick = { viewModel.swap(1, 2, frameType, themeId) }, enabled = uiState.order.size >= 3) {
                Text("2↔3")
            }
            TextButton(onClick = { viewModel.swap(2, 3, frameType, themeId) }, enabled = uiState.order.size >= 4) {
                Text("3↔4")
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        val text = if (selected) "[$label]" else label
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

