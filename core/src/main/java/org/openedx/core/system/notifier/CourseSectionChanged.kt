package org.openedx.core.system.notifier

data class CourseSectionChanged(
    val blockId: String
) : CourseEvent
