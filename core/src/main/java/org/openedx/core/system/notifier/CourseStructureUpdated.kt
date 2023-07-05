package org.openedx.core.system.notifier

class CourseStructureUpdated(
    val courseId: String,
    val withSwipeRefresh: Boolean,
) : CourseEvent