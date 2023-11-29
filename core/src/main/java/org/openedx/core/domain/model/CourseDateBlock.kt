package org.openedx.core.domain.model

import org.openedx.core.data.model.DateType
import org.openedx.core.presentation.course.CourseDatesBadge
import java.util.Date

data class CourseDateBlock(
    val title: String = "",
    val description: String = "",
    val link: String = "",
    val blockId: String = "",
    val learnerHasAccess: Boolean = false,
    val complete: Boolean = false,
    val date: Date?,
    val dateType: DateType = DateType.NONE,
    val assignmentType: String? = "",
    var dateBlockBadge: CourseDatesBadge = CourseDatesBadge.BLANK,
) {
    companion object {
        fun getTodayDateBlock() =
            CourseDateBlock(
                date = Date(),
                dateType = DateType.TODAY_DATE,
                dateBlockBadge = CourseDatesBadge.TODAY
            )
    }
}
