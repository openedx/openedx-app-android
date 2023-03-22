package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.ProfileImage

data class ProfileImage(
    @SerializedName("image_url_full")
    val imageUrlFull: String?,
    @SerializedName("image_url_large")
    val imageUrlLarge: String?,
    @SerializedName("image_url_medium")
    val imageUrlMedium: String?,
    @SerializedName("image_url_small")
    val imageUrlSmall: String?,
    @SerializedName("has_image")
    val hasImage: Boolean?,
) {

    fun mapToDomain(): ProfileImage {
        return ProfileImage(
            imageUrlFull = imageUrlFull ?: "",
            imageUrlLarge = imageUrlLarge ?: "",
            imageUrlMedium = imageUrlMedium ?: "",
            imageUrlSmall = imageUrlSmall ?: "",
            hasImage = hasImage ?: false
        )
    }
}
