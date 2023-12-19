package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.utils.TimeUtils
import java.util.Collections
import java.util.Date
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
        val currentDate = Date()
        val courseDatesResponse: LinkedHashMap<DatesSection, List<DomainCourseDateBlock>> =
            LinkedHashMap()
        val datesList = mapToDomains()
        // Added dates for completed, past due, today, this week, next week and upcoming
        courseDatesResponse[DatesSection.COMPLETED] = datesList.filter { it.isCompleted() }

        courseDatesResponse[DatesSection.PAST_DUE] =
            datesList.filter { it.date != null && it.date < currentDate && !it.isCompleted() }

        courseDatesResponse[DatesSection.TODAY] = datesList.filter {
            it.date != null && TimeUtils.areDatesSame(it.date, currentDate) && !it.isCompleted()
        }
        // for current week except today
        courseDatesResponse[DatesSection.THIS_WEEK] = datesList.filter {
            it.date != null && !it.isCompleted() &&
                    TimeUtils.areDatesSame(it.date, currentDate).not() &&
                    it.date > currentDate && it.date < TimeUtils.addDays(currentDate, 7)
        }
        // for coming week
        courseDatesResponse[DatesSection.NEXT_WEEK] = datesList.filter {
            it.date != null && !it.isCompleted() &&
                    it.date > TimeUtils.addDays(currentDate, 7) &&
                    it.date < TimeUtils.addDays(currentDate, 14)
        }
        // for upcoming
        courseDatesResponse[DatesSection.UPCOMING] = datesList.filter {
            it.date != null && it.date > TimeUtils.addDays(currentDate, 14) && !it.isCompleted()
        }

        return courseDatesResponse
    }

    private fun mapToDomains(): ArrayList<DomainCourseDateBlock> {
        val courseDates = ArrayList<DomainCourseDateBlock>()
        courseDateBlocks.forEach { item ->
            val dateBlock = DomainCourseDateBlock(
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
            courseDates.add(dateBlock)
        }
        Collections.sort(courseDates, Comparator.comparing(DomainCourseDateBlock::date))
        return courseDates
    }
}
