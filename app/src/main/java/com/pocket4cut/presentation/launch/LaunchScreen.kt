package com.pocket4cut.presentation.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import kotlinx.coroutines.delay

@Composable
fun LaunchScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        delay(1_150)
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppSpacing.Screen.horizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FilmStripMark()
            Spacer(Modifier.height(AppSpacing.xl))
            TextLogo()
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppSpacing.xxxl),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(28.dp)
                    .height(1.dp)
                    .background(AppColors.Border.medium),
            )
            Text(
                text = "SELF PHOTO BOOTH",
                style = AppTypography.caption2.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.6.sp,
                ),
                color = AppColors.Text.secondary,
            )
            Box(
                Modifier
                    .width(28.dp)
                    .height(1.dp)
                    .background(AppColors.Border.medium),
            )
        }
    }
}

@Composable
private fun FilmStripMark() {
    val stripShape = RoundedCornerShape(AppLayout.Radius.xs)
    Column(
        modifier = Modifier
            .width(72.dp)
            .border(2.dp, AppColors.Text.primary, stripShape)
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(width = 58.dp, height = 42.dp)
                    .background(
                        color = if (index == 3) AppColors.Accent.pink else AppColors.Background.card,
                        shape = RoundedCornerShape(AppLayout.Radius.none),
                    )
                    .border(
                        width = 1.dp,
                        color = if (index == 3) AppColors.Accent.pink else AppColors.Border.medium,
                    ),
            )
        }
    }
}

@Composable
private fun TextLogo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "POCKET",
            style = AppTypography.title1.copy(
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
            ),
            color = AppColors.Text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "4CUT",
            style = AppTypography.largeTitle.copy(
                fontSize = 42.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
            ),
            color = AppColors.Accent.pink,
            textAlign = TextAlign.Center,
        )
    }
}
