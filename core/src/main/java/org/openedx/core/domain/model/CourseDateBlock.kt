package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.DateType
import java.util.Date

@Parcelize
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
) : Parcelable {
    fun isCompleted(): Boolean {
        val dateTypeInSet = dateType in setOf(
            DateType.COURSE_START_DATE,
            DateType.COURSE_END_DATE,
            DateType.CERTIFICATE_AVAILABLE_DATE,
            DateType.VERIFIED_UPGRADE_DEADLINE,
            DateType.VERIFICATION_DEADLINE_DATE
        )
        return complete || (dateTypeInSet && date.before(Date()))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CourseDateBlock

        if (blockId != other.blockId) return false
        if (date != other.date) return false
        if (assignmentType != other.assignmentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blockId.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + (assignmentType?.hashCode() ?: 0)
        return result
    }
}
