package com.pocket4cut.presentation.frameFlow

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pocket4cut.frame.CollagePreviewScaledToFit
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.frame.PhotoCropTransform
import com.pocket4cut.frame.occasion.OccasionCatalog
import com.pocket4cut.frame.occasion.OccasionCatalogLoader
import com.pocket4cut.frame.occasion.OccasionTheme
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OccasionFramePickRoute(
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onCompleted: (OccasionTheme) -> Unit,
    modifier: Modifier = Modifier,
    initialThemeId: String? = null,
    saveError: String? = null,
    applying: Boolean = false,
    images: List<Bitmap> = emptyList(),
    cropTransforms: List<PhotoCropTransform> = emptyList(),
    frameType: FrameType? = null,
    frameStyle: FrameStyle? = null,
    frameTheme: FrameTheme? = null,
    layoutVersion: Int = 2,
    filterId: FilterId = FilterId.ORIGINAL,
    captionTextPart: String? = null,
    captionDatePart: String? = null,
    captionTextSizePt: Float = 16f,
    captionDateSizePt: Float = 16f,
    captionFontName: String? = null,
    captionColorRGB: Long? = null,
) {
    // Keep consuming the system back gesture while an atomic session update is in flight.
    // Disabling this handler would expose NavHost's default pop and cancel the applying scope.
    BackHandler { if (!applying) onBack() }
    val context = LocalContext.current
    var loadAttempt by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val catalogResult by produceState<Result<OccasionCatalog>?>(
        initialValue = null,
        context,
        loadAttempt,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching { OccasionCatalogLoader.load(context.applicationContext) }
        }
    }
    val result = catalogResult
    if (result?.isSuccess == true) {
        OccasionFramePickScreen(
            catalog = result.getOrThrow(),
            onBack = onBack,
            onDismiss = onDismiss,
            onCompleted = onCompleted,
            modifier = modifier,
            initialThemeId = initialThemeId,
            saveError = saveError,
            applying = applying,
            images = images,
            cropTransforms = cropTransforms,
            frameType = frameType,
            frameStyle = frameStyle,
            frameTheme = frameTheme,
            layoutVersion = layoutVersion,
            filterId = filterId,
            captionTextPart = captionTextPart,
            captionDatePart = captionDatePart,
            captionTextSizePt = captionTextSizePt,
            captionDateSizePt = captionDateSizePt,
            captionFontName = captionFontName,
            captionColorRGB = captionColorRGB,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.Background.primary),
        ) {
            OccasionHeader(onBack = onBack, onDismiss = onDismiss, enabled = !applying)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(AppSpacing.Screen.horizontal),
                contentAlignment = Alignment.Center,
            ) {
                if (result == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AppColors.Accent.pink)
                        Spacer(Modifier.height(AppSpacing.md))
                        Text(
                            text = "88가지 프레임을 불러오는 중입니다.",
                            style = AppTypography.callout,
                            color = AppColors.Text.secondary,
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "프레임 목록을 불러오지 못했습니다.",
                            style = AppTypography.headline,
                            color = AppColors.Text.primary,
                        )
                        Spacer(Modifier.height(AppSpacing.sm))
                        Text(
                            text = "이전 프레임 선택은 그대로 보존됩니다.",
                            style = AppTypography.callout,
                            color = AppColors.Text.secondary,
                        )
                        Spacer(Modifier.height(AppSpacing.lg))
                        PrimaryButton(
                            text = "다시 시도",
                            onClick = { loadAttempt += 1 },
                            fullWidth = false,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Browses the packaged 88-theme catalog without mutating the session. The caller persists only
 * [onCompleted], so Back and category/theme exploration leave the previous frame selection intact.
 */
@Composable
fun OccasionFramePickScreen(
    catalog: OccasionCatalog,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onCompleted: (OccasionTheme) -> Unit,
    modifier: Modifier = Modifier,
    initialThemeId: String? = null,
    saveError: String? = null,
    applying: Boolean = false,
    images: List<Bitmap> = emptyList(),
    cropTransforms: List<PhotoCropTransform> = emptyList(),
    frameType: FrameType? = null,
    frameStyle: FrameStyle? = null,
    frameTheme: FrameTheme? = null,
    layoutVersion: Int = 2,
    filterId: FilterId = FilterId.ORIGINAL,
    captionTextPart: String? = null,
    captionDatePart: String? = null,
    captionTextSizePt: Float = 16f,
    captionDateSizePt: Float = 16f,
    captionFontName: String? = null,
    captionColorRGB: Long? = null,
) {
    val allCategoryId = "__all__"
    var selectedCategoryId by rememberSaveable(catalog.designVersion) { mutableStateOf(allCategoryId) }
    var selectedThemeId by rememberSaveable(catalog.designVersion, initialThemeId) {
        mutableStateOf(initialThemeId?.takeIf { id -> catalog.themes.any { it.id == id } }
            ?: catalog.themes.firstOrNull()?.id.orEmpty())
    }
    val visibleThemes = remember(catalog, selectedCategoryId) {
        if (selectedCategoryId == allCategoryId) catalog.themes
        else catalog.themes.filter { it.categoryId == selectedCategoryId }
    }
    var artworkReadyThemeId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedCategoryId, visibleThemes) {
        if (visibleThemes.none { it.id == selectedThemeId }) {
            artworkReadyThemeId = null
            selectedThemeId = visibleThemes.firstOrNull()?.id.orEmpty()
        }
    }
    val selectedTheme = catalog.themes.firstOrNull { it.id == selectedThemeId }
    val livePreviewRequired = images.isNotEmpty() && frameType != null &&
        frameStyle != null && frameTheme != null

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        val compactLandscape = maxWidth > maxHeight && maxHeight <= 480.dp
        Column(modifier = Modifier.fillMaxSize()) {
        OccasionHeader(
            onBack = onBack,
            onDismiss = onDismiss,
            compact = compactLandscape,
            enabled = !applying,
        )

        if (!compactLandscape) {
            Text(
                text = "10개 주제에서 88가지 프레임을 골라보세요.",
                style = AppTypography.callout,
                color = AppColors.Text.secondary,
                modifier = Modifier.padding(horizontal = AppSpacing.Screen.horizontal),
            )
            Spacer(Modifier.height(AppSpacing.sm))
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .testTag("occasion-category-row"),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = AppSpacing.Screen.horizontal,
            ),
        ) {
            item(key = allCategoryId) {
                OccasionCategoryChip(
                    id = allCategoryId,
                    label = "전체 88",
                    selected = selectedCategoryId == allCategoryId,
                    onClick = { selectedCategoryId = allCategoryId },
                )
            }
            items(catalog.categories, key = { it.id }) { category ->
                OccasionCategoryChip(
                    id = category.id,
                    label = category.displayName,
                    selected = selectedCategoryId == category.id,
                    onClick = { selectedCategoryId = category.id },
                )
            }
        }

        Spacer(Modifier.height(if (compactLandscape) AppSpacing.xxs else AppSpacing.sm))
        selectedTheme?.let { theme ->
            OccasionCurrentPreview(
                occasionTheme = theme,
                images = images,
                cropTransforms = cropTransforms,
                frameType = frameType,
                frameStyle = frameStyle,
                frameTheme = frameTheme,
                layoutVersion = layoutVersion,
                compact = compactLandscape,
                filterId = filterId,
                captionTextPart = captionTextPart,
                captionDatePart = captionDatePart,
                captionTextSizePt = captionTextSizePt,
                captionDateSizePt = captionDateSizePt,
                captionFontName = captionFontName,
                captionColorRGB = captionColorRGB,
                onArtworkReadyChanged = { themeId, ready ->
                    if (themeId == selectedThemeId) {
                        artworkReadyThemeId = themeId.takeIf { ready }
                    }
                },
            )
        }
        saveError?.let { message ->
            Text(
                text = message,
                style = AppTypography.caption1,
                color = AppColors.Semantic.error,
                modifier = Modifier.padding(
                    start = AppSpacing.Screen.horizontal,
                    end = AppSpacing.Screen.horizontal,
                    top = AppSpacing.xs,
                ),
            )
        }
        Spacer(Modifier.height(if (compactLandscape) AppSpacing.xxs else AppSpacing.sm))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 136.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .selectableGroup()
                .testTag("occasion-theme-grid")
                .semantics { stateDescription = "${visibleThemes.size}개 프레임" },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = AppSpacing.Screen.horizontal,
                end = AppSpacing.Screen.horizontal,
                bottom = if (compactLandscape) AppSpacing.xs else AppSpacing.lg,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            itemsIndexed(visibleThemes, key = { _, theme -> theme.id }) { index, theme ->
                OccasionThemeCard(
                    theme = theme,
                    selected = theme.id == selectedThemeId,
                    position = index + 1,
                    total = visibleThemes.size,
                    onClick = {
                        // Re-tapping the active radio item must not invalidate a preview that is
                        // already ready. The preview keys do not change in that case, so there is
                        // no subsequent artwork callback that could restore the ready state.
                        if (theme.id != selectedThemeId) {
                            artworkReadyThemeId = null
                            selectedThemeId = theme.id
                        }
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.Screen.horizontal),
        ) {
            PrimaryButton(
                text = if (applying) "프레임 적용 중" else
                    selectedTheme?.let { "${it.displayName} 프레임 적용" } ?: "프레임 적용",
                onClick = { selectedTheme?.let(onCompleted) },
                enabled = selectedTheme != null && !applying &&
                    (!livePreviewRequired || artworkReadyThemeId == selectedTheme.id),
                fullWidth = true,
                modifier = Modifier.testTag("occasion-apply"),
            )
            Spacer(
                Modifier.height(
                    if (compactLandscape) AppSpacing.xxs else AppSpacing.Layout.ctaBottomSpace,
                ),
            )
        }
        }
    }
}

@Composable
private fun OccasionHeader(
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    compact: Boolean = false,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (compact) AppSpacing.xs else AppSpacing.xxxl,
                start = AppSpacing.Screen.horizontal,
                end = AppSpacing.Screen.horizontal,
                bottom = if (compact) AppSpacing.xxs else AppSpacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(AppLayout.Height.IconButton.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            IconCircleButton(
                onClick = onBack,
                accessibilityLabel = "프레임 방식으로 돌아가기",
                enabled = enabled,
                variant = IconButtonVariant.SOLID,
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = AppColors.Text.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = "OCCASION 88",
            style = AppTypography.title2,
            color = AppColors.Text.primary,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(
            modifier = Modifier.width(AppLayout.Height.IconButton.md),
            contentAlignment = Alignment.CenterEnd,
        ) {
            IconCircleButton(
                onClick = onDismiss,
                accessibilityLabel = "프레임 선택 닫기",
                enabled = enabled,
                variant = IconButtonVariant.SOLID,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = AppColors.Text.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun OccasionCategoryChip(
    id: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (selected) AppColors.Accent.pink else AppColors.Background.card)
            .border(1.dp, if (selected) AppColors.Accent.pink else AppColors.Border.medium, shape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag("occasion-category-$id")
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else AppColors.Text.primary,
            maxLines = 1,
        )
    }
}

@Composable
private fun OccasionCurrentPreview(
    occasionTheme: OccasionTheme,
    images: List<Bitmap>,
    cropTransforms: List<PhotoCropTransform>,
    frameType: FrameType?,
    frameStyle: FrameStyle?,
    frameTheme: FrameTheme?,
    layoutVersion: Int,
    compact: Boolean,
    filterId: FilterId,
    captionTextPart: String?,
    captionDatePart: String?,
    captionTextSizePt: Float,
    captionDateSizePt: Float,
    captionFontName: String?,
    captionColorRGB: Long?,
    onArtworkReadyChanged: (themeId: String, ready: Boolean) -> Unit,
) {
    val canRenderCollage = images.isNotEmpty() && frameType != null &&
        frameStyle != null && frameTheme != null
    if (canRenderCollage) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = if (compact) 72.dp else 132.dp,
                    max = if (compact) 88.dp else 260.dp,
                )
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .testTag("occasion-live-preview")
                .semantics {
                    contentDescription = "${occasionTheme.displayName} 실제 프레임 미리보기"
                },
            contentAlignment = Alignment.Center,
        ) {
            CollagePreviewScaledToFit(
                images = images,
                cropTransforms = cropTransforms,
                frameType = requireNotNull(frameType),
                frameStyle = requireNotNull(frameStyle),
                theme = requireNotNull(frameTheme),
                occasionTheme = occasionTheme,
                layoutVersion = layoutVersion,
                filterId = filterId,
                captionTextPart = captionTextPart,
                captionDatePart = captionDatePart,
                captionTextSizePt = captionTextSizePt,
                captionDateSizePt = captionDateSizePt,
                captionFontName = captionFontName,
                captionColorRGB = captionColorRGB,
                onOccasionArtworkReadyChanged = onArtworkReadyChanged,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!compact) Spacer(Modifier.height(AppSpacing.xs))
    }
    if (compact) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.Screen.horizontal)
            .background(AppColors.Background.card, RoundedCornerShape(AppLayout.Radius.sm))
            .border(1.dp, AppColors.Border.light, RoundedCornerShape(AppLayout.Radius.sm))
            .padding(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!canRenderCollage) {
            OccasionThumbnail(
                theme = occasionTheme,
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(3f / 2f),
            )
            Spacer(Modifier.width(AppSpacing.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = occasionTheme.displayName,
                style = AppTypography.headline,
                color = AppColors.Text.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (occasionTheme.isManualRecord) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "직접 기록 · 기록 내용은 편집할 때 직접 입력해요.",
                    style = AppTypography.caption1,
                    color = AppColors.Text.secondary,
                )
            }
        }
    }
}

@Composable
private fun OccasionThemeCard(
    theme: OccasionTheme,
    selected: Boolean,
    position: Int,
    total: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppLayout.Radius.sm)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Background.card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AppColors.Accent.pink else AppColors.Border.light,
                shape = shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag("occasion-theme-${theme.id}")
            .semantics {
                contentDescription = buildString {
                    append(theme.displayName)
                    if (theme.isManualRecord) append(", 직접 기록")
                    append(", ${position}번째, 전체 ${total}개")
                }
            }
            .padding(AppSpacing.xs),
    ) {
        OccasionThumbnail(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f),
        )
        Spacer(Modifier.height(AppSpacing.xs))
        Text(
            text = theme.displayName,
            style = AppTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.Text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 40.dp),
        )
        if (theme.isManualRecord) {
            Text(
                text = "직접 기록",
                style = AppTypography.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Accent.pink,
            )
        }
    }
}

@Composable
private fun OccasionThumbnail(theme: OccasionTheme, modifier: Modifier = Modifier) {
    AsyncImage(
        model = "file:///android_asset/${theme.thumbnailAssetPath}",
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(AppColors.Background.tertiary),
    )
}
