package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.data.model.room.discovery.CourseSharingUtmParametersDb
import com.raccoongang.core.domain.model.CourseSharingUtmParameters

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