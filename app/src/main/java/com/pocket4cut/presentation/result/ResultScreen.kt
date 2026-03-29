package com.pocket4cut.presentation.result

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocket4cut.core.util.FileUris
import com.pocket4cut.presentation.settings.AppSettings
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.AppToast
import com.pocket4cut.ui.designsystem.components.AppToastType
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import com.pocket4cut.ui.designsystem.components.SecondaryButton
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ConfettiSpec(
    val xFrac: Float,
    val yFrac: Float,
    val driftX: Dp,
    val driftY: Dp,
    val rotStart: Float,
    val rotDelta: Float,
    val width: Dp,
    val height: Dp,
    val color: Color,
)

private val BottomChromeReserve = 240.dp

@Composable
fun ResultScreen(
    resultPath: String,
    onHome: () -> Unit,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val file = remember(resultPath) { File(resultPath) }
    val uri = remember(file, context) { FileUris.contentUriForFile(context, file) }

    var isSaving by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var showDeferredChrome by remember { mutableStateOf(false) }
    var confettiPieces by remember { mutableStateOf<List<ConfettiSpec>>(emptyList()) }
    var confettiTargetProgress by remember { mutableFloatStateOf(0f) }

    val confettiProgress by animateFloatAsState(
        targetValue = confettiTargetProgress,
        animationSpec = tween(durationMillis = 1_500),
        label = "confetti",
    )

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(AppToastType.Success) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(1_500)
        showDeferredChrome = true
    }

    LaunchedEffect(isSaved) {
        if (!isSaved) return@LaunchedEffect
        val random = Random(System.currentTimeMillis())
        confettiPieces = List(30) {
            ConfettiSpec(
                xFrac = random.nextFloat() * 0.92f + 0.04f,
                yFrac = random.nextFloat() * 0.75f + 0.05f,
                driftX = random.nextInt(-140, 140).dp,
                driftY = random.nextInt(-120, 320).dp,
                rotStart = random.nextFloat() * 360f,
                rotDelta = random.nextFloat() * 540f - 270f,
                width = random.nextInt(6, 14).dp,
                height = random.nextInt(12, 28).dp,
                color = if (random.nextBoolean()) AppColors.Accent.pink else AppColors.Accent.pinkLight,
            )
        }
        confettiTargetProgress = 1f
        delay(1_500)
        confettiPieces = emptyList()
    }

    LaunchedEffect(toastMessage) {
        val m = toastMessage ?: return@LaunchedEffect
        delay(2_800)
        if (toastMessage == m) toastMessage = null
    }

    fun performShare() {
        if (onShare != null) {
            onShare()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "공유"))
    }

    fun performSave() {
        if (isSaving || isSaved) return
        scope.launch {
            isSaving = true
            val saveResult = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = context.contentResolver
                    val name = file.nameWithoutExtension.ifBlank { "Pocket4Cut_${System.currentTimeMillis()}" }
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= 29) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pocket4Cut")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }
                    val collection =
                        if (Build.VERSION.SDK_INT >= 29) {
                            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        } else {
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                    val outUri = resolver.insert(collection, values) ?: error("insert failed")
                    resolver.openOutputStream(outUri)?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                    } ?: error("openOutputStream failed")
                    if (Build.VERSION.SDK_INT >= 29) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(outUri, values, null, null)
                    }
                }
            }
            isSaving = false
            saveResult.fold(
                onSuccess = {
                    isSaved = true
                    toastType = AppToastType.Success
                    toastMessage = "갤러리에 저장 완료!"
                },
                onFailure = { e ->
                    toastType = AppToastType.Error
                    toastMessage = e.message?.takeIf { it.isNotBlank() } ?: "저장에 실패했어요"
                },
            )
        }
    }

    var didAttemptAutoSave by remember(resultPath) { mutableStateOf(false) }
    LaunchedEffect(resultPath) {
        if (didAttemptAutoSave) return@LaunchedEffect
        if (!AppSettings.autoSaveToGallery) return@LaunchedEffect
        if (!file.exists()) return@LaunchedEffect
        didAttemptAutoSave = true
        performSave()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppColors.Accent.pink.copy(alpha = 0.06f), Color.Transparent),
                        radius = size.minDimension * 0.7f,
                    ),
                    center = center.copy(y = size.height * 0.3f),
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = BottomChromeReserve),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppSpacing.xxxl,
                        start = AppSpacing.Screen.horizontal,
                        end = AppSpacing.Screen.horizontal,
                        bottom = AppSpacing.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = showDeferredChrome,
                    enter = scaleIn(
                        initialScale = 0.6f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) + fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ),
                ) {
                    IconCircleButton(onClick = onHome, variant = IconButtonVariant.SOLID) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "홈",
                            tint = AppColors.Text.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = AppColors.Accent.pink,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(
                        text = "완성!",
                        style = AppTypography.largeTitle,
                        color = AppColors.Text.primary,
                    )
                    Spacer(Modifier.width(AppSpacing.xs))
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = AppColors.Accent.pink,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(AppSpacing.sm))
                Text(
                    text = "소중한 순간이 담긴 사진이 완성되었어요",
                    style = AppTypography.callout,
                    color = AppColors.Text.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal),
                )
            }

            val imageShape = RoundedCornerShape(AppLayout.Radius.xl)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .shadow(
                            elevation = 28.dp,
                            shape = imageShape,
                            clip = false,
                            ambientColor = AppColors.Accent.pink.copy(alpha = 0.12f),
                            spotColor = AppColors.Accent.pink.copy(alpha = 0.12f),
                        )
                        .shadow(
                            elevation = 20.dp,
                            shape = imageShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.45f),
                            spotColor = Color.Black.copy(alpha = 0.45f),
                        )
                        .clip(imageShape),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(uri).build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }

        if (confettiPieces.isNotEmpty()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = BottomChromeReserve),
            ) {
                val w = maxWidth
                val h = maxHeight
                val p = confettiProgress
                confettiPieces.forEach { spec ->
                    val baseX = w * spec.xFrac
                    val baseY = h * spec.yFrac
                    Box(
                        modifier = Modifier
                            .offset(
                                x = baseX + spec.driftX * p - spec.width / 2,
                                y = baseY + spec.driftY * p - spec.height / 2,
                            )
                            .size(spec.width, spec.height)
                            .graphicsLayer {
                                rotationZ = spec.rotStart + spec.rotDelta * p
                            }
                            .clip(RoundedCornerShape(4.dp))
                            .background(spec.color),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary),
        ) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = AppColors.Border.light,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(top = AppSpacing.md, bottom = AppSpacing.Layout.ctaBottomSpace),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                when {
                    isSaved -> SavedResultButton()
                    isSaving -> SavingResultButton()
                    else -> PrimaryButton(
                        text = "갤러리에 저장",
                        onClick = { performSave() },
                        fullWidth = true,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                tint = AppColors.Text.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }

                SecondaryButton(
                    text = "공유하기",
                    onClick = { performShare() },
                    fullWidth = true,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = AppColors.Text.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )

                AnimatedVisibility(
                    visible = showDeferredChrome,
                    enter = fadeIn(animationSpec = tween(300)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppLayout.Height.Button.lg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onHome,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "홈으로 돌아가기",
                            style = AppTypography.callout,
                            color = AppColors.Text.tertiary,
                        )
                    }
                }
            }
        }

        toastMessage?.let { msg ->
            AppToast(
                message = msg,
                type = toastType,
                onDismiss = { toastMessage = null },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = AppSpacing.md)
                    .padding(horizontal = AppSpacing.Screen.horizontal),
            )
        }
    }
}

@Composable
private fun SavingResultButton() {
    val shape = RoundedCornerShape(AppLayout.Radius.md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppLayout.Height.Button.lg)
            .clip(shape)
            .background(AppColors.Accent.pink),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = AppColors.Text.primary,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(AppSpacing.xs))
        Text(
            text = "저장 중...",
            style = AppTypography.headline,
            color = AppColors.Text.primary,
        )
    }
}

@Composable
private fun SavedResultButton() {
    val shape = RoundedCornerShape(AppLayout.Radius.md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppLayout.Height.Button.lg)
            .clip(shape)
            .background(AppColors.Semantic.success),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = AppColors.Text.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(AppSpacing.xs))
        Text(
            text = "저장 완료",
            style = AppTypography.headline,
            color = AppColors.Text.primary,
        )
    }
}
