package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.CourseComponentStatus

data class CourseComponentStatus(
    @SerializedName("last_visited_block_id")
    var lastVisitedBlockId: String?,
) {

    fun mapToDomain(): CourseComponentStatus {
        return CourseComponentStatus(
            lastVisitedBlockId = lastVisitedBlockId ?: ""
        )
    }
}