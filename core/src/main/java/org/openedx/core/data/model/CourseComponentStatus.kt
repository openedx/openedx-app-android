package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.CourseComponentStatus

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
