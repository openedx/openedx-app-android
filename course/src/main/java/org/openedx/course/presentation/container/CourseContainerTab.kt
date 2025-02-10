package org.openedx.course.presentation.container

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.ui.graphics.vector.ImageVector
import org.openedx.core.ui.TabItem
import org.openedx.course.R

enum class CourseContainerTab(
    @StringRes
    override val labelResId: Int,
    override val icon: ImageVector,
) : TabItem {
    HOME(R.string.course_container_nav_home, Icons.Default.Home),
    VIDEOS(R.string.course_container_nav_videos, Icons.Rounded.PlayCircleFilled),
    DATES(R.string.course_container_nav_dates, Icons.Outlined.CalendarMonth),
    OFFLINE(R.string.course_container_nav_downloads, Icons.Outlined.CloudDownload),
    DISCUSSIONS(R.string.course_container_nav_discussions, Icons.AutoMirrored.Filled.Chat),
    MORE(R.string.course_container_nav_more, Icons.AutoMirrored.Filled.TextSnippet)
}
