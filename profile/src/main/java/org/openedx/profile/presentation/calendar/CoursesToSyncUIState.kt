package org.openedx.profile.presentation.calendar

import org.openedx.core.domain.model.CourseCalendarState
import org.openedx.core.domain.model.EnrollmentStatus

data class CoursesToSyncUIState(
    val enrollmentsStatus: List<EnrollmentStatus>,
    val coursesCalendarState: List<CourseCalendarState>,
    val isHideInactiveCourses: Boolean,
    val isLoading: Boolean
)
