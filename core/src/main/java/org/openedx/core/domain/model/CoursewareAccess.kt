package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.discovery.CoursewareAccessDb

@Parcelize
data class CoursewareAccess(
    val hasAccess: Boolean,
    val errorCode: String,
    val developerMessage: String,
    val userMessage: String,
    val additionalContextUserMessage: String,
    val userFragment: String
) : Parcelable {

    fun mapToEntity() = CoursewareAccessDb(
        hasAccess = hasAccess,
        errorCode = errorCode,
        developerMessage = developerMessage,
        userMessage = userMessage,
        additionalContextUserMessage = additionalContextUserMessage,
        userFragment = userFragment
    )
}
