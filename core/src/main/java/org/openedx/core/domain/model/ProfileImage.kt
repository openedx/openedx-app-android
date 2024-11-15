package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProfileImage(
    val imageUrlFull: String,
    val imageUrlLarge: String,
    val imageUrlMedium: String,
    val imageUrlSmall: String,
    val hasImage: Boolean
) : Parcelable
