package org.openedx.core.domain.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.DateType
import org.openedx.core.presentation.course.CourseDatesBadge
import java.util.Date

data class CourseDateBlock(
    val title: String = "",
    val description: String = "",
    val link: String = "",
    @SerializedName("first_component_block_id")
    val blockId: String = "",
    @SerializedName("learner_has_access")
    val learnerHasAccess: Boolean = false,
    val complete: Boolean = false,
    val date: Date?,
    @SerializedName("date_type")
    val dateType: DateType = DateType.NONE,
    @SerializedName("assignment_type")
    val assignmentType: String = "",
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
