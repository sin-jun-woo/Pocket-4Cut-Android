package com.pocket4cut.presentation.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocket4cut.presentation.navigation.FrameType

@Composable
fun EditScreen(
    frameType: FrameType,
    sessionId: String,
    selectedIndexes: List<Int>,
    themeId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Edit (Phase 3에서 본격 구현)", style = MaterialTheme.typography.titleLarge)
        Text("frameType=${frameType.id}", modifier = Modifier.padding(top = 8.dp))
        Text("sessionId=$sessionId", modifier = Modifier.padding(top = 4.dp))
        Text("selectedIndexes=$selectedIndexes", modifier = Modifier.padding(top = 4.dp))
        Text("themeId=$themeId", modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        Row {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
        }
    }
}

