package org.openedx.core.domain.model

import androidx.compose.ui.graphics.Color

data class CourseProgress(
    val mfeProctoredExamSettingsUrl: String,
    val courseAssignmentLists: Map<String, List<String>>,
    val courseDetails: CourseDetails,
    val showCreditEligibility: Boolean,
    val isCreditCourse: Boolean,
    val defaultGradeDesignations: List<String>
) {
    data class CourseDetails(
        val graders: List<Grader>,
        val gradeCutoffs: Map<String, Float>,
        val gracePeriod: GracePeriod,
        val minimumGradeCredit: Float
    )

    data class Grader(
        val type: String,
        val minCount: Int,
        val dropCount: Int,
        val shortLabel: String,
        val weight: Int,
        val id: String,
        val color: Color
    )

    data class GracePeriod(
        val hours: Int,
        val minutes: Int
    )
}
