package org.openedx.core.presentation.course

import org.openedx.core.R

/**
 * This enum defines the Date type of Course Dates
 */
enum class CourseDatesBadge {
    TODAY, BLANK, VERIFIED_ONLY, COMPLETED, PAST_DUE, DUE_NEXT, NOT_YET_RELEASED,
    COURSE_EXPIRED_DATE;

    /**
     * @return The string resource's ID if it's a valid enum inside [CourseDatesBadge], otherwise -1.
     */
    fun getStringResIdForDateType(): Int {
        return when (this) {
            TODAY -> R.string.core_date_type_today
            VERIFIED_ONLY -> R.string.core_date_type_verified_only
            COMPLETED -> R.string.core_date_type_completed
            PAST_DUE -> R.string.core_date_type_past_due
            DUE_NEXT -> R.string.core_date_type_due_next
            NOT_YET_RELEASED -> R.string.core_date_type_not_yet_released
            else -> -1
        }
    }
}
