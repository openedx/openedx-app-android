package org.openedx.core.presentation.course

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.ui.graphics.vector.ImageVector
import org.openedx.core.R
import org.openedx.core.ui.TabItem

enum class CourseContainerTab(@StringRes override val labelResId: Int, override val icon: ImageVector) : TabItem {
    HOME(R.string.core_course_container_nav_home, Icons.Default.Home),
    VIDEOS(R.string.core_course_container_nav_videos, Icons.Rounded.PlayCircleFilled),
    DATES(R.string.core_course_container_nav_dates, Icons.Outlined.CalendarMonth),
    DISCUSSIONS(R.string.core_course_container_nav_discussions, Icons.AutoMirrored.Filled.Chat),
    MORE(R.string.core_course_container_nav_more, Icons.AutoMirrored.Filled.TextSnippet)
}
