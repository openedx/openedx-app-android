package org.openedx.core.data.model.room

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
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
    val certificateData: CertificateDataDb?,
    @Embedded(prefix = "completion_")
    val completionSummary: CompletionSummaryDb?,
    @Embedded(prefix = "grade_")
    val courseGrade: CourseGradeDb?,
    @ColumnInfo("creditCourseRequirements")
    val creditCourseRequirements: String,
    @ColumnInfo("end")
    val end: String,
    @ColumnInfo("enrollmentMode")
    val enrollmentMode: String,
    @Embedded(prefix = "grading_")
    val gradingPolicy: GradingPolicyDb?,
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
    val verificationData: VerificationDataDb?,
    @ColumnInfo("disableProgressGraph")
    val disableProgressGraph: Boolean,
) {
    fun mapToDomain(): CourseProgress {
        return CourseProgress(
            verifiedMode = verifiedMode,
            accessExpiration = accessExpiration,
            certificateData = certificateData?.mapToDomain(),
            completionSummary = completionSummary?.mapToDomain(),
            courseGrade = courseGrade?.mapToDomain(),
            creditCourseRequirements = creditCourseRequirements,
            end = end,
            enrollmentMode = enrollmentMode,
            gradingPolicy = gradingPolicy?.mapToDomain(),
            hasScheduledContent = hasScheduledContent,
            sectionScores = sectionScores.map { it.mapToDomain() },
            studioUrl = studioUrl,
            username = username,
            userHasPassingGrade = userHasPassingGrade,
            verificationData = verificationData?.mapToDomain(),
            disableProgressGraph = disableProgressGraph,
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
) {
    fun mapToDomain() = CourseProgress.CertificateData(
        certStatus = certStatus,
        certWebViewUrl = certWebViewUrl,
        downloadUrl = downloadUrl,
        certificateAvailableDate = certificateAvailableDate
    )
}

data class CompletionSummaryDb(
    @ColumnInfo("completeCount")
    val completeCount: Int,
    @ColumnInfo("incompleteCount")
    val incompleteCount: Int,
    @ColumnInfo("lockedCount")
    val lockedCount: Int
) {
    fun mapToDomain() = CourseProgress.CompletionSummary(
        completeCount = completeCount,
        incompleteCount = incompleteCount,
        lockedCount = lockedCount
    )
}

data class CourseGradeDb(
    @ColumnInfo("letterGrade")
    val letterGrade: String,
    @ColumnInfo("percent")
    val percent: Double,
    @ColumnInfo("isPassing")
    val isPassing: Boolean
) {
    fun mapToDomain() = CourseProgress.CourseGrade(
        letterGrade = letterGrade,
        percent = percent,
        isPassing = isPassing
    )
}

data class GradingPolicyDb(
    @ColumnInfo("assignmentPolicies")
    val assignmentPolicies: List<AssignmentPolicyDb>,
    @ColumnInfo("gradeRange")
    val gradeRange: Map<String, Float>,
    @ColumnInfo("assignmentColors")
    val assignmentColors: List<String>
) {
    fun mapToDomain() = CourseProgress.GradingPolicy(
        assignmentPolicies = assignmentPolicies.map { it.mapToDomain() },
        gradeRange = gradeRange,
        assignmentColors = assignmentColors.map { colorString ->
            Color(colorString.toColorInt())
        }
    )

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
    ) {
        fun mapToDomain() = CourseProgress.GradingPolicy.AssignmentPolicy(
            numDroppable = numDroppable,
            numTotal = numTotal,
            shortLabel = shortLabel,
            type = type,
            weight = weight
        )
    }
}

data class SectionScoreDb(
    @ColumnInfo("displayName")
    val displayName: String,
    @ColumnInfo("subsections")
    val subsections: List<SubsectionDb>
) {
    fun mapToDomain() = CourseProgress.SectionScore(
        displayName = displayName,
        subsections = subsections.map { it.mapToDomain() }
    )

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
        val numPointsEarned: Float,
        @ColumnInfo("numPointsPossible")
        val numPointsPossible: Float,
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
        fun mapToDomain() = CourseProgress.SectionScore.Subsection(
            assignmentType = assignmentType,
            blockKey = blockKey,
            displayName = displayName,
            hasGradedAssignment = hasGradedAssignment,
            override = override,
            learnerHasAccess = learnerHasAccess,
            numPointsEarned = numPointsEarned,
            numPointsPossible = numPointsPossible,
            percentGraded = percentGraded,
            problemScores = problemScores.map { it.mapToDomain() },
            showCorrectness = showCorrectness,
            showGrades = showGrades,
            url = url
        )

        data class ProblemScoreDb(
            @ColumnInfo("earned")
            val earned: Double,
            @ColumnInfo("possible")
            val possible: Double
        ) {
            fun mapToDomain() = CourseProgress.SectionScore.Subsection.ProblemScore(
                earned = earned,
                possible = possible
            )
        }
    }
}

data class VerificationDataDb(
    @ColumnInfo("link")
    val link: String,
    @ColumnInfo("status")
    val status: String,
    @ColumnInfo("statusDate")
    val statusDate: String
) {
    fun mapToDomain() = CourseProgress.VerificationData(
        link = link,
        status = status,
        statusDate = statusDate
    )
}
