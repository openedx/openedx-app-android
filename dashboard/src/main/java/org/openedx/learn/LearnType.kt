package org.openedx.learn

import androidx.annotation.StringRes
import org.openedx.dashboard.R

enum class LearnType(@StringRes val title: Int) {
    COURSES(R.string.dashboard_courses),
    PROGRAMS(R.string.dashboard_programs)
}
