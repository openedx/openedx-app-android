package org.openedx.dates.presentation.dates

import androidx.annotation.StringRes
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.openedx.core.ui.theme.appColors
import org.openedx.dates.R

enum class DueDateCategory(
    @StringRes
    val label: Int,
) {
    UPCOMING(R.string.dates_category_upcoming),
    NEXT_WEEK(R.string.dates_category_next_week),
    THIS_WEEK(R.string.dates_category_this_week),
    TODAY(R.string.dates_category_today),
    PAST_DUE(R.string.dates_category_past_due);

    val color: Color
        @Composable
        get() {
            return when (this) {
                PAST_DUE -> MaterialTheme.appColors.warning
                TODAY -> MaterialTheme.appColors.info
                THIS_WEEK -> MaterialTheme.appColors.textPrimaryVariant
                NEXT_WEEK -> MaterialTheme.appColors.textFieldBorder
                UPCOMING -> MaterialTheme.appColors.divider
            }
        }
}
