package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.Media

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

    fun mapToDomain(): Media {
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
    fun mapToDomain(): org.openedx.core.domain.model.Image {
        return org.openedx.core.domain.model.Image(
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
    fun mapToDomain(): org.openedx.core.domain.model.CourseVideo {
        return org.openedx.core.domain.model.CourseVideo(
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
    fun mapToDomain(): org.openedx.core.domain.model.CourseImage {
        return org.openedx.core.domain.model.CourseImage(
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
    fun mapToDomain(): org.openedx.core.domain.model.BannerImage {
        return org.openedx.core.domain.model.BannerImage(
            uri = uri ?: "",
            uriAbsolute = uriAbsolute ?: ""
        )
    }
}
