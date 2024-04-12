package org.openedx.courses.domain.model

import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.core.domain.model.EnrolledCourse

data class UserCourses(
    val enrollments: DashboardCourseList,
    val primary: EnrolledCourse?
)
