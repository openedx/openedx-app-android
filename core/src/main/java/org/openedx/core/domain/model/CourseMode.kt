package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CourseMode(
    val slug: String?,
    val androidSku: String?,
    var storeSku: String?,
) : Parcelable
