package org.openedx.core.domain.model

import androidx.compose.ui.graphics.Color

data class CourseProgress(
    val verifiedMode: String,
    val accessExpiration: String,
    val certificateData: CertificateData?,
    val completionSummary: CompletionSummary?,
    val courseGrade: CourseGrade?,
    val creditCourseRequirements: String,
    val end: String,
    val enrollmentMode: String,
    val gradingPolicy: GradingPolicy?,
    val hasScheduledContent: Boolean,
    val sectionScores: List<SectionScore>,
    val studioUrl: String,
    val username: String,
    val userHasPassingGrade: Boolean,
    val verificationData: VerificationData?,
    val disableProgressGraph: Boolean,
) {
    val completion = with(completionSummary) {
        val total = (this?.completeCount ?: 0) + (this?.incompleteCount ?: 0)
        if (total > 0f) (this?.completeCount ?: 0).toFloat() / total else 0f
    }
    val completionPercent = (completion * 100f).toInt()
    val requiredGrade = gradingPolicy?.gradeRange?.values?.firstOrNull() ?: 0f
    val requiredGradePercent = (requiredGrade * 100f).toInt()

    fun getEarnedAssignmentProblems(
        policy: GradingPolicy.AssignmentPolicy
    ) = sectionScores
        .flatMap { section ->
            section.subsections.filter { it.assignmentType == policy.type }
        }.sumOf { subsection ->
            subsection.problemScores.sumOf { it.earned }
        }

    fun getPossibleAssignmentProblems(
        policy: GradingPolicy.AssignmentPolicy
    ) = sectionScores
        .flatMap { section ->
            section.subsections.filter { it.assignmentType == policy.type }
        }.sumOf { subsection ->
            subsection.problemScores.sumOf { it.possible }
        }

    fun getAssignmentGradedPercent(type: String): Float {
        val assignmentSections = sectionScores
            .flatMap { it.subsections }
            .filter { it.assignmentType == type }
        if (assignmentSections.isEmpty()) return 0f
        return assignmentSections.sumOf { it.percentGraded }.toFloat() / assignmentSections.size
    }

    fun getAssignmentWeightedGradedPercent(assignmentPolicy: GradingPolicy.AssignmentPolicy): Float {
        return (assignmentPolicy.weight * getAssignmentGradedPercent(assignmentPolicy.type) * 100f).toFloat()
    }

    fun getTotalWeightPercent() =
        gradingPolicy?.assignmentPolicies?.sumOf { getAssignmentWeightedGradedPercent(it).toDouble() }
            ?.toFloat() ?: 0f

    fun getNotCompletedWeightedGradePercent(): Float {
        val totalWeightedPercent = getTotalWeightPercent()
        val notCompletedPercent = 100.0 - totalWeightedPercent
        return if (notCompletedPercent < 0.0) 0f else notCompletedPercent.toFloat()
    }

    data class CertificateData(
        val certStatus: String,
        val certWebViewUrl: String,
        val downloadUrl: String,
        val certificateAvailableDate: String
    )

    data class CompletionSummary(
        val completeCount: Int,
        val incompleteCount: Int,
        val lockedCount: Int
    )

    data class CourseGrade(
        val letterGrade: String,
        val percent: Double,
        val isPassing: Boolean
    )

    data class GradingPolicy(
        val assignmentPolicies: List<AssignmentPolicy>,
        val gradeRange: Map<String, Float>,
        val assignmentColors: List<Color>,
    ) {
        data class AssignmentPolicy(
            val numDroppable: Int,
            val numTotal: Int,
            val shortLabel: String,
            val type: String,
            val weight: Double
        )
    }

    data class SectionScore(
        val displayName: String,
        val subsections: List<Subsection>
    ) {
        data class Subsection(
            val assignmentType: String,
            val blockKey: String,
            val displayName: String,
            val hasGradedAssignment: Boolean,
            val override: String,
            val learnerHasAccess: Boolean,
            val numPointsEarned: Float,
            val numPointsPossible: Float,
            val percentGraded: Double,
            val problemScores: List<ProblemScore>,
            val showCorrectness: String,
            val showGrades: Boolean,
            val url: String
        ) {
            data class ProblemScore(
                val earned: Double,
                val possible: Double
            )
        }
    }

    data class VerificationData(
        val link: String,
        val status: String,
        val statusDate: String
    )
}
