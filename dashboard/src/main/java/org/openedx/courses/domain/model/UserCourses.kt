package org.openedx.courses.domain.model

import org.openedx.core.domain.model.EnrolledCourse

data class UserCourses(
    val enrollments: List<EnrolledCourse>,
    val primary: EnrolledCourse?
)
