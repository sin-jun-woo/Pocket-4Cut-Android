package com.pocket4cut.presentation.frameTypeSelect

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton

@Composable
fun FrameTypeSelectScreen(
    onBack: () -> Unit,
    onSelected: (FrameType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = remember { listOf(FrameType.TWO_CUT, FrameType.FOUR_CUT, FrameType.SIX_CUT) }
    var selectedType by remember { mutableStateOf(FrameType.FOUR_CUT) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppSpacing.xxxl,
                        start = AppSpacing.Screen.horizontal,
                        end = AppSpacing.Screen.horizontal,
                        bottom = AppSpacing.xxl,
                    ),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "사진 구성",
                        style = AppTypography.title1,
                        color = AppColors.Text.primary,
                    )
                    Spacer(Modifier.height(AppSpacing.xxs))
                    Text(
                        text = "완성할 인화지의 컷 수를 고르세요.",
                        style = AppTypography.callout,
                        color = AppColors.Text.secondary,
                    )
                }
                IconCircleButton(
                    onClick = onBack,
                    variant = IconButtonVariant.SOLID,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                types.forEachIndexed { index, type ->
                    FrameTypeRow(
                        index = index + 1,
                        type = type,
                        layoutCount = FrameLayouts.bySlots(type.selectCount).size,
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary)
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(top = AppSpacing.md, bottom = AppSpacing.Layout.ctaBottomSpace),
        ) {
            PrimaryButton(
                text = "${selectedType.displayName}으로 촬영",
                onClick = { onSelected(selectedType) },
            )
        }
    }
}

@Composable
private fun FrameTypeRow(
    index: Int,
    type: FrameType,
    layoutCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppLayout.Radius.md)
    val background by animateColorAsState(
        targetValue = if (selected) AppColors.Accent.pinkSubtle else AppColors.Background.card,
        label = "frameTypeBackground",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AppColors.Accent.pink else AppColors.Border.subtle,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(64.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(AppColors.Accent.pink),
                )
            }
            Text(
                text = index.toString().padStart(2, '0'),
                style = AppTypography.caption1.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = if (selected) AppColors.Accent.pink else AppColors.Text.tertiary,
                modifier = Modifier.padding(start = 9.dp),
            )
        }

        Spacer(Modifier.width(AppSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${type.selectCount} CUT",
                style = AppTypography.title2.copy(fontWeight = FontWeight.Bold),
                color = AppColors.Text.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = type.subtitle,
                style = AppTypography.subheadline,
                color = AppColors.Text.secondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "레이아웃 ${layoutCount}종",
                style = AppTypography.caption1,
                color = AppColors.Text.tertiary,
            )
        }
        MiniPrintLayout(type.selectCount, selected)
    }
}

@Composable
private fun MiniPrintLayout(slotCount: Int, selected: Boolean) {
    val line = if (selected) AppColors.Accent.pink else AppColors.Border.medium
    Column(
        modifier = Modifier
            .size(width = 42.dp, height = 64.dp)
            .border(1.dp, line)
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(slotCount.coerceAtMost(6)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AppColors.Background.secondary)
                    .border(1.dp, line.copy(alpha = 0.6f)),
            )
        }
    }
}
