package com.pocket4cut.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.*

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.padding(bottom = AppSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = AppTypography.headline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = AppColors.Text.secondary)
            if (trailing != null) trailing()
        }
        if (subtitle != null) {
            Text(text = subtitle, style = AppTypography.footnote, color = AppColors.Text.secondary, modifier = Modifier.padding(top = AppSpacing.xxs))
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
