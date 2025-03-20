package org.openedx.core.domain.model

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.openedx.core.R
import org.openedx.core.ui.theme.appColors

enum class DatesSection(val stringResId: Int) {
    COMPLETED(R.string.core_date_type_completed),
    PAST_DUE(R.string.core_date_type_past_due),
    TODAY(R.string.core_date_type_today),
    THIS_WEEK(R.string.core_date_type_this_week),
    NEXT_WEEK(R.string.core_date_type_next_week),
    UPCOMING(R.string.core_date_type_upcoming),
    NONE(R.string.core_date_type_none);

    val color: Color
        @Composable
        get() {
            return when (this) {
                COMPLETED -> MaterialTheme.appColors.cardViewBackground
                PAST_DUE -> MaterialTheme.appColors.datesSectionBarPastDue
                TODAY -> MaterialTheme.appColors.datesSectionBarToday
                THIS_WEEK -> MaterialTheme.appColors.datesSectionBarThisWeek
                NEXT_WEEK -> MaterialTheme.appColors.datesSectionBarNextWeek
                UPCOMING -> MaterialTheme.appColors.datesSectionBarUpcoming
                else -> MaterialTheme.appColors.background
            }
        }
}
