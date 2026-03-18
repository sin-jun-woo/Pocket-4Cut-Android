package com.pocket4cut.presentation.frameTheme

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocket4cut.frame.FrameDefinitions
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.presentation.navigation.FrameType

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
                    Text(uiState.errorMessage ?: "에러")
                }
            }

            else -> {
                val selectedPaths = selectedIndexes
                    .mapNotNull { idx -> uiState.imagePaths.getOrNull(idx) }

                val activeTheme = uiState.selectedThemeId?.let { FrameDefinitions.byId(it) }

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
                    FramePreview(
                        frameType = frameType,
                        theme = activeTheme,
                        imagePaths = selectedPaths,
                    )
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

@Composable
private fun FramePreview(
    frameType: FrameType,
    theme: FrameTheme?,
    imagePaths: List<String>,
) {
    val bg = theme?.background ?: Color.Black.copy(alpha = 0.05f)
    val border = theme?.border ?: Color.Black.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .padding(14.dp),
    ) {
        when (frameType) {
            FrameType.TWO_CUT -> PreviewGrid(cols = 1, rows = 2, border = border, imagePaths = imagePaths)
            FrameType.FOUR_CUT -> PreviewGrid(cols = 2, rows = 2, border = border, imagePaths = imagePaths)
            FrameType.SIX_CUT -> PreviewGrid(cols = 2, rows = 3, border = border, imagePaths = imagePaths)
        }
    }
}

@Composable
private fun PreviewGrid(
    cols: Int,
    rows: Int,
    border: Color,
    imagePaths: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (c in 0 until cols) {
                    val idx = r * cols + c
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .background(border.copy(alpha = 0.05f)),
                    ) {
                        val path = imagePaths.getOrNull(idx)
                        if (path != null) {
                            AsyncImage(
                                model = path,
                                contentDescription = "preview_$idx",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}


