package org.openedx.core.system.notifier

import org.openedx.core.domain.model.CourseStructure

data class CourseDataReady(val courseStructure: CourseStructure) : CourseEvent
