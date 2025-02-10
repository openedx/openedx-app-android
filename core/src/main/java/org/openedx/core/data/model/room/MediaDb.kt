package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import org.openedx.core.domain.model.BannerImage
import org.openedx.core.domain.model.CourseImage
import org.openedx.core.domain.model.CourseVideo
import org.openedx.core.domain.model.Image
import org.openedx.core.domain.model.Media

data class MediaDb(
    @ColumnInfo("bannerImage")
    val bannerImage: BannerImageDb? = null,
    @ColumnInfo("courseImage")
    val courseImage: CourseImageDb? = null,
    @ColumnInfo("courseVideo")
    val courseVideo: CourseVideoDb? = null,
    @ColumnInfo("image")
    val image: ImageDb? = null
) {
    fun mapToDomain() = Media(
        bannerImage = bannerImage?.mapToDomain(),
        courseImage = courseImage?.mapToDomain(),
        courseVideo = courseVideo?.mapToDomain(),
        image = image?.mapToDomain(),
    )

    companion object {
        fun createFrom(media: org.openedx.core.data.model.Media?) =
            if (media == null) {
                MediaDb()
            } else {
                MediaDb(
                    bannerImage = BannerImageDb.createFrom(media.bannerImage),
                    courseImage = CourseImageDb.createFrom(media.courseImage),
                    courseVideo = CourseVideoDb.createFrom(media.courseVideo),
                    image = ImageDb.createFrom(media.image)
                )
            }
    }
}

data class ImageDb(
    @ColumnInfo("large")
    val large: String,
    @ColumnInfo("raw")
    val raw: String,
    @ColumnInfo("small")
    val small: String
) {
    fun mapToDomain() = Image(
        large,
        raw,
        small
    )

    companion object {
        fun createFrom(image: org.openedx.core.data.model.Image?) =
            ImageDb(
                large = image?.large ?: "",
                raw = image?.raw ?: "",
                small = image?.small ?: ""
            )
    }
}

data class CourseVideoDb(
    @ColumnInfo("uri")
    val uri: String
) {
    fun mapToDomain() = CourseVideo(uri)

    companion object {
        fun createFrom(courseVideo: org.openedx.core.data.model.CourseVideo?) =
            CourseVideoDb(uri = courseVideo?.uri ?: "")
    }
}

data class CourseImageDb(
    @ColumnInfo("uri")
    val uri: String,
    @ColumnInfo("name")
    val name: String
) {
    fun mapToDomain() = CourseImage(uri, name)

    companion object {
        fun createFrom(courseImage: org.openedx.core.data.model.CourseImage?) =
            CourseImageDb(
                uri = courseImage?.uri ?: "",
                name = courseImage?.name ?: ""
            )
    }
}

data class BannerImageDb(
    @ColumnInfo("uri")
    val uri: String,
    @ColumnInfo("uriAbsolute")
    val uriAbsolute: String
) {
    fun mapToDomain() = BannerImage(uri, uriAbsolute)

    companion object {

        fun createFrom(bannerImage: org.openedx.core.data.model.BannerImage?) =
            BannerImageDb(
                uri = bannerImage?.uri ?: "",
                uriAbsolute = bannerImage?.uriAbsolute ?: ""
            )
    }
}
