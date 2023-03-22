package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName

data class AnnouncementModel(
    @SerializedName("date")
    val date: String,
    @SerializedName("content")
    val content: String
) {
    fun mapToDomain() = com.raccoongang.core.domain.model.AnnouncementModel(
        date, content
    )
}