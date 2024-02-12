package org.openedx.core.domain.model

import org.openedx.core.data.model.DateType
import org.openedx.core.utils.isTimeLessThan24Hours
import org.openedx.core.utils.isToday
import java.util.Date

data class CourseDateBlock(
    val title: String = "",
    val description: String = "",
    val link: String = "",
    val blockId: String = "",
    val learnerHasAccess: Boolean = false,
    val complete: Boolean = false,
    val date: Date,
    val dateType: DateType = DateType.NONE,
    val assignmentType: String? = "",
) {
    fun isCompleted(): Boolean {
        return complete || (dateType in setOf(
            DateType.COURSE_START_DATE,
            DateType.COURSE_END_DATE,
            DateType.CERTIFICATE_AVAILABLE_DATE,
            DateType.VERIFIED_UPGRADE_DEADLINE,
            DateType.VERIFICATION_DEADLINE_DATE,
        ) && date.before(Date()))
    }

    fun isTimeDifferenceLessThan24Hours(): Boolean {
        return (date.isToday() && date.before(Date())) || date.isTimeLessThan24Hours()
    }
}
