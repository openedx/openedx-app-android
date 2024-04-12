package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.ProgressDb

data class Progress(
    @SerializedName("num_points_earned")
    val numPointsEarned: Int?,
    @SerializedName("num_points_possible")
    val numPointsPossible: Int?
) {
    fun mapToDomain(): org.openedx.core.domain.model.Progress {
        return org.openedx.core.domain.model.Progress(
            numPointsEarned = numPointsEarned ?: 0,
            numPointsPossible = numPointsPossible ?: 0
        )
    }

    fun mapToRoomEntity() = ProgressDb(
        numPointsEarned = numPointsEarned ?: 0,
        numPointsPossible = numPointsPossible ?: 0
    )
}