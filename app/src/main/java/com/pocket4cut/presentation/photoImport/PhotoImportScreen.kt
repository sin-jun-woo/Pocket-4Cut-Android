package com.pocket4cut.presentation.photoImport

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import com.pocket4cut.ui.designsystem.components.SecondaryButton
import java.io.File
import java.util.UUID

@Composable
fun PhotoImportScreen(
    frameType: FrameType,
    resumeSessionId: String?,
    onBack: () -> Unit,
    onDone: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val stableSessionId = rememberSaveable(resumeSessionId) {
        resumeSessionId ?: UUID.randomUUID().toString()
    }
    val viewModel: PhotoImportViewModel = viewModel(
        key = "photoImport_$stableSessionId",
        factory = PhotoImportViewModel.Factory(application, frameType, stableSessionId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var launchedAutomatically by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(frameType.selectCount),
        onResult = viewModel::importUris,
    )
    val launchPicker = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(uiState.isInitialized) {
        if (uiState.isInitialized && !launchedAutomatically && uiState.photos.isEmpty()) {
            launchedAutomatically = true
            launchPicker()
        }
    }

    PhotoImportContent(
        frameType = frameType,
        uiState = uiState,
        onBack = onBack,
        onOpenAlbum = launchPicker,
        onMove = viewModel::movePhoto,
        onRemove = viewModel::removePhoto,
        onDismissMessage = viewModel::clearMessage,
        onDone = { onDone(viewModel.sessionId) },
        modifier = modifier,
    )
}

@Composable
internal fun PhotoImportContent(
    frameType: FrameType,
    uiState: PhotoImportUiState,
    onBack: () -> Unit,
    onOpenAlbum: () -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: (photoId: String) -> Unit,
    onDismissMessage: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
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
            Column(modifier = Modifier.weight(1f)) {
                Text("앨범 사진 확인", style = AppTypography.title1, color = AppColors.Text.primary)
                Spacer(Modifier.height(AppSpacing.xxs))
                Text(
                    "${frameType.selectCount}장 중 ${uiState.photos.size}장 · 길게 끌어 순서를 바꿀 수 있어요.",
                    style = AppTypography.callout,
                    color = AppColors.Text.secondary,
                )
            }
            IconCircleButton(
                onClick = onBack,
                accessibilityLabel = "앨범 사진 확인 닫기",
                variant = IconButtonVariant.SOLID,
            ) {
                Icon(Icons.Default.Close, contentDescription = null,
                    tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
            }
        }

        uiState.message?.let { message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal, vertical = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message,
                    color = if (uiState.isMessageError) AppColors.Semantic.error else AppColors.Semantic.info,
                    style = AppTypography.footnote,
                    modifier = Modifier.weight(1f),
                )
                if (!uiState.hasBlockingRecoveryError) {
                    IconCircleButton(
                        onClick = onDismissMessage,
                        accessibilityLabel = "안내 닫기",
                        diameter = 48.dp,
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null,
                            tint = AppColors.Text.secondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (!uiState.isInitialized || (uiState.isBusy && uiState.photos.isEmpty())) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Accent.pink)
            }
        } else if (uiState.photos.isEmpty()) {
            EmptyImportState(
                requiredCount = frameType.selectCount,
                onOpenAlbum = onOpenAlbum,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = AppSpacing.Screen.horizontal,
                    end = AppSpacing.Screen.horizontal,
                    top = AppSpacing.xs,
                    bottom = AppSpacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                itemsIndexed(uiState.photos, key = { _, item -> item.photoId }) { index, item ->
                    ImportedPhotoRow(
                        item = item,
                        index = index,
                        count = uiState.photos.size,
                        enabled = !uiState.isBusy,
                        onMove = onMove,
                        onRemove = { onRemove(item.photoId) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Background.primary)
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(top = AppSpacing.sm, bottom = AppSpacing.Layout.ctaBottomSpace),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            if (uiState.photos.isNotEmpty() && uiState.photos.size < frameType.selectCount) {
                SecondaryButton(
                    text = "사진 추가 · ${frameType.selectCount - uiState.photos.size}장 필요",
                    onClick = onOpenAlbum,
                    enabled = !uiState.isBusy,
                    icon = {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null,
                            tint = AppColors.Text.primary, modifier = Modifier.size(19.dp))
                    },
                )
            }
            PrimaryButton(
                text = "레이아웃 선택",
                onClick = onDone,
                enabled = uiState.canContinue(frameType.selectCount),
            )
        }
    }
}

@Composable
private fun EmptyImportState(
    requiredCount: Int,
    onOpenAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.Screen.horizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = AppColors.Text.tertiary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(AppSpacing.md))
        Text(
            text = "앨범에서 ${requiredCount}장을 선택해 주세요",
            style = AppTypography.title2,
            color = AppColors.Text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.xs))
        Text(
            text = "선택을 취소해도 이 화면에서 다시 열 수 있어요.",
            style = AppTypography.body,
            color = AppColors.Text.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.xl))
        SecondaryButton(
            text = "앨범 열기",
            onClick = onOpenAlbum,
            icon = {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null,
                    tint = AppColors.Text.primary, modifier = Modifier.size(19.dp))
            },
        )
    }
}

@Composable
private fun ImportedPhotoRow(
    item: ImportedPhotoItem,
    index: Int,
    count: Int,
    enabled: Boolean,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemove: () -> Unit,
) {
    val density = LocalDensity.current
    var dragDistance by remember(item.photoId) { mutableFloatStateOf(0f) }
    val moveEarlier = { if (index > 0) onMove(index, index - 1); index > 0 }
    val moveLater = { if (index < count - 1) onMove(index, index + 1); index < count - 1 }
    val actions = buildList {
        if (index > 0) add(CustomAccessibilityAction("앞으로 이동", moveEarlier))
        if (index < count - 1) add(CustomAccessibilityAction("뒤로 이동", moveLater))
        add(CustomAccessibilityAction("사진 제거") { onRemove(); true })
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppLayout.Radius.sm))
            .background(AppColors.Background.card)
            .semantics {
                contentDescription = "${index + 1}번째 가져온 사진"
                stateDescription = "${index + 1}번째 사진, 전체 ${count}장"
                customActions = actions
            }
            .pointerInput(item.photoId, index, count, enabled) {
                if (!enabled) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragDistance = 0f },
                    onDragCancel = { dragDistance = 0f },
                    onDragEnd = { dragDistance = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        dragDistance += amount.y
                        val threshold = with(density) { 56.dp.toPx() }
                        when {
                            dragDistance <= -threshold && index > 0 -> {
                                onMove(index, index - 1)
                                dragDistance = 0f
                            }
                            dragDistance >= threshold && index < count - 1 -> {
                                onMove(index, index + 1)
                                dragDistance = 0f
                            }
                        }
                    },
                )
            }
            .padding(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(AppLayout.Radius.xs))
                .background(AppColors.Background.secondary),
        ) {
            AsyncImage(
                model = File(item.path),
                contentDescription = "${index + 1}번째로 사용할 사진",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(AppColors.Accent.pink)
                    .padding(horizontal = AppSpacing.xs, vertical = 2.dp),
            ) {
                Text("${index + 1}", style = AppTypography.caption1.copy(fontWeight = FontWeight.Bold),
                    color = Color.White)
            }
        }
        Spacer(Modifier.width(AppSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text("사진 ${index + 1}", style = AppTypography.headline, color = AppColors.Text.primary)
            Text("길게 눌러 위아래로 이동", style = AppTypography.caption1,
                color = AppColors.Text.secondary)
        }
        Icon(Icons.Default.DragHandle, contentDescription = null,
            tint = AppColors.Text.tertiary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(AppSpacing.xs))
        IconCircleButton(
            onClick = onRemove,
            accessibilityLabel = "${index + 1}번째 사진 제거",
            enabled = enabled,
            diameter = 48.dp,
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null,
                tint = AppColors.Semantic.error, modifier = Modifier.size(20.dp))
        }
    }
}
