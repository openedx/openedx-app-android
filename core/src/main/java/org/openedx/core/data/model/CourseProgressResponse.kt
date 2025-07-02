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
    @SerializedName("assignment_colors") val assignmentColors: List<String>?
) {
    data class CertificateData(
        @SerializedName("cert_status") val certStatus: String?,
        @SerializedName("cert_web_view_url") val certWebViewUrl: String?,
        @SerializedName("download_url") val downloadUrl: String?,
        @SerializedName("certificate_available_date") val certificateAvailableDate: String?
    )

    data class CompletionSummary(
        @SerializedName("complete_count") val completeCount: Int?,
        @SerializedName("incomplete_count") val incompleteCount: Int?,
        @SerializedName("locked_count") val lockedCount: Int?
    )

    data class CourseGrade(
        @SerializedName("letter_grade") val letterGrade: String?,
        @SerializedName("percent") val percent: Double?,
        @SerializedName("is_passing") val isPassing: Boolean?
    )

    data class GradingPolicy(
        @SerializedName("assignment_policies") val assignmentPolicies: List<AssignmentPolicy>?,
        @SerializedName("grade_range") val gradeRange: Map<String, Float>?
    ) {
        data class AssignmentPolicy(
            @SerializedName("num_droppable") val numDroppable: Int?,
            @SerializedName("num_total") val numTotal: Int?,
            @SerializedName("short_label") val shortLabel: String?,
            @SerializedName("type") val type: String?,
            @SerializedName("weight") val weight: Double?
        )
    }

    data class SectionScore(
        @SerializedName("display_name") val displayName: String?,
        @SerializedName("subsections") val subsections: List<Subsection>?
    ) {
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
            data class ProblemScore(
                @SerializedName("earned") val earned: Double?,
                @SerializedName("possible") val possible: Double?
            )
        }
    }

    data class VerificationData(
        @SerializedName("link") val link: String?,
        @SerializedName("status") val status: String?,
        @SerializedName("status_date") val statusDate: String?
    )

    @Suppress("LongMethod")
    fun mapToDomain(): CourseProgress {
        return CourseProgress(
            verifiedMode = verifiedMode ?: "",
            accessExpiration = accessExpiration ?: "",
            certificateData = CourseProgress.CertificateData(
                certStatus = certificateData?.certStatus ?: "",
                certWebViewUrl = certificateData?.certWebViewUrl ?: "",
                downloadUrl = certificateData?.downloadUrl ?: "",
                certificateAvailableDate = certificateData?.certificateAvailableDate ?: ""
            ),
            completionSummary = CourseProgress.CompletionSummary(
                completeCount = completionSummary?.completeCount ?: 0,
                incompleteCount = completionSummary?.incompleteCount ?: 0,
                lockedCount = completionSummary?.lockedCount ?: 0
            ),
            courseGrade = CourseProgress.CourseGrade(
                letterGrade = courseGrade?.letterGrade ?: "",
                percent = courseGrade?.percent ?: 0.0,
                isPassing = courseGrade?.isPassing ?: false
            ),
            creditCourseRequirements = creditCourseRequirements ?: "",
            end = end ?: "",
            enrollmentMode = enrollmentMode ?: "",
            gradingPolicy = CourseProgress.GradingPolicy(
                assignmentPolicies = gradingPolicy?.assignmentPolicies?.map {
                    CourseProgress.GradingPolicy.AssignmentPolicy(
                        numDroppable = it.numDroppable ?: 0,
                        numTotal = it.numTotal ?: 0,
                        shortLabel = it.shortLabel ?: "",
                        type = it.type ?: "",
                        weight = it.weight ?: 0.0
                    )
                } ?: emptyList(),
                gradeRange = gradingPolicy?.gradeRange ?: emptyMap()
            ),
            hasScheduledContent = hasScheduledContent ?: false,
            sectionScores = sectionScores?.map { section ->
                CourseProgress.SectionScore(
                    displayName = section.displayName ?: "",
                    subsections = section.subsections?.map { subsection ->
                        CourseProgress.SectionScore.Subsection(
                            assignmentType = subsection.assignmentType ?: "",
                            blockKey = subsection.blockKey ?: "",
                            displayName = subsection.displayName ?: "",
                            hasGradedAssignment = subsection.hasGradedAssignment ?: false,
                            override = subsection.override ?: "",
                            learnerHasAccess = subsection.learnerHasAccess ?: false,
                            numPointsEarned = subsection.numPointsEarned ?: 0f,
                            numPointsPossible = subsection.numPointsPossible ?: 0f,
                            percentGraded = subsection.percentGraded ?: 0.0,
                            problemScores = subsection.problemScores?.map { problemScore ->
                                CourseProgress.SectionScore.Subsection.ProblemScore(
                                    earned = problemScore.earned ?: 0.0,
                                    possible = problemScore.possible ?: 0.0
                                )
                            } ?: emptyList(),
                            showCorrectness = subsection.showCorrectness ?: "",
                            showGrades = subsection.showGrades ?: false,
                            url = subsection.url ?: ""
                        )
                    } ?: emptyList()
                )
            } ?: emptyList(),
            studioUrl = studioUrl ?: "",
            username = username ?: "",
            userHasPassingGrade = userHasPassingGrade ?: false,
            verificationData = CourseProgress.VerificationData(
                link = verificationData?.link ?: "",
                status = verificationData?.status ?: "",
                statusDate = verificationData?.statusDate ?: ""
            ),
            disableProgressGraph = disableProgressGraph ?: false,
            assignmentColors = assignmentColors?.map { colorString ->
                Color(colorString.toColorInt())
            } ?: listOf()
        )
    }

    @Suppress("LongMethod, CyclomaticComplexMethod")
    fun mapToRoomEntity(courseId: String): CourseProgressEntity {
        return CourseProgressEntity(
            courseId = courseId,
            verifiedMode = verifiedMode ?: "",
            accessExpiration = accessExpiration ?: "",
            certificateData = CertificateDataDb(
                certStatus = certificateData?.certStatus ?: "",
                certWebViewUrl = certificateData?.certWebViewUrl ?: "",
                downloadUrl = certificateData?.downloadUrl ?: "",
                certificateAvailableDate = certificateData?.certificateAvailableDate ?: ""
            ),
            completionSummary = CompletionSummaryDb(
                completeCount = completionSummary?.completeCount ?: 0,
                incompleteCount = completionSummary?.incompleteCount ?: 0,
                lockedCount = completionSummary?.lockedCount ?: 0
            ),
            courseGrade = CourseGradeDb(
                letterGrade = courseGrade?.letterGrade ?: "",
                percent = courseGrade?.percent ?: 0.0,
                isPassing = courseGrade?.isPassing ?: false
            ),
            creditCourseRequirements = creditCourseRequirements ?: "",
            end = end ?: "",
            enrollmentMode = enrollmentMode ?: "",
            gradingPolicy = GradingPolicyDb(
                assignmentPolicies = gradingPolicy?.assignmentPolicies?.map {
                    GradingPolicyDb.AssignmentPolicyDb(
                        numDroppable = it.numDroppable ?: 0,
                        numTotal = it.numTotal ?: 0,
                        shortLabel = it.shortLabel ?: "",
                        type = it.type ?: "",
                        weight = it.weight ?: 0.0
                    )
                } ?: emptyList(),
                gradeRange = gradingPolicy?.gradeRange ?: emptyMap()
            ),
            hasScheduledContent = hasScheduledContent ?: false,
            sectionScores = sectionScores?.map { section ->
                SectionScoreDb(
                    displayName = section.displayName ?: "",
                    subsections = section.subsections?.map { subsection ->
                        SectionScoreDb.SubsectionDb(
                            assignmentType = subsection.assignmentType ?: "",
                            blockKey = subsection.blockKey ?: "",
                            displayName = subsection.displayName ?: "",
                            hasGradedAssignment = subsection.hasGradedAssignment ?: false,
                            override = subsection.override ?: "",
                            learnerHasAccess = subsection.learnerHasAccess ?: false,
                            numPointsEarned = subsection.numPointsEarned ?: 0f,
                            numPointsPossible = subsection.numPointsPossible ?: 0f,
                            percentGraded = subsection.percentGraded ?: 0.0,
                            problemScores = subsection.problemScores?.map { problemScore ->
                                SectionScoreDb.SubsectionDb.ProblemScoreDb(
                                    earned = problemScore.earned ?: 0.0,
                                    possible = problemScore.possible ?: 0.0
                                )
                            } ?: emptyList(),
                            showCorrectness = subsection.showCorrectness ?: "",
                            showGrades = subsection.showGrades ?: false,
                            url = subsection.url ?: ""
                        )
                    } ?: emptyList()
                )
            } ?: emptyList(),
            studioUrl = studioUrl ?: "",
            username = username ?: "",
            userHasPassingGrade = userHasPassingGrade ?: false,
            verificationData = org.openedx.core.data.model.room.VerificationDataDb(
                link = verificationData?.link ?: "",
                status = verificationData?.status ?: "",
                statusDate = verificationData?.statusDate ?: ""
            ),
            disableProgressGraph = disableProgressGraph ?: false,
            assignmentColors = assignmentColors,
        )
    }
}
