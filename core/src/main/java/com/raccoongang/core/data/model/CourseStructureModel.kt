package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.CourseStructure

data class CourseStructureModel(
    @SerializedName("root")
    val root: String,
    @SerializedName("blocks")
    val blockData: Map<String, Block>
) {
    fun mapToDomain(): CourseStructure {
        return CourseStructure(
            root = root,
            blockData = blockData.map {
                it.key to it.value.mapToDomain()
            }.toMap()
        )
    }
}
