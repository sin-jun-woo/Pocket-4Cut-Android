package com.pocket4cut.presentation.launch

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.pocket4cut.ui.designsystem.*
import kotlinx.coroutines.delay

@Composable
fun LaunchScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(AppLayout.Radius.xl))
                    .background(AppColors.Accent.pink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = AppColors.Text.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(AppSpacing.xl))
            Text(
                text = "Pocket 4Cut",
                style = AppTypography.title1.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
                color = AppColors.Text.primary,
            )
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                text = "나만의 인생네컷",
                style = AppTypography.callout.copy(letterSpacing = 0.5.sp),
                color = AppColors.Text.secondary,
            )
        }

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .size(24.dp),
            color = AppColors.Accent.pink,
            strokeWidth = 2.dp,
        )
    }
}
