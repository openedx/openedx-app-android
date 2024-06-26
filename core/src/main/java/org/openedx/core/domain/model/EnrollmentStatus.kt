package org.openedx.core.domain.model

data class EnrollmentStatus(
    val courseId: String,
    val courseName: String,
    val isActive: Boolean
)
