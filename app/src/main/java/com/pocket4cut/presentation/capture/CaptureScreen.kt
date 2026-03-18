package com.pocket4cut.presentation.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket4cut.presentation.navigation.FrameType

@Composable
fun CaptureScreen(
    frameType: FrameType,
    onBack: () -> Unit,
    onCompleted: (sessionId: String, frameType: FrameType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
        },
    )

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.bindCamera(lifecycleOwner, previewView)
        }
    }

    LaunchedEffect(uiState.phase, uiState.sessionId) {
        if (uiState.phase == CapturePhase.COMPLETED) {
            val id = uiState.sessionId
            if (!id.isNullOrBlank()) onCompleted(id, frameType)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("카메라 권한이 필요해")
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = onBack) { Text("뒤로") }
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("권한 허용")
                    }
                }
            }
        } else {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )

            if (uiState.flash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.65f)),
                )
            }

            // Top controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OutlinedButton(onClick = onBack) { Text("뒤로") }

                    Text(
                        text = when (uiState.phase) {
                            CapturePhase.CAPTURING, CapturePhase.COMPLETED ->
                                "${uiState.currentShot}/${uiState.totalShots}"
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )

                    val stopEnabled = uiState.phase == CapturePhase.COUNTDOWN || uiState.phase == CapturePhase.CAPTURING
                    Box(
                        modifier = Modifier.combinedClickable(
                            enabled = stopEnabled,
                            onClick = { },
                            onLongClick = { viewModel.stop() },
                        ),
                    ) {
                        OutlinedButton(onClick = { }, enabled = stopEnabled) {
                            Text("길게눌러 종료")
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    uiState.errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }

                    val canStart =
                        uiState.phase == CapturePhase.READY ||
                            uiState.phase == CapturePhase.IDLE ||
                            uiState.phase == CapturePhase.FAILED
                    Button(
                        onClick = { viewModel.start(frameType) },
                        enabled = canStart,
                    ) {
                        Text("촬영 시작 (${frameType.captureCount}장)")
                    }
                }
            }

            if (uiState.phase == CapturePhase.COUNTDOWN) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.countdownRemaining.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

