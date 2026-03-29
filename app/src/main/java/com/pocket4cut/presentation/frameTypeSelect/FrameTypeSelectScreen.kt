package com.pocket4cut.presentation.frameTypeSelect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton

private data class TypeOption(
    val type: FrameType,
    val name: String,
    val description: String,
    val shots: Int,
    val layoutCount: Int,
)

@Composable
fun FrameTypeSelectScreen(
    onBack: () -> Unit,
    onSelected: (FrameType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember {
        listOf(
            TypeOption(FrameType.TWO_CUT, "미니 2컷", "4장 촬영 후 2장 선택", 2, FrameLayouts.bySlots(2).size),
            TypeOption(FrameType.FOUR_CUT, "클래식 4컷", "8장 촬영 후 4장 선택", 4, FrameLayouts.bySlots(4).size),
            TypeOption(FrameType.SIX_CUT, "콜라주 6컷", "10장 촬영 후 6장 선택", 6, FrameLayouts.bySlots(6).size),
        )
    }
    var selectedType by remember { mutableStateOf(FrameType.FOUR_CUT) }
    val currentOption = options.first { it.type == selectedType }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.xxxl, start = AppSpacing.Screen.horizontal, end = AppSpacing.Screen.horizontal, bottom = AppSpacing.xl),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text("사진 구성 선택", style = AppTypography.title1, color = AppColors.Text.primary)
                    Spacer(Modifier.height(AppSpacing.xxs))
                    Text("원하는 컷 수를 선택해주세요", style = AppTypography.callout, color = AppColors.Text.secondary)
                }
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(Icons.Default.Close, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                }
            }

            // Options
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                options.forEach { option ->
                    val isSelected = selectedType == option.type
                    val bg = if (isSelected) AppColors.Accent.pinkSubtle else AppColors.Background.tertiary
                    val borderColor = if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle
                    val borderWidth = if (isSelected) 2.dp else 1.dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppLayout.Radius.lg))
                            .background(bg)
                            .border(borderWidth, borderColor, RoundedCornerShape(AppLayout.Radius.lg))
                            .clickable { selectedType = option.type }
                            .padding(AppSpacing.lg),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Number badge
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(AppLayout.Radius.md))
                                    .background(if (isSelected) AppColors.Accent.pink else AppColors.Overlay.white),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = option.shots.toString(),
                                    style = AppTypography.title1,
                                    color = if (isSelected) AppColors.Text.primary else AppColors.Accent.pink,
                                )
                            }
                            Spacer(Modifier.width(AppSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(option.name, style = AppTypography.headline, color = AppColors.Text.primary)
                                Spacer(Modifier.height(AppSpacing.xxs))
                                Text(option.description, style = AppTypography.subheadline, color = AppColors.Text.secondary)
                                Spacer(Modifier.height(AppSpacing.sm))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(AppLayout.Radius.md))
                                        .background(AppColors.Background.secondary)
                                        .padding(AppSpacing.sm),
                                ) {
                                    Text(
                                        "촬영 후 ${option.layoutCount}가지 레이아웃 중 선택",
                                        style = AppTypography.caption1,
                                        color = AppColors.Text.tertiary,
                                    )
                                }
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.Accent.pink),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Check, null, tint = AppColors.Text.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom CTA
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary)
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(bottom = AppSpacing.Layout.ctaBottomSpace, top = AppSpacing.md),
        ) {
            PrimaryButton(
                text = "${currentOption.name} 촬영 시작",
                onClick = { onSelected(selectedType) },
                fullWidth = true,
            )
        }
    }
}
