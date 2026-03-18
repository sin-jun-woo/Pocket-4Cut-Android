package com.pocket4cut.presentation.frameTheme

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.FrameDefinitions
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.frame.RenderFilter
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FrameThemeScreen(
    frameType: FrameType,
    sessionId: String,
    selectedIndexes: List<Int>,
    onBack: () -> Unit,
    onDone: (themeId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FrameThemeViewModel = viewModel(),
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val uiState by viewModel.uiState.collectAsState()
    val themes = FrameDefinitions.themesFor(frameType)
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPreviewLoading by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Text("프레임 테마", style = MaterialTheme.typography.titleMedium)
            val doneEnabled = !uiState.selectedThemeId.isNullOrBlank()
            Button(
                onClick = { onDone(uiState.selectedThemeId ?: return@Button) },
                enabled = doneEnabled,
            ) { Text("다음") }
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.errorMessage ?: "오류가 발생했습니다.")
                }
            }

            else -> {
                val selectedPaths = selectedIndexes
                    .mapNotNull { idx -> uiState.imagePaths.getOrNull(idx) }

                val activeTheme = uiState.selectedThemeId?.let { FrameDefinitions.byId(it) }
                LaunchedEffect(frameType, selectedPaths, activeTheme?.id) {
                    previewBitmap = null
                    if (activeTheme == null || selectedPaths.isEmpty()) return@LaunchedEffect
                    isPreviewLoading = true
                    previewBitmap = runCatching {
                        withContext(Dispatchers.IO) {
                            val bitmaps = selectedPaths
                                .take(frameType.selectCount)
                                .mapNotNull { BitmapDecoding.decodeSampled(it, reqSize = 720) }
                            CollageRenderer.render(
                                frameType = frameType,
                                theme = activeTheme,
                                bitmaps = bitmaps,
                                filter = RenderFilter.SOFT,
                                text = null,
                                dateText = null,
                                targetWidth = 720,
                            )
                        }
                    }.getOrNull()
                    isPreviewLoading = false
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = "미리보기",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(18.dp))
                            .background((activeTheme?.background ?: Color.Black.copy(alpha = 0.05f)))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            isPreviewLoading -> CircularProgressIndicator()
                            previewBitmap != null -> {
                                androidx.compose.foundation.Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = "frame_preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                            else -> Text("미리보기를 준비 중입니다.")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(themes) { theme ->
                        ThemeRow(
                            theme = theme,
                            selected = uiState.selectedThemeId == theme.id,
                            onClick = { viewModel.selectTheme(theme.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(
    theme: FrameTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) theme.accent.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(theme.name, style = MaterialTheme.typography.titleMedium)
            Text(theme.id, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.accent),
        )
    }
}

// (프리뷰는 CollageRenderer로 렌더링하여 기기별 레이아웃 깨짐을 방지합니다.)
