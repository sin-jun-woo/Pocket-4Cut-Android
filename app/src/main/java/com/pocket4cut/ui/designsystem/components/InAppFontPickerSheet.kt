package com.pocket4cut.ui.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.pocket4cut.core.util.AppFontCatalog
import com.pocket4cut.core.util.FontOption
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppFontPickerSheet(
    selectedFontName: String?,
    onFontSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val options = remember(context) { AppFontCatalog.options(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun isSelected(opt: FontOption): Boolean {
        val sel = selectedFontName
        val default = sel.isNullOrEmpty() || sel == "system"
        return if (opt.id == "system") default else opt.id == sel
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Background.card,
        contentColor = AppColors.Text.primary,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(bottom = AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            items(options, key = { it.id }) { opt ->
                val family = opt.fontFamily ?: FontFamily.Default
                val selected = isSelected(opt)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val idOrNull = if (opt.id == "system") null else opt.id
                            onFontSelected(idOrNull)
                            onDismiss()
                        }
                        .padding(vertical = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = opt.displayName,
                        style = AppTypography.body.copy(fontFamily = family),
                        color = AppColors.Text.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = AppColors.Accent.pink,
                        )
                    }
                }
            }
        }
    }
}
