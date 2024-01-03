package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.utils.TimeUtils
import org.openedx.core.utils.addDays
import org.openedx.core.utils.isToday
import org.openedx.core.domain.model.CourseDateBlock as DomainCourseDateBlock

data class CourseDates(
    @SerializedName("dates_banner_info")
    val datesBannerInfo: CourseDatesBannerInfo?,
    @SerializedName("course_date_blocks")
    val courseDateBlocks: List<CourseDateBlock>,
    @SerializedName("missed_deadlines")
    val missedDeadlines: Boolean = false,
    @SerializedName("missed_gated_content")
    val missedGatedContent: Boolean = false,
    @SerializedName("learner_is_full_access")
    val learnerIsFullAccess: Boolean = false,
    @SerializedName("user_timezone")
    val userTimezone: String? = "",
    @SerializedName("verified_upgrade_link")
    val verifiedUpgradeLink: String? = "",
) {
    fun getStructuredCourseDates(): LinkedHashMap<DatesSection, List<DomainCourseDateBlock>> {
        val currentDate = TimeUtils.getCurrentDate()
        val courseDatesResponse: LinkedHashMap<DatesSection, List<DomainCourseDateBlock>> =
            LinkedHashMap()
        val datesList = mapToDomain()
        // Added dates for completed, past due, today, this week, next week and upcoming
        courseDatesResponse[DatesSection.COMPLETED] =
            datesList.filter { it.isCompleted() }.also { datesList.removeAll(it) }

        courseDatesResponse[DatesSection.PAST_DUE] =
            datesList.filter { currentDate.after(it.date) }.also { datesList.removeAll(it) }

        courseDatesResponse[DatesSection.TODAY] =
            datesList.filter { it.date != null && it.date.isToday() }
                .also { datesList.removeAll(it) }

        // for current week except today
        courseDatesResponse[DatesSection.THIS_WEEK] = datesList.filter {
            it.date != null && it.date.after(currentDate) &&
                    it.date.before(currentDate.addDays(8))
        }.also { datesList.removeAll(it) }

        // for coming week
        courseDatesResponse[DatesSection.NEXT_WEEK] = datesList.filter {
            it.date != null &&
                    it.date.after(currentDate.addDays(7)) &&
                    it.date.before(currentDate.addDays(15))
        }.also { datesList.removeAll(it) }

        // for upcoming
        courseDatesResponse[DatesSection.UPCOMING] = datesList.filter {
            it.date != null && it.date.after(currentDate.addDays(14))
        }.also { datesList.removeAll(it) }

        return courseDatesResponse
    }

    private fun mapToDomain(): MutableList<DomainCourseDateBlock> {
        return courseDateBlocks.map { item ->
            DomainCourseDateBlock(
                title = item.title,
                description = item.description,
                link = item.link,
                blockId = item.blockId,
                date = TimeUtils.iso8601ToDate(item.date),
                complete = item.complete,
                learnerHasAccess = item.learnerHasAccess,
                dateType = item.dateType,
                assignmentType = item.assignmentType
            )
        }.sortedBy { it.date }.filter { it.date != null }.toMutableList()
    }
}
