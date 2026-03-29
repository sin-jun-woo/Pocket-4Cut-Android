package com.pocket4cut.presentation.frameTypeSelect

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*

@Composable
fun FrameTypeSelectScreen(
    onBack: () -> Unit,
    onSelected: (FrameType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = remember { listOf(FrameType.TWO_CUT, FrameType.FOUR_CUT, FrameType.SIX_CUT) }
    var selectedType by remember { mutableStateOf(FrameType.FOUR_CUT) }

    val cardShape = RoundedCornerShape(AppLayout.Radius.lg)
    val footerShape = RoundedCornerShape(
        bottomStart = AppLayout.Radius.lg,
        bottomEnd = AppLayout.Radius.lg,
    )
    val badgeSize = IconCircleButtonSize.LG.toLayoutDp()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppSpacing.xxxl,
                        start = AppSpacing.Screen.horizontal,
                        end = AppSpacing.Screen.horizontal,
                        bottom = AppSpacing.xl,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "사진 구성 선택",
                        style = AppTypography.title1,
                        color = AppColors.Text.primary,
                        modifier = Modifier.weight(1f),
                    )
                    IconCircleButton(
                        onClick = onBack,
                        presetSize = IconCircleButtonSize.MD,
                        variant = IconButtonVariant.SOLID,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = AppColors.Text.primary,
                            modifier = Modifier.size(IconCircleButtonSize.MD.toIconDp()),
                        )
                    }
                }
                Spacer(Modifier.height(AppSpacing.xxs))
                Text(
                    text = "원하는 컷 수를 선택해주세요",
                    style = AppTypography.callout,
                    color = AppColors.Text.secondary,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                types.forEach { type ->
                    val isSelected = selectedType == type
                    val layoutCount = FrameLayouts.bySlots(type.selectCount).size

                    val cardBg by animateColorAsState(
                        targetValue = if (isSelected) AppColors.Accent.pinkSubtle else AppColors.Background.tertiary,
                        animationSpec = AppAnimation.defaultSpring(),
                        label = "frameTypeCardBg",
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) AppColors.Accent.pink else AppColors.Border.subtle,
                        animationSpec = AppAnimation.defaultSpring(),
                        label = "frameTypeCardBorder",
                    )
                    val borderWidth by animateDpAsState(
                        targetValue = if (isSelected) AppLayout.BorderWidth.medium else AppLayout.BorderWidth.thin,
                        animationSpec = AppAnimation.defaultSpring(),
                        label = "frameTypeCardBorderW",
                    )

                    val pinkGlow = AppColors.Accent.pink.copy(alpha = 0.42f)
                    val cardModifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSelected) {
                                Modifier.shadow(
                                    elevation = 16.dp,
                                    shape = cardShape,
                                    clip = false,
                                    ambientColor = pinkGlow,
                                    spotColor = pinkGlow,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clip(cardShape)
                        .border(borderWidth, borderColor, cardShape)
                        .background(cardBg)

                    ScaleOnPress(onClick = { selectedType = type }, modifier = cardModifier) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(AppSpacing.lg)
                                        .padding(end = if (isSelected) 36.dp else 0.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(badgeSize)
                                            .clip(RoundedCornerShape(AppLayout.Radius.md))
                                            .background(
                                                if (isSelected) AppColors.Accent.pink
                                                else AppColors.Overlay.white,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = type.selectCount.toString(),
                                            style = AppTypography.title1,
                                            color = if (isSelected) {
                                                AppColors.Text.primary
                                            } else {
                                                AppColors.Accent.pink
                                            },
                                        )
                                    }
                                    Spacer(Modifier.width(AppSpacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = type.description,
                                            style = AppTypography.headline,
                                            color = AppColors.Text.primary,
                                        )
                                        Spacer(Modifier.height(AppSpacing.xxs))
                                        Text(
                                            text = type.subtitle,
                                            style = AppTypography.subheadline,
                                            color = AppColors.Text.secondary,
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = AppColors.Accent.pink,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(
                                                top = AppSpacing.md,
                                                end = AppSpacing.md,
                                            )
                                            .size(28.dp),
                                    )
                                }
                            }
                            Text(
                                text = "${layoutCount}가지 레이아웃 사용 가능 (사진 선택 후 고를 수 있어요)",
                                style = AppTypography.caption1,
                                color = AppColors.Text.tertiary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(footerShape)
                                    .background(AppColors.Background.secondary)
                                    .padding(
                                        horizontal = AppSpacing.lg,
                                        vertical = AppSpacing.sm,
                                    ),
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary)
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(bottom = AppSpacing.Layout.ctaBottomSpace, top = AppSpacing.md),
        ) {
            PrimaryButton(
                text = "${selectedType.displayName} 선택",
                onClick = { onSelected(selectedType) },
                fullWidth = true,
            )
        }
    }
}
