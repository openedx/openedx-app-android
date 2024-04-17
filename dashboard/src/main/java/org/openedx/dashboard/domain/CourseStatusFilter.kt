package org.openedx.dashboard.domain

import androidx.annotation.StringRes
import org.openedx.dashboard.R

enum class CourseStatusFilter(val key: String, @StringRes val text: Int) {
    ALL("all", R.string.dashboard_course_filter_all),
    IN_PROGRESS("in_progress", R.string.dashboard_course_filter_in_progress),
    COMPLETE("completed", R.string.dashboard_course_filter_completed),
    EXPIRED("expired", R.string.dashboard_course_filter_expired)
}