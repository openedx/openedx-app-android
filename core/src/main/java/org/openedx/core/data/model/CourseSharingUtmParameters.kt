package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.CourseSharingUtmParametersDb
import org.openedx.core.domain.model.CourseSharingUtmParameters

data class CourseSharingUtmParameters(
    @SerializedName("facebook")
    val facebook: String?,
    @SerializedName("twitter")
    val twitter: String?
) {
    fun mapToDomain(): CourseSharingUtmParameters {
        return CourseSharingUtmParameters(
            facebook = facebook ?: "",
            twitter = twitter ?: ""
        )
    }

    fun mapToRoomEntity() = CourseSharingUtmParametersDb(
        facebook = facebook ?: "",
        twitter = twitter ?: ""
    )
}
