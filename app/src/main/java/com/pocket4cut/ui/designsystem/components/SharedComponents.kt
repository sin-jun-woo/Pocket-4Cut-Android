package com.pocket4cut.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.*

enum class AppToastType {
    Success,
    Error,
    Warning,
    Info,
}

private fun AppToastType.icon(): ImageVector = when (this) {
    AppToastType.Success -> Icons.Filled.CheckCircle
    AppToastType.Error -> Icons.Filled.Error
    AppToastType.Warning -> Icons.Filled.Warning
    AppToastType.Info -> Icons.Filled.Info
}

private fun AppToastType.tint(): Color = when (this) {
    AppToastType.Success -> AppColors.Semantic.success
    AppToastType.Error -> AppColors.Semantic.error
    AppToastType.Warning -> AppColors.Semantic.warning
    AppToastType.Info -> AppColors.Semantic.info
}

@Composable
fun AppToast(
    message: String,
    type: AppToastType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppLayout.Radius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f),
            )
            .clip(shape)
            .background(AppColors.Background.tertiary)
            .border(1.dp, AppColors.Border.light, shape)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Icon(
            imageVector = type.icon(),
            contentDescription = null,
            tint = type.tint(),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = message,
            style = AppTypography.callout,
            color = AppColors.Text.primary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "닫기",
                tint = AppColors.Text.secondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun LoadingOverlay(
    visible: Boolean,
    message: String = "처리 중...",
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Overlay.heavy),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = AppColors.Accent.pink,
                strokeWidth = 3.dp,
            )
            Text(
                text = message,
                style = AppTypography.callout,
                color = AppColors.Text.primary,
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = AppSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = AppTypography.title3,
                color = AppColors.Text.primary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTypography.footnote,
                    color = AppColors.Text.secondary,
                    modifier = Modifier.padding(top = AppSpacing.xxs),
                )
            }
        }
        if (trailing != null) {
            Box(modifier = Modifier.padding(start = AppSpacing.sm)) {
                trailing()
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AppColors.Overlay.white),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.height(AppSpacing.xl))
        Text(text = title, style = AppTypography.title3, color = AppColors.Text.secondary)
        Spacer(Modifier.height(AppSpacing.xs))
        Text(text = description, style = AppTypography.callout, color = AppColors.Text.tertiary)
        if (action != null) {
            Spacer(Modifier.height(AppSpacing.xl))
            action()
        }
    }
}

@Composable
fun BottomCTABar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Background.primary)
            .padding(horizontal = AppSpacing.Screen.horizontal)
            .padding(bottom = AppSpacing.Layout.ctaBottomSpace),
        content = content,
    )
}

@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) AppColors.Accent.pink else AppColors.Overlay.white
    val borderColor = if (selected) androidx.compose.ui.graphics.Color.Transparent else AppColors.Border.light
    val textColor = if (selected) AppColors.Text.primary else AppColors.Text.secondary
    val fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal

    Text(
        text = label,
        style = AppTypography.subheadline.copy(fontWeight = fontWeight, color = textColor),
        modifier = modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(bg)
            .then(if (!selected) Modifier.border(1.dp, borderColor, RoundedCornerShape(9999.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
    )
}
