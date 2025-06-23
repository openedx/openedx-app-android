package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.domain.model.CourseProgress

@Entity(tableName = "course_progress_table")
data class CourseProgressEntity(
    @PrimaryKey
    @ColumnInfo("courseId")
    val courseId: String,
    @ColumnInfo("verifiedMode")
    val verifiedMode: String,
    @ColumnInfo("accessExpiration")
    val accessExpiration: String,
    @Embedded(prefix = "certificate_")
    val certificateData: CertificateDataDb,
    @Embedded(prefix = "completion_")
    val completionSummary: CompletionSummaryDb,
    @Embedded(prefix = "grade_")
    val courseGrade: CourseGradeDb,
    @ColumnInfo("creditCourseRequirements")
    val creditCourseRequirements: String,
    @ColumnInfo("end")
    val end: String,
    @ColumnInfo("enrollmentMode")
    val enrollmentMode: String,
    @Embedded(prefix = "grading_")
    val gradingPolicy: GradingPolicyDb,
    @ColumnInfo("hasScheduledContent")
    val hasScheduledContent: Boolean,
    @ColumnInfo("sectionScores")
    val sectionScores: List<SectionScoreDb>,
    @ColumnInfo("studioUrl")
    val studioUrl: String,
    @ColumnInfo("username")
    val username: String,
    @ColumnInfo("userHasPassingGrade")
    val userHasPassingGrade: Boolean,
    @Embedded(prefix = "verification_")
    val verificationData: VerificationDataDb,
    @ColumnInfo("disableProgressGraph")
    val disableProgressGraph: Boolean
) {
    fun mapToDomain(): CourseProgress {
        return CourseProgress(
            verifiedMode = verifiedMode,
            accessExpiration = accessExpiration,
            certificateData = CourseProgress.CertificateData(
                certStatus = certificateData.certStatus,
                certWebViewUrl = certificateData.certWebViewUrl,
                downloadUrl = certificateData.downloadUrl,
                certificateAvailableDate = certificateData.certificateAvailableDate
            ),
            completionSummary = CourseProgress.CompletionSummary(
                completeCount = completionSummary.completeCount,
                incompleteCount = completionSummary.incompleteCount,
                lockedCount = completionSummary.lockedCount
            ),
            courseGrade = CourseProgress.CourseGrade(
                letterGrade = courseGrade.letterGrade,
                percent = courseGrade.percent,
                isPassing = courseGrade.isPassing
            ),
            creditCourseRequirements = creditCourseRequirements,
            end = end,
            enrollmentMode = enrollmentMode,
            gradingPolicy = CourseProgress.GradingPolicy(
                assignmentPolicies = gradingPolicy.assignmentPolicies.map {
                    CourseProgress.GradingPolicy.AssignmentPolicy(
                        numDroppable = it.numDroppable,
                        numTotal = it.numTotal,
                        shortLabel = it.shortLabel,
                        type = it.type,
                        weight = it.weight
                    )
                },
                gradeRange = gradingPolicy.gradeRange
            ),
            hasScheduledContent = hasScheduledContent,
            sectionScores = sectionScores.map { section ->
                CourseProgress.SectionScore(
                    displayName = section.displayName,
                    subsections = section.subsections.map { subsection ->
                        CourseProgress.SectionScore.Subsection(
                            assignmentType = subsection.assignmentType,
                            blockKey = subsection.blockKey,
                            displayName = subsection.displayName,
                            hasGradedAssignment = subsection.hasGradedAssignment,
                            override = subsection.override,
                            learnerHasAccess = subsection.learnerHasAccess,
                            numPointsEarned = subsection.numPointsEarned,
                            numPointsPossible = subsection.numPointsPossible,
                            percentGraded = subsection.percentGraded,
                            problemScores = subsection.problemScores.map { problemScore ->
                                CourseProgress.SectionScore.Subsection.ProblemScore(
                                    earned = problemScore.earned,
                                    possible = problemScore.possible
                                )
                            },
                            showCorrectness = subsection.showCorrectness,
                            showGrades = subsection.showGrades,
                            url = subsection.url
                        )
                    }
                )
            },
            studioUrl = studioUrl,
            username = username,
            userHasPassingGrade = userHasPassingGrade,
            verificationData = CourseProgress.VerificationData(
                link = verificationData.link,
                status = verificationData.status,
                statusDate = verificationData.statusDate
            ),
            disableProgressGraph = disableProgressGraph
        )
    }
}

data class CertificateDataDb(
    @ColumnInfo("certStatus")
    val certStatus: String,
    @ColumnInfo("certWebViewUrl")
    val certWebViewUrl: String,
    @ColumnInfo("downloadUrl")
    val downloadUrl: String,
    @ColumnInfo("certificateAvailableDate")
    val certificateAvailableDate: String
)

data class CompletionSummaryDb(
    @ColumnInfo("completeCount")
    val completeCount: Int,
    @ColumnInfo("incompleteCount")
    val incompleteCount: Int,
    @ColumnInfo("lockedCount")
    val lockedCount: Int
)

data class CourseGradeDb(
    @ColumnInfo("letterGrade")
    val letterGrade: String,
    @ColumnInfo("percent")
    val percent: Double,
    @ColumnInfo("isPassing")
    val isPassing: Boolean
)

data class GradingPolicyDb(
    @ColumnInfo("assignmentPolicies")
    val assignmentPolicies: List<AssignmentPolicyDb>,
    @ColumnInfo("gradeRange")
    val gradeRange: Map<String, Float>
) {
    data class AssignmentPolicyDb(
        @ColumnInfo("numDroppable")
        val numDroppable: Int,
        @ColumnInfo("numTotal")
        val numTotal: Int,
        @ColumnInfo("shortLabel")
        val shortLabel: String,
        @ColumnInfo("type")
        val type: String,
        @ColumnInfo("weight")
        val weight: Double
    )
}

data class SectionScoreDb(
    @ColumnInfo("displayName")
    val displayName: String,
    @ColumnInfo("subsections")
    val subsections: List<SubsectionDb>
) {
    data class SubsectionDb(
        @ColumnInfo("assignmentType")
        val assignmentType: String,
        @ColumnInfo("blockKey")
        val blockKey: String,
        @ColumnInfo("displayName")
        val displayName: String,
        @ColumnInfo("hasGradedAssignment")
        val hasGradedAssignment: Boolean,
        @ColumnInfo("override")
        val override: String,
        @ColumnInfo("learnerHasAccess")
        val learnerHasAccess: Boolean,
        @ColumnInfo("numPointsEarned")
        val numPointsEarned: Int,
        @ColumnInfo("numPointsPossible")
        val numPointsPossible: Int,
        @ColumnInfo("percentGraded")
        val percentGraded: Double,
        @ColumnInfo("problemScores")
        val problemScores: List<ProblemScoreDb>,
        @ColumnInfo("showCorrectness")
        val showCorrectness: String,
        @ColumnInfo("showGrades")
        val showGrades: Boolean,
        @ColumnInfo("url")
        val url: String
    ) {
        data class ProblemScoreDb(
            @ColumnInfo("earned")
            val earned: Int,
            @ColumnInfo("possible")
            val possible: Int
        )
    }
}

data class VerificationDataDb(
    @ColumnInfo("link")
    val link: String,
    @ColumnInfo("status")
    val status: String,
    @ColumnInfo("statusDate")
    val statusDate: String
)
