package org.openedx.core.data.model

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.CertificateDataDb
import org.openedx.core.data.model.room.CompletionSummaryDb
import org.openedx.core.data.model.room.CourseGradeDb
import org.openedx.core.data.model.room.CourseProgressEntity
import org.openedx.core.data.model.room.GradingPolicyDb
import org.openedx.core.data.model.room.SectionScoreDb
import org.openedx.core.data.model.room.VerificationDataDb
import org.openedx.core.domain.model.CourseProgress

data class CourseProgressResponse(
    @SerializedName("verified_mode") val verifiedMode: String?,
    @SerializedName("access_expiration") val accessExpiration: String?,
    @SerializedName("certificate_data") val certificateData: CertificateData?,
    @SerializedName("completion_summary") val completionSummary: CompletionSummary?,
    @SerializedName("course_grade") val courseGrade: CourseGrade?,
    @SerializedName("credit_course_requirements") val creditCourseRequirements: String?,
    @SerializedName("end") val end: String?,
    @SerializedName("enrollment_mode") val enrollmentMode: String?,
    @SerializedName("grading_policy") val gradingPolicy: GradingPolicy?,
    @SerializedName("has_scheduled_content") val hasScheduledContent: Boolean?,
    @SerializedName("section_scores") val sectionScores: List<SectionScore>?,
    @SerializedName("studio_url") val studioUrl: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("user_has_passing_grade") val userHasPassingGrade: Boolean?,
    @SerializedName("verification_data") val verificationData: VerificationData?,
    @SerializedName("disable_progress_graph") val disableProgressGraph: Boolean?,
) {
    data class CertificateData(
        @SerializedName("cert_status") val certStatus: String?,
        @SerializedName("cert_web_view_url") val certWebViewUrl: String?,
        @SerializedName("download_url") val downloadUrl: String?,
        @SerializedName("certificate_available_date") val certificateAvailableDate: String?
    ) {
        fun mapToRoomEntity() = CertificateDataDb(
            certStatus = certStatus.orEmpty(),
            certWebViewUrl = certWebViewUrl.orEmpty(),
            downloadUrl = downloadUrl.orEmpty(),
            certificateAvailableDate = certificateAvailableDate.orEmpty()
        )

        fun mapToDomain() = CourseProgress.CertificateData(
            certStatus = certStatus ?: "",
            certWebViewUrl = certWebViewUrl ?: "",
            downloadUrl = downloadUrl ?: "",
            certificateAvailableDate = certificateAvailableDate ?: ""
        )
    }

    data class CompletionSummary(
        @SerializedName("complete_count") val completeCount: Int?,
        @SerializedName("incomplete_count") val incompleteCount: Int?,
        @SerializedName("locked_count") val lockedCount: Int?
    ) {
        fun mapToRoomEntity() = CompletionSummaryDb(
            completeCount = completeCount ?: 0,
            incompleteCount = incompleteCount ?: 0,
            lockedCount = lockedCount ?: 0
        )

        fun mapToDomain() = CourseProgress.CompletionSummary(
            completeCount = completeCount ?: 0,
            incompleteCount = incompleteCount ?: 0,
            lockedCount = lockedCount ?: 0
        )
    }

    data class CourseGrade(
        @SerializedName("letter_grade") val letterGrade: String?,
        @SerializedName("percent") val percent: Double?,
        @SerializedName("is_passing") val isPassing: Boolean?
    ) {
        fun mapToRoomEntity() = CourseGradeDb(
            letterGrade = letterGrade.orEmpty(),
            percent = percent ?: 0.0,
            isPassing = isPassing ?: false
        )

        fun mapToDomain() = CourseProgress.CourseGrade(
            letterGrade = letterGrade ?: "",
            percent = percent ?: 0.0,
            isPassing = isPassing ?: false
        )
    }

    data class GradingPolicy(
        @SerializedName("assignment_policies") val assignmentPolicies: List<AssignmentPolicy>?,
        @SerializedName("grade_range") val gradeRange: Map<String, Float>?,
        @SerializedName("assignment_colors") val assignmentColors: List<String>?
    ) {
        // TODO Temporary solution. Backend will returns color list later
        val defaultColors = listOf(
            "#D24242",
            "#7B9645",
            "#5A5AD8",
            "#B0842C",
            "#2E90C2",
            "#D13F88",
            "#36A17D",
            "#AE5AD8",
            "#3BA03B"
        )

        fun mapToRoomEntity() = GradingPolicyDb(
            assignmentPolicies = assignmentPolicies?.map { it.mapToRoomEntity() } ?: emptyList(),
            gradeRange = gradeRange ?: emptyMap(),
            assignmentColors = assignmentColors ?: defaultColors
        )

        fun mapToDomain() = CourseProgress.GradingPolicy(
            assignmentPolicies = assignmentPolicies?.map { it.mapToDomain() } ?: emptyList(),
            gradeRange = gradeRange ?: emptyMap(),
            assignmentColors = assignmentColors?.map { colorString ->
                Color(colorString.toColorInt())
            } ?: defaultColors.map { Color(it.toColorInt()) }
        )

        data class AssignmentPolicy(
            @SerializedName("num_droppable") val numDroppable: Int?,
            @SerializedName("num_total") val numTotal: Int?,
            @SerializedName("short_label") val shortLabel: String?,
            @SerializedName("type") val type: String?,
            @SerializedName("weight") val weight: Double?
        ) {
            fun mapToRoomEntity() = GradingPolicyDb.AssignmentPolicyDb(
                numDroppable = numDroppable ?: 0,
                numTotal = numTotal ?: 0,
                shortLabel = shortLabel.orEmpty(),
                type = type.orEmpty(),
                weight = weight ?: 0.0
            )

            fun mapToDomain() = CourseProgress.GradingPolicy.AssignmentPolicy(
                numDroppable = numDroppable ?: 0,
                numTotal = numTotal ?: 0,
                shortLabel = shortLabel ?: "",
                type = type ?: "",
                weight = weight ?: 0.0
            )
        }
    }

    data class SectionScore(
        @SerializedName("display_name") val displayName: String?,
        @SerializedName("subsections") val subsections: List<Subsection>?
    ) {
        fun mapToRoomEntity() = SectionScoreDb(
            displayName = displayName.orEmpty(),
            subsections = subsections?.map { it.mapToRoomEntity() } ?: emptyList()
        )

        fun mapToDomain() = CourseProgress.SectionScore(
            displayName = displayName ?: "",
            subsections = subsections?.map { it.mapToDomain() } ?: emptyList()
        )

        data class Subsection(
            @SerializedName("assignment_type") val assignmentType: String?,
            @SerializedName("block_key") val blockKey: String?,
            @SerializedName("display_name") val displayName: String?,
            @SerializedName("has_graded_assignment") val hasGradedAssignment: Boolean?,
            @SerializedName("override") val override: String?,
            @SerializedName("learner_has_access") val learnerHasAccess: Boolean?,
            @SerializedName("num_points_earned") val numPointsEarned: Float?,
            @SerializedName("num_points_possible") val numPointsPossible: Float?,
            @SerializedName("percent_graded") val percentGraded: Double?,
            @SerializedName("problem_scores") val problemScores: List<ProblemScore>?,
            @SerializedName("show_correctness") val showCorrectness: String?,
            @SerializedName("show_grades") val showGrades: Boolean?,
            @SerializedName("url") val url: String?
        ) {
            fun mapToRoomEntity() = SectionScoreDb.SubsectionDb(
                assignmentType = assignmentType.orEmpty(),
                blockKey = blockKey.orEmpty(),
                displayName = displayName.orEmpty(),
                hasGradedAssignment = hasGradedAssignment ?: false,
                override = override.orEmpty(),
                learnerHasAccess = learnerHasAccess ?: false,
                numPointsEarned = numPointsEarned ?: 0f,
                numPointsPossible = numPointsPossible ?: 0f,
                percentGraded = percentGraded ?: 0.0,
                problemScores = problemScores?.map { it.mapToRoomEntity() } ?: emptyList(),
                showCorrectness = showCorrectness.orEmpty(),
                showGrades = showGrades ?: false,
                url = url.orEmpty()
            )

            fun mapToDomain() = CourseProgress.SectionScore.Subsection(
                assignmentType = assignmentType ?: "",
                blockKey = blockKey ?: "",
                displayName = displayName ?: "",
                hasGradedAssignment = hasGradedAssignment ?: false,
                override = override ?: "",
                learnerHasAccess = learnerHasAccess ?: false,
                numPointsEarned = numPointsEarned ?: 0f,
                numPointsPossible = numPointsPossible ?: 0f,
                percentGraded = percentGraded ?: 0.0,
                problemScores = problemScores?.map { it.mapToDomain() } ?: emptyList(),
                showCorrectness = showCorrectness ?: "",
                showGrades = showGrades ?: false,
                url = url ?: ""
            )

            data class ProblemScore(
                @SerializedName("earned") val earned: Double?,
                @SerializedName("possible") val possible: Double?
            ) {
                fun mapToRoomEntity() = SectionScoreDb.SubsectionDb.ProblemScoreDb(
                    earned = earned ?: 0.0,
                    possible = possible ?: 0.0
                )

                fun mapToDomain() = CourseProgress.SectionScore.Subsection.ProblemScore(
                    earned = earned ?: 0.0,
                    possible = possible ?: 0.0
                )
            }
        }
    }

    data class VerificationData(
        @SerializedName("link") val link: String?,
        @SerializedName("status") val status: String?,
        @SerializedName("status_date") val statusDate: String?
    ) {
        fun mapToRoomEntity() = VerificationDataDb(
            link = link.orEmpty(),
            status = status.orEmpty(),
            statusDate = statusDate.orEmpty()
        )

        fun mapToDomain() = CourseProgress.VerificationData(
            link = link ?: "",
            status = status ?: "",
            statusDate = statusDate ?: ""
        )
    }

    fun mapToDomain(): CourseProgress {
        return CourseProgress(
            verifiedMode = verifiedMode ?: "",
            accessExpiration = accessExpiration ?: "",
            certificateData = certificateData?.mapToDomain(),
            completionSummary = completionSummary?.mapToDomain(),
            courseGrade = courseGrade?.mapToDomain(),
            creditCourseRequirements = creditCourseRequirements ?: "",
            end = end ?: "",
            enrollmentMode = enrollmentMode ?: "",
            gradingPolicy = gradingPolicy?.mapToDomain(),
            hasScheduledContent = hasScheduledContent ?: false,
            sectionScores = sectionScores?.map { it.mapToDomain() } ?: emptyList(),
            studioUrl = studioUrl ?: "",
            username = username ?: "",
            userHasPassingGrade = userHasPassingGrade ?: false,
            verificationData = verificationData?.mapToDomain(),
            disableProgressGraph = disableProgressGraph ?: false,
        )
    }

    fun mapToRoomEntity(courseId: String): CourseProgressEntity {
        return CourseProgressEntity(
            courseId = courseId,
            verifiedMode = verifiedMode.orEmpty(),
            accessExpiration = accessExpiration.orEmpty(),
            certificateData = certificateData?.mapToRoomEntity(),
            completionSummary = completionSummary?.mapToRoomEntity(),
            courseGrade = courseGrade?.mapToRoomEntity(),
            creditCourseRequirements = creditCourseRequirements.orEmpty(),
            end = end.orEmpty(),
            enrollmentMode = enrollmentMode.orEmpty(),
            gradingPolicy = gradingPolicy?.mapToRoomEntity(),
            hasScheduledContent = hasScheduledContent ?: false,
            sectionScores = sectionScores?.map { it.mapToRoomEntity() } ?: emptyList(),
            studioUrl = studioUrl.orEmpty(),
            username = username.orEmpty(),
            userHasPassingGrade = userHasPassingGrade ?: false,
            verificationData = verificationData?.mapToRoomEntity(),
            disableProgressGraph = disableProgressGraph ?: false,
        )
    }
}
