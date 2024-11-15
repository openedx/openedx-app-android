package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CourseSharingUtmParameters(
    val facebook: String,
    val twitter: String
) : Parcelable
