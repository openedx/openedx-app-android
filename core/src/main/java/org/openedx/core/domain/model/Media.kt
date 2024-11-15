package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Media(
    val bannerImage: BannerImage? = null,
    val courseImage: CourseImage? = null,
    val courseVideo: CourseVideo? = null,
    val image: Image? = null
) : Parcelable

@Parcelize
data class Image(
    val large: String,
    val raw: String,
    val small: String
) : Parcelable

@Parcelize
data class CourseVideo(
    val uri: String
) : Parcelable

@Parcelize
data class CourseImage(
    val uri: String,
    val name: String
) : Parcelable

@Parcelize
data class BannerImage(
    val uri: String,
    val uriAbsolute: String
) : Parcelable
