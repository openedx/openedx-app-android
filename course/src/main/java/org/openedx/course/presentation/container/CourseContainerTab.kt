package org.openedx.course.presentation.container

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Moving
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.ui.graphics.vector.ImageVector
import org.openedx.core.ui.TabItem
import org.openedx.course.R

enum class CourseContainerTab(
    @StringRes
    override val labelResId: Int,
    override val icon: ImageVector,
) : TabItem {
    HOME(R.string.course_container_nav_home, Icons.Default.Home),
    CONTENT(R.string.course_container_nav_content, Icons.AutoMirrored.Filled.List),
    PROGRESS(R.string.course_container_nav_progress, Icons.Default.Moving),
    DATES(R.string.course_container_nav_dates, Icons.Outlined.CalendarMonth),
    OFFLINE(R.string.course_container_nav_downloads, Icons.Filled.CloudDownload),
    DISCUSSIONS(R.string.course_container_nav_discussions, Icons.AutoMirrored.Filled.Chat),
    MORE(R.string.course_container_nav_more, Icons.AutoMirrored.Filled.TextSnippet),
}

enum class CourseContentTab(
    @StringRes
    val labelResId: Int
) {
    ALL(R.string.course_container_content_tab_all),
    VIDEOS(R.string.course_container_content_tab_video),
    ASSIGNMENTS(R.string.course_container_content_tab_assignment)
}
