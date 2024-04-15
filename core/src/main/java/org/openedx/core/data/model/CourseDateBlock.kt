package org.openedx.core.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.discovery.CourseDateBlockDb
import org.openedx.core.utils.TimeUtils
import java.util.Date

@Parcelize
data class CourseDateBlock(
    @SerializedName("complete")
    val complete: Boolean = false,
    @SerializedName("date")
    val date: String = "",  // ISO 8601 compliant format
    @SerializedName("assignment_type")
    val assignmentType: String? = "",
    @SerializedName("date_type")
    val dateType: DateType = DateType.NONE,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("learner_has_access")
    val learnerHasAccess: Boolean = false,
    @SerializedName("link")
    val link: String = "",
    @SerializedName("link_text")
    val linkText: String = "",
    @SerializedName("title")
    val title: String = "",
    // component blockId in-case of navigating inside the app for component available in mobile
    @SerializedName("first_component_block_id")
    val blockId: String = "",
): Parcelable {
    fun mapToDomain() = org.openedx.core.domain.model.CourseDateBlock(
        complete = complete,
        date = TimeUtils.iso8601ToDate(date) ?: Date(),
        assignmentType = assignmentType,
        dateType = dateType,
        description = description,
        learnerHasAccess = learnerHasAccess,
        link = link,
        title = title,
        blockId = blockId
    )
    fun mapToRoomEntity() = CourseDateBlockDb(
        complete = complete,
        date = date,
        assignmentType = assignmentType,
        dateType = dateType,
        description = description,
        learnerHasAccess = learnerHasAccess,
        link = link,
        title = title,
        blockId = blockId
    )
}
