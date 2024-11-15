package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

data class AnnouncementModel(
    @SerializedName("date")
    val date: String,
    @SerializedName("content")
    val content: String
) {
    fun mapToDomain() = org.openedx.core.domain.model.AnnouncementModel(
        date,
        content
    )
}
