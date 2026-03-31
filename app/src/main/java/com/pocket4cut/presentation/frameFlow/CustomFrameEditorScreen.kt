package com.pocket4cut.presentation.frameFlow

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pocket4cut.core.util.AppFontCatalog
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.CollagePreviewScaledToFit
import com.pocket4cut.frame.CustomFrameDecoration
import com.pocket4cut.frame.CustomFrameDesign
import com.pocket4cut.frame.EmojiPicklist
import com.pocket4cut.frame.FrameColor
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.frame.NormPoint
import com.pocket4cut.frame.StickerPalette
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import kotlin.math.hypot
import kotlin.math.min

private enum class TrayMode {
    Hidden,
    Emoji,
    Sticker,
}

private val stickerTintPalette = listOf(
    0xFFE91E63L,
    0xFFFF5722L,
    0xFFFFC107L,
    0xFF4CAF50L,
    0xFF2196F3L,
    0xFF9C27B0L,
    0xFF000000L,
    0xFFFFFFFFL,
)

@Composable
fun CustomFrameEditorScreen(
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onCompleted: (CustomFrameDesign) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var fillColorId by remember { mutableStateOf(FrameColors.all.first().id) }
    var decorations by remember { mutableStateOf(listOf<CustomFrameDecoration>()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var trayMode by remember { mutableStateOf(TrayMode.Hidden) }
    var selectedStickerColor by remember { mutableStateOf(stickerTintPalette.first()) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textDraft by remember { mutableStateOf("텍스트") }
    var draftTextColorHex by remember { mutableStateOf(0x000000L) }
    var draftTextFontName by remember { mutableStateOf<String?>(null) }
    var showTextFontSheet by remember { mutableStateOf(false) }
    var showTextColorSheet by remember { mutableStateOf(false) }

    val design = remember(fillColorId, decorations) {
        CustomFrameDesign(fillColorId = fillColorId, decorations = decorations)
    }

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
            Box(
                modifier = Modifier.width(AppLayout.Height.IconButton.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = "커스텀 프레임",
                style = AppTypography.title2,
                color = AppColors.Text.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.width(AppLayout.Height.IconButton.md),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconCircleButton(onClick = onDismiss, variant = IconButtonVariant.SOLID) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = AppColors.Border.subtle)

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.Screen.horizontal),
        ) {
            val density = LocalDensity.current
            val maxWPx = with(density) { maxWidth.toPx() }
            val maxHPx = with(density) { maxHeight.toPx() }
            val dim = remember(frameStyle, theme, maxWPx) {
                CollageLayoutMath.computeForPreview(frameStyle, theme, null, maxWPx.coerceAtLeast(1f))
            }
            val pw = dim.canvasWidth
            val ph = dim.canvasHeight
            val scale = min(min(maxWPx / pw, maxHPx / ph), 1f).coerceAtLeast(0.0001f)
            val sw = pw * scale
            val sh = ph * scale
            val ox = (maxWPx - sw) / 2f
            val oy = (maxHPx - sh) / 2f
            val hitSlopPx = with(density) { 56.dp.toPx() }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CollagePreviewScaledToFit(
                    images = images,
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    customFrameDesign = design,
                    modifier = Modifier.fillMaxSize(),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(sw, sh, ox, oy, decorations, hitSlopPx) {
                            detectTapGestures { offset ->
                                val nx = (offset.x - ox) / sw
                                val ny = (offset.y - oy) / sh
                                if (nx !in 0f..1f || ny !in 0f..1f) {
                                    selectedId = null
                                    return@detectTapGestures
                                }
                                var hit: String? = null
                                for (d in decorations.asReversed()) {
                                    val cx = ox + d.position.x * sw
                                    val cy = oy + d.position.y * sh
                                    if (hypot(offset.x - cx, offset.y - cy) < hitSlopPx) {
                                        hit = d.id
                                        break
                                    }
                                }
                                selectedId = hit
                            }
                        },
                )

                decorations.forEach { dec ->
                    val handle = 52.dp
                    Box(
                        modifier = Modifier
                            .zIndex(if (dec.id == selectedId) 2f else 1f)
                            .offset(
                                x = with(density) { (ox + dec.position.x * sw).toDp() } - handle,
                                y = with(density) { (oy + dec.position.y * sh).toDp() } - handle,
                            )
                            .size(handle * 2)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin.Center
                                rotationZ = Math.toDegrees(dec.rotationRadians.toDouble()).toFloat()
                                scaleX = dec.scale
                                scaleY = dec.scale
                            }
                            .then(
                                if (dec.id == selectedId) {
                                    Modifier
                                        .pointerInput(selectedId, sw, sh) {
                                            detectDragGestures { _, dragAmount ->
                                                val sid = selectedId ?: return@detectDragGestures
                                                decorations = decorations.map {
                                                    if (it.id != sid) {
                                                        it
                                                    } else {
                                                        it.copy(
                                                            position = NormPoint(
                                                                x = (it.position.x + dragAmount.x / sw)
                                                                    .coerceIn(0.02f, 0.98f),
                                                                y = (it.position.y + dragAmount.y / sh)
                                                                    .coerceIn(0.02f, 0.98f),
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        .pointerInput(selectedId, sw, sh) {
                                            detectTransformGestures { _, _, zoomChange, rotationChange ->
                                                val sid = selectedId ?: return@detectTransformGestures
                                                decorations = decorations.map {
                                                    if (it.id != sid) {
                                                        it
                                                    } else {
                                                        it.copy(
                                                            scale = (it.scale * zoomChange).coerceIn(0.25f, 5f),
                                                            rotationRadians = it.rotationRadians + rotationChange,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        DecorationPreviewContent(decoration = dec, context = context)
                        if (dec.id == selectedId) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .border(1.dp, AppColors.Accent.pink, RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .heightIn(max = 320.dp),
        ) {
            Text(
                text = "배경",
                style = AppTypography.caption1,
                color = AppColors.Text.secondary,
                modifier = Modifier.padding(bottom = AppSpacing.xs),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                FrameColors.all.forEach { fc ->
                    BackgroundColorChip(
                        frameColor = fc,
                        selected = fc.id == fillColorId,
                        onClick = { fillColorId = fc.id },
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                ToolLabel("텍스트 추가") {
                    textDraft = "텍스트"
                    showTextDialog = true
                    trayMode = TrayMode.Hidden
                }
                ToolLabel("이모지 추가") {
                    trayMode = if (trayMode == TrayMode.Emoji) TrayMode.Hidden else TrayMode.Emoji
                }
                ToolLabel("스티커 추가") {
                    trayMode = if (trayMode == TrayMode.Sticker) TrayMode.Hidden else TrayMode.Sticker
                }
                if (selectedId != null) {
                    ToolLabel("삭제", emphasize = true) {
                        decorations = decorations.filter { it.id != selectedId }
                        selectedId = null
                    }
                }
            }

            when (trayMode) {
                TrayMode.Hidden -> Unit
                TrayMode.Emoji -> {
                    Spacer(Modifier.height(AppSpacing.sm))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 44.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        contentPadding = PaddingValues(vertical = AppSpacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(
                            count = EmojiPicklist.all.size,
                            key = { index -> "emoji_$index" },
                        ) { index ->
                            val emoji = EmojiPicklist.all[index]
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        decorations = decorations + CustomFrameDecoration(
                                            kind = CustomFrameDecoration.Kind.Emoji(emoji),
                                        )
                                        selectedId = decorations.last().id
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
                TrayMode.Sticker -> {
                    Spacer(Modifier.height(AppSpacing.sm))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        stickerTintPalette.forEach { rgb ->
                            val selected = rgb == selectedStickerColor
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF000000L or rgb))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) AppColors.Accent.pink else AppColors.Border.medium,
                                        shape = CircleShape,
                                    )
                                    .clickable { selectedStickerColor = rgb },
                            )
                        }
                    }
                    Spacer(Modifier.height(AppSpacing.sm))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(StickerPalette.entries.toList()) { sticker ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.Background.card)
                                    .clickable {
                                        decorations = decorations + CustomFrameDecoration(
                                            kind = CustomFrameDecoration.Kind.Sticker(
                                                assetId = sticker.assetId,
                                                colorRGB = selectedStickerColor and 0xFFFFFFL,
                                            ),
                                        )
                                        selectedId = decorations.last().id
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = sticker.icon,
                                    contentDescription = sticker.displayName,
                                    tint = Color(0xFF000000L or (selectedStickerColor and 0xFFFFFFL)),
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (decorations.isNotEmpty()) {
                Spacer(Modifier.height(AppSpacing.md))
                Text(
                    text = "추가한 꾸미기",
                    style = AppTypography.caption1,
                    color = AppColors.Text.secondary,
                    modifier = Modifier.padding(bottom = AppSpacing.xs),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    decorations.forEach { dec ->
                        val sel = dec.id == selectedId
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (sel) 2.dp else 1.dp,
                                    color = if (sel) AppColors.Accent.pink else AppColors.Border.light,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .background(AppColors.Background.card)
                                .clickable {
                                    selectedId = dec.id
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            DecorationChipMini(decoration = dec, context = context)
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))
            PrimaryButton(
                text = "이 프레임으로 계속",
                onClick = { onCompleted(design) },
                fullWidth = true,
            )
            Spacer(Modifier.height(AppSpacing.Layout.ctaBottomSpace))
        }
    }

    if (showTextDialog) {
        val fontOptions = remember { AppFontCatalog.options(context) }
        val currentFontDisplay = fontOptions.firstOrNull { it.id == draftTextFontName }?.displayName ?: "기본"
        val textColor = Color(0xFF000000L or (draftTextColorHex and 0xFFFFFFL))

        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showTextDialog = false },
            containerColor = AppColors.Background.primary,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = AppSpacing.Layout.ctaBottomSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("텍스트 추가", style = AppTypography.title3, color = AppColors.Text.primary)
                Spacer(Modifier.height(AppSpacing.md))

                Text(
                    text = textDraft.ifBlank { "텍스트" },
                    color = textColor,
                    fontSize = 24.sp,
                    fontFamily = draftTextFontName?.let { name ->
                        fontOptions.firstOrNull { it.id == name }?.fontFamily
                    } ?: androidx.compose.ui.text.font.FontFamily.Default,
                    modifier = Modifier.padding(vertical = AppSpacing.md),
                )

                androidx.compose.material3.OutlinedTextField(
                    value = textDraft,
                    onValueChange = { textDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("텍스트 입력") },
                )
                Spacer(Modifier.height(AppSpacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(AppLayout.Radius.sm))
                            .background(AppColors.Background.tertiary)
                            .clickable { showTextFontSheet = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("글꼴: $currentFontDisplay", style = AppTypography.callout, color = AppColors.Text.primary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(AppLayout.Radius.sm))
                            .background(AppColors.Background.tertiary)
                            .clickable { showTextColorSheet = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(textColor)
                                    .border(1.dp, AppColors.Border.medium, CircleShape),
                            )
                            Text("색상", style = AppTypography.callout, color = AppColors.Text.primary)
                        }
                    }
                }
                Spacer(Modifier.height(AppSpacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        com.pocket4cut.ui.designsystem.components.SecondaryButton(
                            text = "취소",
                            onClick = { showTextDialog = false },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PrimaryButton(
                            text = "추가",
                            onClick = {
                                val newDec = CustomFrameDecoration(
                                    kind = CustomFrameDecoration.Kind.Text(
                                        content = textDraft.ifBlank { "텍스트" },
                                        textColorARGB = 0xFF000000L or (draftTextColorHex and 0xFFFFFFL),
                                        fontScale = 0.08f,
                                    ),
                                    fontName = draftTextFontName,
                                )
                                decorations = decorations + newDec
                                selectedId = newDec.id
                                showTextDialog = false
                                trayMode = TrayMode.Hidden
                            },
                        )
                    }
                }
            }
        }
    }

    if (showTextFontSheet) {
        com.pocket4cut.ui.designsystem.components.InAppFontPickerSheet(
            selectedFontName = draftTextFontName,
            onFontSelected = { draftTextFontName = it; showTextFontSheet = false },
            onDismiss = { showTextFontSheet = false },
        )
    }

    if (showTextColorSheet) {
        com.pocket4cut.ui.designsystem.components.InAppColorPaletteSheet(
            selectedColorRGB = draftTextColorHex,
            onColorSelected = { rgb -> draftTextColorHex = rgb ?: 0x000000L; showTextColorSheet = false },
            onDismiss = { showTextColorSheet = false },
        )
    }
}

@Composable
private fun BackgroundColorChip(
    frameColor: FrameColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AppColors.Accent.pink else AppColors.Border.medium,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    ) {
        val brush = frameColor.gradientBrush
        if (brush != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(frameColor.color),
            )
        }
    }
}

@Composable
private fun ToolLabel(text: String, emphasize: Boolean = false, onClick: () -> Unit) {
    Text(
        text = text,
        style = if (emphasize) {
            AppTypography.subheadline.copy(color = AppColors.Semantic.error)
        } else {
            AppTypography.subheadline.copy(color = AppColors.Accent.pink)
        },
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = AppSpacing.xs, horizontal = AppSpacing.sm),
    )
}

@Composable
private fun DecorationPreviewContent(
    decoration: CustomFrameDecoration,
    context: android.content.Context,
) {
    when (val k = decoration.kind) {
        is CustomFrameDecoration.Kind.Text -> {
            Text(
                text = k.content,
                style = AppTypography.title3.copy(
                    fontFamily = AppFontCatalog.fontFamily(context, decoration.fontName),
                    color = Color(0xFF000000L or (k.textColorARGB and 0xFFFFFFL)),
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        is CustomFrameDecoration.Kind.Emoji -> {
            Text(text = k.content, fontSize = 28.sp)
        }
        is CustomFrameDecoration.Kind.Sticker -> {
            val p = StickerPalette.fromAssetId(k.assetId)
            if (p != null) {
                Icon(
                    imageVector = p.icon,
                    contentDescription = p.displayName,
                    tint = Color(0xFF000000L or (k.colorRGB and 0xFFFFFFL)),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun DecorationChipMini(
    decoration: CustomFrameDecoration,
    context: android.content.Context,
) {
    when (val k = decoration.kind) {
        is CustomFrameDecoration.Kind.Text -> {
            Text(
                text = k.content.take(2),
                style = AppTypography.caption1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        is CustomFrameDecoration.Kind.Emoji -> Text(text = k.content, fontSize = 18.sp)
        is CustomFrameDecoration.Kind.Sticker -> {
            val p = StickerPalette.fromAssetId(k.assetId)
            if (p != null) {
                Icon(
                    imageVector = p.icon,
                    contentDescription = null,
                    tint = Color(0xFF000000L or (k.colorRGB and 0xFFFFFFL)),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
