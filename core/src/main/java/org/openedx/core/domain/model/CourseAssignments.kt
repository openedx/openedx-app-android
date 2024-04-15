package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CourseAssignments(
    val futureAssignment: CourseDateBlock?,
    val pastAssignments: List<CourseDateBlock>?
): Parcelable
