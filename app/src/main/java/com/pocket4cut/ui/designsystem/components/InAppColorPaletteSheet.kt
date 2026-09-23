package com.pocket4cut.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography

private val ColorCircleSize = 44.dp
private val ColorTouchTargetSize = 48.dp

private data class PaletteColor(val rgb: Long, val label: String)

private val PresetColors: List<PaletteColor> = listOf(
    PaletteColor(0x000000L, "검정"),
    PaletteColor(0xFFFFFFL, "흰색"),
    PaletteColor(0xFF6B9DL, "핑크"),
    PaletteColor(0x4FC3F7L, "하늘색"),
    PaletteColor(0xFFD93DL, "노랑"),
    PaletteColor(0x6BCF9FL, "민트"),
    PaletteColor(0x9B8CFFL, "보라"),
    PaletteColor(0xFF7E67L, "코랄"),
    PaletteColor(0xE8956BL, "테라코타"),
    PaletteColor(0x89CFF0L, "파스텔 블루"),
    PaletteColor(0xD97D54L, "브라운"),
    PaletteColor(0xA8D8EAL, "라이트 블루"),
)

private fun rgbToComposeColor(rgb: Long): Color {
    val v = (rgb and 0xFFFFFFL).toInt()
    return Color(0xFF000000 or v.toLong())
}

private fun rgbMatches(selected: Long?, preset: Long): Boolean {
    if (selected == null) return false
    return (selected and 0xFFFFFFL) == (preset and 0xFFFFFFL)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppColorPaletteSheet(
    selectedColorRGB: Long?,
    onColorSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Background.card,
        contentColor = AppColors.Text.primary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(bottom = AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Text(
                text = "색상",
                style = AppTypography.title3,
                color = AppColors.Text.primary,
            )
            ColorPaletteCell(
                rgb = null,
                isAuto = true,
                accessibilityLabel = "자동 색상",
                selected = selectedColorRGB == null,
                onClick = {
                    onColorSelected(null)
                    onDismiss()
                },
            )
            PresetColors.chunked(6).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowColors.forEach { color ->
                        val selected = rgbMatches(selectedColorRGB, color.rgb)
                        ColorPaletteCell(
                            rgb = color.rgb,
                            isAuto = false,
                            accessibilityLabel = "${color.label} 색상",
                            selected = selected,
                            onClick = {
                                onColorSelected(color.rgb)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ColorPaletteCell(
    rgb: Long?,
    isAuto: Boolean,
    accessibilityLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(accessibilityLabel.isNotBlank()) { "accessibilityLabel must not be blank" }
    Box(
        modifier = modifier
            .size(ColorTouchTargetSize)
            .clip(CircleShape)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ColorCircleSize)
                .clip(CircleShape)
                .then(
                    if (isAuto) {
                        Modifier
                            .background(AppColors.Background.secondary)
                            .border(AppLayout.BorderWidth.thin, AppColors.Border.medium, CircleShape)
                    } else {
                        val c = rgbToComposeColor(rgb!!)
                        Modifier
                            .background(c)
                            .then(
                                if ((rgb and 0xFFFFFFL) == 0xFFFFFFL) {
                                    Modifier.border(AppLayout.BorderWidth.thin, AppColors.Border.medium, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isAuto && !selected) {
                Text(
                    text = "자동",
                    style = AppTypography.caption2,
                    color = AppColors.Text.secondary,
                )
            }
            if (selected) {
                val checkTint = if (!isAuto && rgb != null && (rgb and 0xFFFFFFL) == 0xFFFFFFL) {
                    AppColors.Text.primary
                } else {
                    AppColors.Text.inverse
                }
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isAuto) AppColors.Accent.pink else checkTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
