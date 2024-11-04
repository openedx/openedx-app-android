package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoursewareAccess(
    val hasAccess: Boolean,
    val errorCode: String,
    val developerMessage: String,
    val userMessage: String,
    val additionalContextUserMessage: String,
    val userFragment: String
) : Parcelable
