package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.presentation.course.CourseDatesBadge
import org.openedx.core.utils.TimeUtils
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
    fun mapToDomain(): LinkedHashMap<String, ArrayList<DomainCourseDateBlock>> {
        var courseDatesDomain = organiseCourseDatesInBlock()
        if (isContainToday().not()) {
            // Adding today's date block manually if not present in the date
            val todayBlock = DomainCourseDateBlock.getTodayDateBlock()
            courseDatesDomain[TimeUtils.formatDate(TimeUtils.FORMAT_DATE, todayBlock.date)] =
                arrayListOf(todayBlock)
        }
        // Sort the map entries date keys wise
        courseDatesDomain = LinkedHashMap(courseDatesDomain.toSortedMap(compareBy {
            TimeUtils.stringToDate(TimeUtils.FORMAT_DATE, it)
        }))
        reviseDateBlockBadge(courseDatesDomain)
        return courseDatesDomain
    }

    /**
     * Map the date blocks according to dates and stack all the blocks of same date against one key
     */
    private fun organiseCourseDatesInBlock(): LinkedHashMap<String, ArrayList<DomainCourseDateBlock>> {
        val courseDates =
            LinkedHashMap<String, ArrayList<DomainCourseDateBlock>>()
        courseDateBlocks.forEach { item ->
            val key =
                TimeUtils.formatDate(TimeUtils.FORMAT_DATE, TimeUtils.iso8601ToDate(item.date))
            val dateBlock = DomainCourseDateBlock(
                title = item.title,
                description = item.description,
                link = item.link,
                blockId = item.blockId,
                date = TimeUtils.iso8601ToDate(item.date),
                complete = item.complete,
                learnerHasAccess = item.learnerHasAccess,
                dateType = item.dateType,
                dateBlockBadge = CourseDatesBadge.BLANK,
                assignmentType = item.assignmentType
            )
            if (courseDates.containsKey(key)) {
                (courseDates[key] as ArrayList).add(dateBlock)
            } else {
                courseDates[key] = arrayListOf(dateBlock)
            }
        }
        return courseDates
    }

    /**
     * Utility method to check that list contains today's date block or not.
     */
    private fun isContainToday(): Boolean {
        val today = Date()
        return courseDateBlocks.any { blockDate ->
            TimeUtils.iso8601ToDate(blockDate.date) == today
        }
    }

    /**
     * Set the Date Block Badge based on the date block data
     */
    private fun reviseDateBlockBadge(courseDatesDomain: LinkedHashMap<String, ArrayList<DomainCourseDateBlock>>) {
        var dueNextCount = 0
        courseDatesDomain.keys.forEach { key ->
            courseDatesDomain[key]?.forEach { item ->
                var dateBlockTag: CourseDatesBadge = getDateTypeBadge(item)
                //Setting Due Next only for first occurrence
                if (dateBlockTag == CourseDatesBadge.DUE_NEXT) {
                    if (dueNextCount == 0)
                        dueNextCount += 1
                    else
                        dateBlockTag = CourseDatesBadge.BLANK
                }
                item.dateBlockBadge = dateBlockTag
            }
        }
    }

    /**
     * Return Pill/Badge type of date block based on data
     */
    private fun getDateTypeBadge(item: DomainCourseDateBlock): CourseDatesBadge {
        val dateBlockTag: CourseDatesBadge
        val currentDate = Date()
        val componentDate: Date = item.date ?: return CourseDatesBadge.BLANK
        when (item.dateType) {
            DateType.TODAY_DATE -> {
                dateBlockTag = CourseDatesBadge.TODAY
            }

            DateType.COURSE_START_DATE,
            DateType.COURSE_END_DATE -> {
                dateBlockTag = CourseDatesBadge.BLANK
            }

            DateType.ASSIGNMENT_DUE_DATE -> {
                when {
                    item.complete -> {
                        dateBlockTag = CourseDatesBadge.COMPLETED
                    }

                    item.learnerHasAccess -> {
                        dateBlockTag = when {
                            item.link.isEmpty() -> {
                                CourseDatesBadge.NOT_YET_RELEASED
                            }

                            TimeUtils.isDueDate(currentDate, componentDate) -> {
                                CourseDatesBadge.DUE_NEXT
                            }

                            TimeUtils.isDatePassed(currentDate, componentDate) -> {
                                CourseDatesBadge.PAST_DUE
                            }

                            else -> {
                                CourseDatesBadge.BLANK
                            }
                        }
                    }

                    else -> {
                        dateBlockTag = CourseDatesBadge.VERIFIED_ONLY
                    }
                }
            }

            DateType.COURSE_EXPIRED_DATE -> {
                dateBlockTag = CourseDatesBadge.COURSE_EXPIRED_DATE
            }

            else -> {
                // dateBlockTag is BLANK for all other cases
                // DateTypes.CERTIFICATE_AVAILABLE_DATE,
                // DateTypes.VERIFIED_UPGRADE_DEADLINE,
                // DateTypes.VERIFICATION_DEADLINE_DATE
                dateBlockTag = CourseDatesBadge.BLANK
            }
        }
        return dateBlockTag
    }
}
