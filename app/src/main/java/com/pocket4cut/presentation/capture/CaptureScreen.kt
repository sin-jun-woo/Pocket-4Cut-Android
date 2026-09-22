package com.pocket4cut.presentation.capture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
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
    resumeSessionId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    BackHandler {
        viewModel.pause()
        onBack()
    }
    var completedNavigationId by rememberSaveable { mutableStateOf<String?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { hasPermission = it }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            contentDescription = "카메라 미리보기"
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.bindCamera(lifecycleOwner, previewView)
    }
    DisposableEffect(hasPermission) {
        val activity = context as? Activity
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = true
            controller?.isAppearanceLightNavigationBars = true
        }
    }

    LaunchedEffect(resumeSessionId, frameType) {
        val id = resumeSessionId ?: viewModel.savedSessionId()
        if (id != null) viewModel.restoreSession(id, frameType)
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.pause()
                Lifecycle.Event.ON_RESUME -> {
                    val allowed = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA,
                    ) == PackageManager.PERMISSION_GRANTED
                    hasPermission = allowed
                    if (allowed && viewModel.uiState.value.phase != CapturePhase.CAPTURING) {
                        viewModel.bindCamera(lifecycleOwner, previewView)
                    } else if (!allowed) {
                        viewModel.pause()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission, previewView) {
        if (!hasPermission) return@LaunchedEffect
        var scaled = false
        val detector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                private var baseZoom = 1f
                private var spanAccum = 1f
                override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
                    baseZoom = viewModel.uiState.value.zoomRatio; spanAccum = 1f; scaled = true; return true
                }
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    spanAccum *= d.scaleFactor; viewModel.setZoomRatio(baseZoom * spanAccum); return true
                }
            },
        )
        previewView.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) scaled = false
            detector.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP && !scaled) view.performClick()
            true
        }
    }

    LaunchedEffect(uiState.phase, uiState.sessionId) {
        if (uiState.phase == CapturePhase.COMPLETED && !uiState.sessionId.isNullOrBlank() &&
            completedNavigationId != uiState.sessionId) {
            delay(1500)
            completedNavigationId = uiState.sessionId
            onCompleted(uiState.sessionId!!, frameType)
        }
    }

    val flashAlpha by animateFloatAsState(
        targetValue = if (uiState.flash) 0.65f else 0f,
        animationSpec = if (uiState.flash) snap() else tween(AppAnimation.Duration.normal),
        label = "flash",
    )

    val isCapturing = uiState.phase == CapturePhase.INITIALIZING ||
        uiState.phase == CapturePhase.CAPTURING ||
        uiState.phase == CapturePhase.COUNTDOWN || uiState.phase == CapturePhase.POST_SHOT_DELAY
    val canControl = uiState.phase == CapturePhase.READY ||
        uiState.phase == CapturePhase.FAILED || uiState.phase == CapturePhase.PAUSED
    val showSideCameraControls = hasPermission && canControl

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

            // Bottom gradient: black(0.6) → clear, 220dp (iOS 동일)
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
                        viewModel.pause()
                        onBack()
                    },
                    diameter = 44.dp,
                ) {
                    if (isCapturing) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
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

                if (uiState.phase == CapturePhase.COUNTDOWN) {
                    CountdownTopRightBadge(number = uiState.countdownRemaining)
                } else {
                    Spacer(Modifier.size(44.dp))
                }
            }

            // ── Side camera controls (iOS: 촬영·카운트다운 중에도 표시) ──
            AnimatedVisibility(
                visible = showSideCameraControls,
                enter = fadeIn(tween(AppAnimation.Duration.normal)) + slideInHorizontally { it / 2 },
                exit = fadeOut(tween(AppAnimation.Duration.fast)) + slideOutHorizontally { it / 2 },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = AppSpacing.md)
                    .padding(vertical = 120.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
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

                // Status text (iOS: 촬영 중일 때만 하단 문구 — 카운트다운 전용 문구 없음)
                if (uiState.phase == CapturePhase.CAPTURING) {
                    Text(
                        "${uiState.currentShot} / ${uiState.totalShots} 촬영 중",
                        style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    Spacer(Modifier.height(AppSpacing.xs))
                }

                // Shot dots
                if (isCapturing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = AppSpacing.sm),
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
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .then(
                                        if (isActive) {
                                            Modifier.border(2.dp, Color.White, CircleShape)
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                    }
                }

                // "바로 촬영" capsule button (countdown only)
                if (uiState.phase == CapturePhase.COUNTDOWN) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppLayout.Radius.sm))
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
                    if (uiState.recoveryBlocked) {
                        PrimaryButton(text = "홈으로 돌아가기", onClick = onBack)
                    } else if (uiState.damagedCaptureIndex != null) {
                        PrimaryButton(
                            text = "손상 파일 격리 후 다시 촬영",
                            onClick = viewModel::quarantineDamagedCaptureAndResume,
                        )
                    } else if (uiState.sessionId != null) {
                        PrimaryButton(text = "이어서 촬영", onClick = viewModel::resume)
                    } else if (uiState.phase == CapturePhase.FAILED) {
                        PrimaryButton(
                            text = "카메라 다시 연결",
                            onClick = { viewModel.bindCamera(lifecycleOwner, previewView) },
                        )
                    } else {
                        ShutterButton(onClick = { viewModel.start(frameType) })
                    }
                }
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
// Camera controls retain a dark backing for contrast over the live preview.
// ────────────────────────────────────────────────────────────────

@Composable
private fun GlassmorphismCircle(
    onClick: () -> Unit,
    diameter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(AppLayout.Radius.sm)
    Box(
        modifier = modifier
            .size(diameter)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
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
    val shape = RoundedCornerShape(AppLayout.Radius.sm)
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

/** iOS `countdownTopRightBadge` — 상단 바 우측 작은 숫자만 */
@Composable
private fun CountdownTopRightBadge(number: Int) {
    val shape = RoundedCornerShape(AppLayout.Radius.sm)
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = AppTypography.headline.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White,
        )
    }
}

// ────────────────────────────────────────────────────────────────
// The shutter keeps the familiar circular capture target.
// ────────────────────────────────────────────────────────────────

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AppColors.Accent.pink)
                .border(2.dp, Color.White, CircleShape)
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
            .size(80.dp)
            .drawBehind {
                drawArc(
                    color = AppColors.Accent.pink,
                    startAngle = rotation,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width, size.height),
                )
            },
    )
}

// ────────────────────────────────────────────────────────────────
// Completion state while the captured photos are prepared.
// ────────────────────────────────────────────────────────────────

@Composable
private fun CompletionOverlay() {
    val transition = rememberInfiniteTransition(label = "completion_ring")
    val rotation by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "completion_rotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(AppLayout.Radius.sm))
                    .background(AppColors.Accent.pink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "완료",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .drawBehind {
                        drawArc(
                            color = AppColors.Accent.pink,
                            startAngle = rotation,
                            sweepAngle = 100f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(size.width, size.height),
                        )
                    },
            )
            Text(
                "촬영 완료!",
                style = AppTypography.title2.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
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
            .background(AppColors.Background.primary)
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
