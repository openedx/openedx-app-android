package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class EnrolledCourse(
    val auditAccessExpires: Date?,
    val created: String,
    val mode: String,
    val isActive: Boolean,
    val course: EnrolledCourseData,
    val certificate: Certificate?,
    val progress: Progress,
    val courseStatus: CourseStatus?,
    val courseAssignments: CourseAssignments?
) : Parcelable
