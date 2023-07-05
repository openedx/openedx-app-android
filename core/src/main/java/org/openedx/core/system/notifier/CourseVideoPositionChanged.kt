package org.openedx.core.system.notifier

data class CourseVideoPositionChanged(
    val videoUrl: String,
    val videoTime: Long
) : CourseEvent