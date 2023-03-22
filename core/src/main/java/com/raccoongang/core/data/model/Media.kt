package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.data.model.room.discovery.*
import com.raccoongang.core.domain.model.Media

data class Media(
    @SerializedName("banner_image")
    val bannerImage: BannerImage?,
    @SerializedName("course_image")
    val courseImage: CourseImage?,
    @SerializedName("course_video")
    val courseVideo: CourseVideo?,
    @SerializedName("image")
    val image: Image?,
) {

    fun mapToDomain(): com.raccoongang.core.domain.model.Media {
        return Media(
            bannerImage = bannerImage?.mapToDomain(),
            courseImage = courseImage?.mapToDomain(),
            courseVideo = courseVideo?.mapToDomain(),
            image = image?.mapToDomain()
        )
    }

}

data class Image(
    @SerializedName("large")
    val large: String?,
    @SerializedName("raw")
    val raw: String?,
    @SerializedName("small")
    val small: String?,
) {
    fun mapToDomain(): com.raccoongang.core.domain.model.Image {
        return com.raccoongang.core.domain.model.Image(
            large = large ?: "",
            raw = raw ?: "",
            small = small ?: ""
        )
    }
}

data class CourseVideo(
    @SerializedName("uri")
    val uri: String?,
) {
    fun mapToDomain(): com.raccoongang.core.domain.model.CourseVideo {
        return com.raccoongang.core.domain.model.CourseVideo(
            uri = uri ?: ""
        )
    }
}

data class CourseImage(
    @SerializedName("uri")
    val uri: String?,
    @SerializedName("name")
    val name: String?
) {
    fun mapToDomain(): com.raccoongang.core.domain.model.CourseImage {
        return com.raccoongang.core.domain.model.CourseImage(
            uri = uri ?: "",
            name = name ?: ""
        )
    }
}

data class BannerImage(
    @SerializedName("uri")
    val uri: String?,
    @SerializedName("uri_absolute")
    val uriAbsolute: String?,
) {
    fun mapToDomain(): com.raccoongang.core.domain.model.BannerImage {
        return com.raccoongang.core.domain.model.BannerImage(
            uri = uri ?: "",
            uriAbsolute = uriAbsolute ?: ""
        )
    }
}