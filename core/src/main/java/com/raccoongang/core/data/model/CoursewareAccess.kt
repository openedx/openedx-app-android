package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.data.model.room.discovery.CoursewareAccessDb
import com.raccoongang.core.domain.model.CoursewareAccess

data class CoursewareAccess(
    @SerializedName("has_access")
    val hasAccess: Boolean?,
    @SerializedName("error_code")
    val errorCode: String?,
    @SerializedName("developer_message")
    val developerMessage: String?,
    @SerializedName("user_message")
    val userMessage: String?,
    @SerializedName("additional_context_user_message")
    val additionalContextUserMessage: String?,
    @SerializedName("user_fragment")
    val userFragment: String?
) {

    fun mapToDomain(): CoursewareAccess {
        return CoursewareAccess(
            hasAccess = hasAccess ?: false,
            errorCode = errorCode ?: "",
            developerMessage = developerMessage ?: "",
            userMessage = userMessage ?: "",
            additionalContextUserMessage = additionalContextUserMessage ?: "",
            userFragment = userFragment ?: ""
        )
    }

    fun mapToRoomEntity(): CoursewareAccessDb {
        return CoursewareAccessDb(
            hasAccess = hasAccess ?: false,
            errorCode = errorCode ?: "",
            developerMessage = developerMessage ?: "",
            userMessage = userMessage ?: "",
            additionalContextUserMessage = additionalContextUserMessage ?: "",
            userFragment = userFragment ?: ""
        )
    }

}