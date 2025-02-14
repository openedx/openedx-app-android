package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.BannerImageDb
import org.openedx.core.data.model.room.CourseImageDb
import org.openedx.core.data.model.room.CourseVideoDb
import org.openedx.core.data.model.room.ImageDb
import org.openedx.core.data.model.room.MediaDb

@Parcelize
data class Media(
    val bannerImage: BannerImage? = null,
    val courseImage: CourseImage? = null,
    val courseVideo: CourseVideo? = null,
    val image: Image? = null
) : Parcelable {

    fun mapToEntity() = MediaDb(
        bannerImage = bannerImage?.mapToEntity(),
        courseImage = courseImage?.mapToEntity(),
        courseVideo = courseVideo?.mapToEntity(),
        image = image?.mapToEntity()
    )
}

@Parcelize
data class Image(
    val large: String,
    val raw: String,
    val small: String
) : Parcelable {

    fun mapToEntity() = ImageDb(large, raw, small)
}

@Parcelize
data class CourseVideo(
    val uri: String
) : Parcelable {

    fun mapToEntity() = CourseVideoDb(uri)
}

@Parcelize
data class CourseImage(
    val uri: String,
    val name: String
) : Parcelable {

    fun mapToEntity() = CourseImageDb(uri, name)
}

@Parcelize
data class BannerImage(
    val uri: String,
    val uriAbsolute: String
) : Parcelable {

    fun mapToEntity() = BannerImageDb(uri, uriAbsolute)
}
