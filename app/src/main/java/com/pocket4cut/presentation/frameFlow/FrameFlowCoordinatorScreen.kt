package com.pocket4cut.presentation.frameFlow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton

@Composable
fun FrameFlowCoordinatorScreen(
    onColorPick: () -> Unit,
    onSeasonPick: () -> Unit,
    onCustomEditor: () -> Unit,
    onDismiss: () -> Unit,
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
                    bottom = AppSpacing.xxl,
                ),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "프레임 방식",
                    style = AppTypography.title1,
                    color = AppColors.Text.primary,
                )
                Spacer(Modifier.height(AppSpacing.xxs))
                Text(
                    text = "사진에 어울리는 마감 방식을 고르세요.",
                    style = AppTypography.callout,
                    color = AppColors.Text.secondary,
                )
            }
            IconCircleButton(onClick = onDismiss, variant = IconButtonVariant.SOLID) {
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
                .padding(horizontal = AppSpacing.Screen.horizontal),
        ) {
            FrameModeRow(
                code = "A",
                icon = Icons.Default.Palette,
                title = "COLOR",
                subtitle = "한 가지 색으로 또렷하게",
                onClick = onColorPick,
            )
            HorizontalDivider(color = AppColors.Border.subtle)
            FrameModeRow(
                code = "B",
                icon = Icons.Default.Photo,
                title = "SEASON",
                subtitle = "계절의 질감과 작은 장식",
                onClick = onSeasonPick,
            )
            HorizontalDivider(color = AppColors.Border.subtle)
            FrameModeRow(
                code = "C",
                icon = Icons.Default.Edit,
                title = "CUSTOM",
                subtitle = "색, 문구, 스티커를 직접 편집",
                onClick = onCustomEditor,
            )
            HorizontalDivider(color = AppColors.Border.subtle)
        }
    }
}

@Composable
private fun FrameModeRow(
    code: String,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AppSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = code,
            style = AppTypography.caption1.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = AppColors.Accent.pink,
            modifier = Modifier.width(28.dp),
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = AppColors.Background.card,
                    shape = RoundedCornerShape(2.dp),
                )
                .border(1.dp, AppColors.Border.medium, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.Text.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(AppSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.headline.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
                color = AppColors.Text.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = AppTypography.subheadline,
                color = AppColors.Text.secondary,
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AppColors.Text.tertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}
