package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CourseMode(
    val slug: String?,
    val sku: String?,
    val androidSku: String?,
    val iosSku: String?,
    val minPrice: Int,
) : Parcelable
