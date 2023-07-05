package org.openedx.core.system.notifier

data class CourseSubtitleLanguageChanged(
    val value: String
) : CourseEvent
