package com.pocket4cut.presentation.capture

import android.Manifest
import android.content.pm.PackageManager
import android.view.ScaleGestureDetector
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket4cut.core.extensions.findActivity
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton

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

    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.bindCamera(lifecycleOwner, previewView)
    }

    LaunchedEffect(hasPermission, previewView) {
        if (!hasPermission) return@LaunchedEffect
        val detector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private var baseZoom = 1f
            private var spanAccum = 1f
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean { baseZoom = viewModel.uiState.value.zoomRatio; spanAccum = 1f; return true }
            override fun onScale(d: ScaleGestureDetector): Boolean { spanAccum *= d.scaleFactor; viewModel.setZoomRatio(baseZoom * spanAccum); return true }
        })
        previewView.setOnTouchListener { _, event -> detector.onTouchEvent(event); true }
    }

    LaunchedEffect(uiState.phase, uiState.sessionId) {
        if (uiState.phase == CapturePhase.COMPLETED && !uiState.sessionId.isNullOrBlank()) {
            onCompleted(uiState.sessionId!!, frameType)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (!hasPermission) {
            Column(
                modifier = Modifier.fillMaxSize().padding(AppSpacing.xl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("카메라 접근 권한이 필요해요", style = AppTypography.title3, color = AppColors.Text.primary)
                Spacer(Modifier.height(AppSpacing.xs))
                Text("멋진 사진을 촬영하려면\n카메라 접근 권한이 필요합니다.", style = AppTypography.callout, color = AppColors.Text.secondary)
                Spacer(Modifier.height(AppSpacing.xl))
                PrimaryButton(text = "권한 허용", onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, fullWidth = true)
                Spacer(Modifier.height(AppSpacing.sm))
                com.pocket4cut.ui.designsystem.components.SecondaryButton(text = "뒤로", onClick = onBack, fullWidth = true)
            }
        } else {
            // Camera preview
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // Flash overlay
            if (uiState.flash) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.65f)))
            }

            // Top gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))),
            )

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconCircleButton(onClick = onBack, size = 44.dp, variant = IconButtonVariant.DEFAULT) {
                    Icon(Icons.Default.Close, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                }

                // Progress pill
                if (uiState.phase == CapturePhase.CAPTURING || uiState.phase == CapturePhase.COUNTDOWN) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppLayout.Radius.full))
                            .background(AppColors.Overlay.medium)
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    ) {
                        Row {
                            Text("${uiState.currentShot}", style = AppTypography.headline.copy(color = AppColors.Accent.pink))
                            Text(" / ${uiState.totalShots}", style = AppTypography.headline.copy(color = AppColors.Text.primary))
                        }
                    }
                }

                Spacer(Modifier.size(44.dp))
            }

            // Right side controls (camera switch) — only in READY/IDLE/FAILED
            val canSwitch = uiState.phase == CapturePhase.READY || uiState.phase == CapturePhase.IDLE || uiState.phase == CapturePhase.FAILED
            if (canSwitch) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    IconCircleButton(onClick = { viewModel.switchCamera() }, size = 52.dp, variant = IconButtonVariant.DEFAULT) {
                        Icon(Icons.Default.Cameraswitch, null, tint = AppColors.Text.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Bottom area
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.Layout.ctaBottomSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Error
                uiState.errorMessage?.let { msg ->
                    Text(msg, style = AppTypography.caption1, color = AppColors.Semantic.error, modifier = Modifier.padding(bottom = AppSpacing.sm))
                }

                // Start button or progress dots
                val canStart = uiState.phase == CapturePhase.READY || uiState.phase == CapturePhase.IDLE || uiState.phase == CapturePhase.FAILED
                if (canStart) {
                    val pulseAnim = rememberInfiniteTransition(label = "shutter_pulse")
                    val pulseScale by pulseAnim.animateFloat(1f, 1.12f, infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse), label = "sp")
                    val pulseAlpha by pulseAnim.animateFloat(0.4f, 0f, infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse), label = "sa")

                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(96.dp)
                                .scale(pulseScale)
                                .border(2.dp, AppColors.Accent.pink.copy(alpha = pulseAlpha), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AppColors.Accent.pink, AppColors.Accent.pinkLight)))
                                .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable { viewModel.start(frameType) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(Modifier.height(AppSpacing.sm))
                    Text("촬영 시작 (${frameType.captureCount}장)", style = AppTypography.caption1, color = Color.White)
                }

                // Progress dots when shooting
                if (uiState.phase == CapturePhase.CAPTURING || uiState.phase == CapturePhase.COUNTDOWN) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = AppSpacing.md),
                    ) {
                        for (i in 1..uiState.totalShots) {
                            val dotColor = when {
                                i < uiState.currentShot + 1 -> AppColors.Accent.pink
                                i == uiState.currentShot + 1 -> Color.White
                                else -> Color.White.copy(alpha = 0.3f)
                            }
                            val isActive = i == uiState.currentShot + 1
                            Box(
                                modifier = Modifier
                                    .size(if (isActive) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .then(if (isActive) Modifier.border(1.dp, Color.White, CircleShape) else Modifier),
                            )
                        }
                    }
                }
            }

            // Countdown overlay
            if (uiState.phase == CapturePhase.COUNTDOWN) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                ) {
                    // Large countdown number with pink glow
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(AppColors.Accent.pink.copy(alpha = 0.3f), Color.Transparent),
                                            center = Offset(size.width / 2, size.height / 2),
                                            radius = size.minDimension / 2,
                                        ),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = uiState.countdownRemaining.toString(),
                                style = AppTypography.countdown.copy(fontSize = 120.sp),
                                color = Color.White,
                            )
                        }
                    }

                    // Manual shutter at bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .size(76.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .clip(CircleShape)
                            .clickable { viewModel.onManualShutter() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
        }
    }
}
