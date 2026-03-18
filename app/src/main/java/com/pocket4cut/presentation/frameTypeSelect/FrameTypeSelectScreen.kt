package com.pocket4cut.presentation.frameTypeSelect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocket4cut.presentation.navigation.FrameType
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameTypeSelectScreen(
    onBack: () -> Unit,
    onSelected: (FrameType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프레임 타입 선택") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("뒤로") }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "어떤 프레임으로 촬영하시겠어요?",
                style = MaterialTheme.typography.headlineSmall,
            )
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = { onSelected(FrameType.TWO_CUT) }) {
                    Text("2컷 (4장 촬영)")
                }
                Button(onClick = { onSelected(FrameType.FOUR_CUT) }) {
                    Text("4컷 (8장 촬영)")
                }
                Button(onClick = { onSelected(FrameType.SIX_CUT) }) {
                    Text("6컷 (10장 촬영)")
                }
            }
        }
    }
}

