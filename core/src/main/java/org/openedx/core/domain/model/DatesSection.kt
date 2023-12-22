package org.openedx.core.domain.model

import android.content.Context
import org.openedx.core.R

enum class DatesSection(private val stringResId: Int) {
    COMPLETED(R.string.core_date_type_completed),
    PAST_DUE(R.string.core_date_type_past_due),
    TODAY(R.string.core_date_type_today),
    THIS_WEEK(R.string.core_date_type_this_week),
    NEXT_WEEK(R.string.core_date_type_next_week),
    UPCOMING(R.string.core_date_type_upcoming),
    NONE(R.string.core_date_type_none);

    fun getTitle(context: Context): String {
        return context.getString(stringResId)
    }
}
