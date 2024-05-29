package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.CourseStatusDb
import org.openedx.core.domain.model.CourseStatus

data class CourseStatus(
    @SerializedName("last_visited_module_id")
    val lastVisitedModuleId: String?,
    @SerializedName("last_visited_module_path")
    val lastVisitedModulePath: List<String>?,
    @SerializedName("last_visited_block_id")
    val lastVisitedBlockId: String?,
    @SerializedName("last_visited_unit_display_name")
    val lastVisitedUnitDisplayName: String?,
) {
    fun mapToDomain() = CourseStatus(
        lastVisitedModuleId = lastVisitedModuleId ?: "",
        lastVisitedModulePath = lastVisitedModulePath ?: emptyList(),
        lastVisitedBlockId = lastVisitedBlockId ?: "",
        lastVisitedUnitDisplayName = lastVisitedUnitDisplayName ?: ""
    )

    fun mapToRoomEntity() = CourseStatusDb(
        lastVisitedModuleId = lastVisitedModuleId ?: "",
        lastVisitedModulePath = lastVisitedModulePath ?: emptyList(),
        lastVisitedBlockId = lastVisitedBlockId ?: "",
        lastVisitedUnitDisplayName = lastVisitedUnitDisplayName ?: ""
    )
}
