package com.pocket4cut.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.legal.PrivacyPolicyContent
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
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
            IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = AppColors.Text.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = "개인정보 처리방침",
                style = AppTypography.title2,
                color = AppColors.Text.primary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(AppLayout.Height.IconButton.md))
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = AppColors.Border.subtle,
        )
        Box(modifier = Modifier.weight(1f)) {
            SelectionContainer {
                Text(
                    text = PrivacyPolicyContent.fullTextKo,
                    style = AppTypography.body,
                    color = AppColors.Text.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = AppSpacing.Screen.horizontal,
                            vertical = AppSpacing.lg,
                        ),
                )
            }
        }
    }
}
