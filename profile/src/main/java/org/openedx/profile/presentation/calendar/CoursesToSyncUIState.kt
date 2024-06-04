package org.openedx.profile.presentation.calendar

import org.openedx.core.domain.model.EnrollmentStatus

data class CoursesToSyncUIState(
    val enrollmentsStatus: List<EnrollmentStatus>,
    val isHideInactiveCourses: Boolean
)
