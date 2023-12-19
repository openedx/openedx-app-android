package org.openedx.core.domain.model

import org.openedx.core.data.model.DateType
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
) {
    fun isCompleted(): Boolean {
        return complete || (dateType in setOf(
            DateType.COURSE_START_DATE,
            DateType.COURSE_END_DATE,
            DateType.CERTIFICATE_AVAILABLE_DATE,
            DateType.VERIFICATION_DEADLINE_DATE
        ) && date?.before(Date()) == true)
    }
}
