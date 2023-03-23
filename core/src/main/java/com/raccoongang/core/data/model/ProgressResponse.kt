package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.CourseProgress
import java.text.DecimalFormat
import kotlin.math.roundToInt

data class ProgressResponse(
    @SerializedName("sections")
    val sections: List<Section>?
) {

    fun mapToDomain(formatter: DecimalFormat): CourseProgress {
        val sections = sections?.map { it.mapToDomain(formatter) } ?: emptyList()

        var earned = 0f
        var total = 0f
        for (section in sections) {
            for (subsection in section.subsections) {
                if (subsection.score.isNotEmpty()) {
                    earned += subsection.earned.toFloatOrNull() ?: 0f
                    total += subsection.total.toFloatOrNull() ?: 0f
                }
            }
        }

        var progress = 0f
        if (earned > 0f && total > 0f) {
            progress = earned / total * 100
        }

        return CourseProgress(sections, progress.roundToInt())
    }

    data class Section(
        @SerializedName("display_name")
        val displayName: String?,
        @SerializedName("subsections")
        val subsections: List<Subsection>?
    ) {
        fun mapToDomain(formatter: DecimalFormat): CourseProgress.Section {
            return CourseProgress.Section(
                displayName ?: "",
                subsections?.map { it.mapToDomain(formatter) } ?: emptyList()
            )
        }
    }

    data class Subsection(
        @SerializedName("earned")
        val earned: Float?,
        @SerializedName("total")
        val total: Float?,
        @SerializedName("percentageString")
        val percentageString: String?,
        @SerializedName("display_name")
        val displayName: String?,
        @SerializedName("score")
        val score: List<Score>?,
        @SerializedName("show_grades")
        val showGrades: Boolean?,
        @SerializedName("graded")
        val graded: Boolean?,
        @SerializedName("grade_type")
        val gradeType: String?
    ) {
        fun mapToDomain(formatter: DecimalFormat): CourseProgress.Subsection {
            return CourseProgress.Subsection(
                formatter.format(earned ?: 0f),
                formatter.format(total ?: 0f),
                percentageString ?: "",
                displayName ?: "",
                score?.map { it.mapToDomain(formatter) } ?: emptyList(),
                showGrades ?: false,
                graded ?: false,
                gradeType ?: ""
            )
        }
    }

    data class Score(
        @SerializedName("earned")
        val earned: Float?,
        @SerializedName("possible")
        val possible: Float?
    ) {
        fun mapToDomain(formatter: DecimalFormat): CourseProgress.Score {
            return CourseProgress.Score(formatter.format(earned ?: 0f), formatter.format(possible ?: 0f))
        }
    }
}
