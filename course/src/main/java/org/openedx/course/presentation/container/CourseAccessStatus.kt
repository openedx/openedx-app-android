package org.openedx.course.presentation.container

import java.util.Date

data class CourseAccessStatus(
    val accessError: CourseAccessError? = null,
    val date: Date? = null,
    val sku: String? = null
)

enum class CourseAccessError {
    COURSE_EXPIRED_NOT_UPGRADABLE, COURSE_EXPIRED_UPGRADABLE, COURSE_NOT_STARTED, COURSE_NO_ACCESS
}

