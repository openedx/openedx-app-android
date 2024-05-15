package org.openedx.core.system.notifier

import org.openedx.core.presentation.course.CourseContainerTab

data class CourseRefresh(val courseContainerTab: CourseContainerTab) : CourseEvent
