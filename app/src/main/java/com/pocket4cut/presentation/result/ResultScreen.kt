package com.pocket4cut.presentation.result

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocket4cut.data.export.ExportMode
import com.pocket4cut.data.export.ExportOutcome
import com.pocket4cut.data.export.GalleryExporter
import com.pocket4cut.data.local.SessionDocumentRepository
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BottomChromeReserve = 184.dp

/** A damaged, unrelated session must not hide a healthy result. */
internal suspend fun findResultLink(
    sessions: SessionDocumentRepository,
    resultFile: File,
): Pair<String, String>? {
    val path = resultFile.canonicalPath
    return sessions.scanForGallery().documents.firstNotNullOfOrNull { document ->
        document.results.firstOrNull { record ->
            runCatching { sessions.resolveResultPath(record).canonicalPath == path }
                .getOrDefault(false)
        }?.let { document.sessionId to it.resultId }
    }
}

@Composable
fun ResultScreen(
    resultPath: String,
    onHome: () -> Unit,
    onShare: (() -> Unit)? = null,
    autoSave: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val file = remember(resultPath) { File(resultPath) }

    var isSaving by remember { mutableStateOf(false) }
    var isSaved by remember(resultPath) { mutableStateOf(false) }
    var resultLink by remember(resultPath) { mutableStateOf<Pair<String, String>?>(null) }
    var resumeRevision by remember(resultPath) { mutableIntStateOf(0) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(AppToastType.Success) }

    val scope = rememberCoroutineScope()
    val sessions = remember(context) { SessionDocumentRepository(context) }
    val exporter = remember(context) { GalleryExporter(context) }
    var pendingPermissionRetry by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun showOutcome(outcome: ExportOutcome) {
        when (outcome) {
            is ExportOutcome.Saved, is ExportOutcome.AlreadySaved -> {
                isSaved = true
                toastType = AppToastType.Success
                toastMessage = "갤러리에 저장 완료!"
            }
            is ExportOutcome.NeedsPermission -> {
                toastType = AppToastType.Error
                toastMessage = "사진첩 저장 권한이 필요합니다."
            }
            is ExportOutcome.NeedsRecovery -> {
                toastType = AppToastType.Error
                toastMessage = "저장 상태를 확인할 수 없습니다. 다시 시도해 주세요."
            }
            is ExportOutcome.Failed -> {
                toastType = AppToastType.Error
                toastMessage = outcome.message
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val target = pendingPermissionRetry
        pendingPermissionRetry = null
        if (granted && target != null) scope.launch {
            isSaving = true
            showOutcome(exporter.export(target.first, target.second, ExportMode.SAVE))
            isSaving = false
        } else if (!granted) {
            toastType = AppToastType.Error
            toastMessage = "사진첩 저장 권한이 거부되었습니다."
        }
    }

    DisposableEffect(lifecycleOwner, resultPath) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeRevision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resultPath, resumeRevision) {
        try {
            val match = findResultLink(sessions, file)
            resultLink = match
            isSaved = match?.let { exporter.isNormalCopyVerified(it.first, it.second) } == true
            if (match == null) {
                toastType = AppToastType.Error
                toastMessage = "저장된 결과를 찾을 수 없습니다."
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            resultLink = null
            isSaved = false
            toastType = AppToastType.Error
            toastMessage = error.message ?: "결과 정보를 읽을 수 없습니다."
        }
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
        val target = resultLink ?: return
        scope.launch {
            runCatching { exporter.shareUri(target.first, target.second) }
                .onSuccess { sharedUri ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, sharedUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "공유"))
                }
                .onFailure {
                    toastType = AppToastType.Error
                    toastMessage = it.message ?: "공유할 수 없습니다."
                }
        }
    }

    fun performSave() {
        if (isSaving || isSaved) return
        val target = resultLink ?: return
        scope.launch {
            isSaving = true
            val outcome = exporter.export(target.first, target.second, ExportMode.SAVE)
            isSaving = false
            if (outcome is ExportOutcome.NeedsPermission) {
                pendingPermissionRetry = target
                permissionLauncher.launch(outcome.permission)
            } else showOutcome(outcome)
        }
    }

    LaunchedEffect(resultLink, autoSave) {
        if (!autoSave) return@LaunchedEffect
        if (!AppSettings.autoSaveToGallery) return@LaunchedEffect
        if (!file.isFile) return@LaunchedEffect
        val target = resultLink ?: return@LaunchedEffect
        val outcome = exporter.export(target.first, target.second, ExportMode.AUTO)
        if (outcome is ExportOutcome.NeedsPermission) {
            pendingPermissionRetry = target
            permissionLauncher.launch(outcome.permission)
        } else showOutcome(outcome)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
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
                IconCircleButton(onClick = onHome, variant = IconButtonVariant.SOLID) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "홈",
                        tint = AppColors.Text.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.xl),
            ) {
                Text(
                    text = "PRINT READY",
                    style = AppTypography.caption1.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                    ),
                    color = AppColors.Accent.pink,
                    modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal),
                )
                Spacer(Modifier.height(AppSpacing.xs))
                Text(
                    text = "완성됐어요",
                    style = AppTypography.largeTitle,
                    color = AppColors.Text.primary,
                    modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal),
                )
                Spacer(Modifier.height(AppSpacing.xxs))
                Text(
                    text = "소중한 순간이 담긴 사진이 완성되었어요",
                    style = AppTypography.callout,
                    color = AppColors.Text.secondary,
                    modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal),
                )
            }

            val imageShape = RoundedCornerShape(AppLayout.Radius.xs)
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
                            elevation = 8.dp,
                            shape = imageShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.16f),
                            spotColor = Color.Black.copy(alpha = 0.16f),
                        )
                        .clip(imageShape),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(file).build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        contentScale = ContentScale.Fit,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    when {
                        isSaved -> SavedResultButton(modifier = Modifier.weight(1f))
                        isSaving -> SavingResultButton(modifier = Modifier.weight(1f))
                        else -> PrimaryButton(
                            text = "저장",
                            onClick = { performSave() },
                            fullWidth = false,
                            modifier = Modifier.weight(1f),
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }

                    SecondaryButton(
                        text = "공유",
                        onClick = { performShare() },
                        fullWidth = false,
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                tint = AppColors.Text.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppLayout.Height.Button.md)
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
private fun SavingResultButton(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppLayout.Radius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppLayout.Height.Button.lg)
            .clip(shape)
            .background(AppColors.Accent.pink),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color.White,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(AppSpacing.xs))
        Text(
            text = "저장 중...",
            style = AppTypography.headline,
            color = Color.White,
        )
    }
}

@Composable
private fun SavedResultButton(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppLayout.Radius.md)
    Row(
        modifier = modifier
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
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(AppSpacing.xs))
        Text(
            text = "저장 완료",
            style = AppTypography.headline,
            color = Color.White,
        )
    }
}
