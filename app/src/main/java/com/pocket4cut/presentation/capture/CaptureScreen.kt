package com.pocket4cut.presentation.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.ScaleGestureDetector
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocket4cut.core.extensions.findActivity
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import com.pocket4cut.ui.designsystem.components.SecondaryButton
import kotlinx.coroutines.delay

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
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { hasPermission = it }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.bindCamera(lifecycleOwner, previewView)
    }

    LaunchedEffect(hasPermission, previewView) {
        if (!hasPermission) return@LaunchedEffect
        val detector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                private var baseZoom = 1f
                private var spanAccum = 1f
                override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
                    baseZoom = viewModel.uiState.value.zoomRatio; spanAccum = 1f; return true
                }
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    spanAccum *= d.scaleFactor; viewModel.setZoomRatio(baseZoom * spanAccum); return true
                }
            },
        )
        previewView.setOnTouchListener { _, event -> detector.onTouchEvent(event); true }
    }

    LaunchedEffect(uiState.phase, uiState.sessionId) {
        if (uiState.phase == CapturePhase.COMPLETED && !uiState.sessionId.isNullOrBlank()) {
            delay(1500)
            onCompleted(uiState.sessionId!!, frameType)
        }
    }

    var cancelConfirmed by remember { mutableStateOf(false) }
    LaunchedEffect(cancelConfirmed) {
        if (cancelConfirmed) {
            delay(2000)
            cancelConfirmed = false
        }
    }

    val flashAlpha by animateFloatAsState(
        targetValue = if (uiState.flash) 0.65f else 0f,
        animationSpec = if (uiState.flash) snap() else tween(AppAnimation.Duration.normal),
        label = "flash",
    )

    val isCapturing = uiState.phase == CapturePhase.CAPTURING || uiState.phase == CapturePhase.COUNTDOWN
    val canControl = uiState.phase == CapturePhase.READY ||
        uiState.phase == CapturePhase.IDLE ||
        uiState.phase == CapturePhase.FAILED

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (!hasPermission) {
            PermissionDeniedView(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                },
                onBack = onBack,
            )
        } else {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // Vignette: radial gradient clear → black(0.3)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.8f,
                            ),
                        )
                    },
            )

            // Bottom gradient: black(0.6) → clear, 220dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        ),
                    ),
            )

            // Top gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                        ),
                    ),
            )

            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassmorphismCircle(
                    onClick = {
                        if (isCapturing) {
                            if (cancelConfirmed) {
                                viewModel.stop()
                                onBack()
                            } else {
                                cancelConfirmed = true
                            }
                        } else {
                            onBack()
                        }
                    },
                    diameter = 44.dp,
                ) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                if (isCapturing) {
                    GlassmorphismCapsule {
                        Row {
                            Text(
                                "${uiState.currentShot}",
                                style = AppTypography.headline.copy(color = AppColors.Accent.pink),
                            )
                            Text(
                                " / ${uiState.totalShots}",
                                style = AppTypography.headline.copy(color = Color.White),
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Spacer(Modifier.size(44.dp))
            }

            // Cancel-confirmed toast
            AnimatedVisibility(
                visible = cancelConfirmed,
                enter = fadeIn(tween(AppAnimation.Duration.fast)) + slideInVertically { -it },
                exit = fadeOut(tween(AppAnimation.Duration.fast)) + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp),
            ) {
                GlassmorphismCapsule {
                    Text(
                        "한 번 더 누르면 종료돼요",
                        style = AppTypography.footnote,
                        color = Color.White,
                    )
                }
            }

            // ── Side camera controls (ready / idle / failed only) ──
            AnimatedVisibility(
                visible = canControl,
                enter = fadeIn(tween(AppAnimation.Duration.normal)) + slideInHorizontally { it / 2 },
                exit = fadeOut(tween(AppAnimation.Duration.fast)) + slideOutHorizontally { it / 2 },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = AppSpacing.md),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    GlassmorphismCircle(onClick = { viewModel.switchCamera() }, diameter = 52.dp) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "카메라 전환", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    GlassmorphismCircle(
                        onClick = {
                            val s = viewModel.uiState.value
                            val step = (s.maxZoom - s.minZoom) / 4f
                            viewModel.setZoomRatio(s.zoomRatio + step)
                        },
                        diameter = 52.dp,
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "확대", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    GlassmorphismCircle(
                        onClick = {
                            val s = viewModel.uiState.value
                            val step = (s.maxZoom - s.minZoom) / 4f
                            viewModel.setZoomRatio(s.zoomRatio - step)
                        },
                        diameter = 52.dp,
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "축소", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // ── Bottom area ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.Layout.ctaBottomSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                uiState.errorMessage?.let { msg ->
                    Text(
                        msg,
                        style = AppTypography.caption1,
                        color = AppColors.Semantic.error,
                        modifier = Modifier.padding(bottom = AppSpacing.sm),
                    )
                }

                // Status text
                when (uiState.phase) {
                    CapturePhase.COUNTDOWN -> {
                        Text(
                            "${uiState.countdownRemaining}초 후 촬영",
                            style = AppTypography.headline,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(AppSpacing.sm))
                    }
                    CapturePhase.CAPTURING -> {
                        Text(
                            "${uiState.currentShot} / ${uiState.totalShots} 촬영 중",
                            style = AppTypography.headline,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(AppSpacing.sm))
                    }
                    else -> {}
                }

                // Shot dots
                if (isCapturing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = AppSpacing.md),
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
                                    .background(dotColor),
                            )
                        }
                    }
                }

                // "바로 촬영" capsule button (countdown only)
                if (uiState.phase == CapturePhase.COUNTDOWN) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppLayout.Radius.full))
                            .background(AppColors.Accent.pink)
                            .clickable { viewModel.onManualShutter() }
                            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "바로 촬영",
                            style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.height(AppSpacing.md))
                }

                // Shooting progress ring (capturing only)
                if (uiState.phase == CapturePhase.CAPTURING) {
                    ShootingProgressRing()
                    Spacer(Modifier.height(AppSpacing.md))
                }

                // Shutter button (ready / idle / failed)
                if (canControl) {
                    ShutterButton(onClick = { viewModel.start(frameType) })
                    Spacer(Modifier.height(AppSpacing.sm))
                    Text(
                        "촬영 시작 (${frameType.captureCount}장)",
                        style = AppTypography.caption1,
                        color = Color.White,
                    )
                }
            }

            // ── Countdown overlay ──
            if (uiState.phase == CapturePhase.COUNTDOWN) {
                CountdownOverlay(number = uiState.countdownRemaining)
            }

            // ── Completion overlay ──
            if (uiState.phase == CapturePhase.COMPLETED) {
                CompletionOverlay()
            }

            // ── Shutter flash ──
            if (flashAlpha > 0f) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha)))
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Glassmorphism primitives (ultraThinMaterial + black(0.5) 근사)
// ────────────────────────────────────────────────────────────────

@Composable
private fun GlassmorphismCircle(
    onClick: () -> Unit,
    diameter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun GlassmorphismCapsule(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(AppLayout.Radius.full)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ────────────────────────────────────────────────────────────────
// Shutter button — 80dp pink circle + pulse ring + camera icon
// ────────────────────────────────────────────────────────────────

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    val pulseTransition = rememberInfiniteTransition(label = "shutter_pulse")
    val pulseScale by pulseTransition.animateFloat(
        1f, 1.12f,
        infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse_scale",
    )
    val pulseAlpha by pulseTransition.animateFloat(
        0.4f, 0f,
        infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse_alpha",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(96.dp)
                .scale(pulseScale)
                .border(2.dp, AppColors.Accent.pink.copy(alpha = pulseAlpha), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(AppColors.Accent.pink, AppColors.Accent.pinkLight)),
                )
                .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "촬영",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Shooting progress ring — spinning arc while capturing
// ────────────────────────────────────────────────────────────────

@Composable
private fun ShootingProgressRing(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shooting_ring")
    val rotation by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "ring_rotation",
    )

    Box(
        modifier = modifier
            .size(60.dp)
            .drawBehind {
                drawArc(
                    color = Color(0xFFFF6B9D),
                    startAngle = rotation,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width, size.height),
                )
            },
    )
}

// ────────────────────────────────────────────────────────────────
// Countdown overlay — glassmorphism circle + large number (120sp)
// ────────────────────────────────────────────────────────────────

@Composable
private fun CountdownOverlay(number: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AppColors.Accent.pink.copy(alpha = 0.2f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension / 2f,
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = AppTypography.countdown.copy(fontSize = 120.sp),
                color = Color.White,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Completion overlay — dark bg + checkmark + spinning ring + text
// ────────────────────────────────────────────────────────────────

@Composable
private fun CompletionOverlay() {
    val transition = rememberInfiniteTransition(label = "completion_ring")
    val rotation by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "completion_rotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .drawBehind {
                            drawArc(
                                color = Color(0xFFFF6B9D),
                                startAngle = rotation,
                                sweepAngle = 90f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                                size = Size(size.width, size.height),
                            )
                        },
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AppColors.Accent.pink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "완료",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Text("촬영 완료!", style = AppTypography.title2, color = Color.White)
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Permission denied view — 설정 링크 포함
// ────────────────────────────────────────────────────────────────

@Composable
private fun PermissionDeniedView(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = AppColors.Text.secondary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(AppSpacing.xl))
        Text(
            "카메라 접근 권한이 필요해요",
            style = AppTypography.title3,
            color = AppColors.Text.primary,
        )
        Spacer(Modifier.height(AppSpacing.xs))
        Text(
            "멋진 사진을 촬영하려면\n카메라 접근 권한이 필요합니다.",
            style = AppTypography.callout,
            color = AppColors.Text.secondary,
        )
        Spacer(Modifier.height(AppSpacing.xl))
        PrimaryButton(text = "권한 허용", onClick = onRequestPermission, fullWidth = true)
        Spacer(Modifier.height(AppSpacing.sm))
        SecondaryButton(text = "설정에서 허용", onClick = onOpenSettings, fullWidth = true)
        Spacer(Modifier.height(AppSpacing.sm))
        SecondaryButton(text = "뒤로", onClick = onBack, fullWidth = true)
    }
}
