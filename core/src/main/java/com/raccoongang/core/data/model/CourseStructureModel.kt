package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.data.model.room.CourseStructureEntity
import com.raccoongang.core.data.model.room.MediaDb
import com.raccoongang.core.domain.model.CourseStructure
import com.raccoongang.core.utils.TimeUtils

data class CourseStructureModel(
    @SerializedName("root")
    val root: String,
    @SerializedName("blocks")
    val blockData: Map<String, Block>,
    @SerializedName("id")
    var id: String?,
    @SerializedName("name")
    var name: String?,
    @SerializedName("number")
    var number: String?,
    @SerializedName("org")
    var org: String?,
    @SerializedName("start")
    var start: String?,
    @SerializedName("start_display")
    var startDisplay: String?,
    @SerializedName("start_type")
    var startType: String?,
    @SerializedName("end")
    var end: String?,
    @SerializedName("courseware_access")
    var coursewareAccess: CoursewareAccess?,
    @SerializedName("media")
    var media: Media?,
    @SerializedName("certificate")
    val certificate: Certificate?,
    @SerializedName("is_self_paced")
    var isSelfPaced: Boolean?
) {
    fun mapToDomain(): CourseStructure {
        return CourseStructure(
            root = root,
            blockData = blockData.map {
                it.value.mapToDomain()
            },
            id = id ?: "",
            name = name ?: "",
            number = number ?: "",
            org = org ?: "",
            start = TimeUtils.iso8601ToDate(start ?: ""),
            startDisplay = startDisplay ?: "",
            startType = startType ?: "",
            end = TimeUtils.iso8601ToDate(end ?: ""),
            coursewareAccess = coursewareAccess?.mapToDomain()!!,
            media = media?.mapToDomain(),
            certificate = certificate?.mapToDomain(),
            isSelfPaced = isSelfPaced ?: false
        )
    }

    fun mapToRoomEntity(): CourseStructureEntity {
        return CourseStructureEntity(
            root,
            id = id ?: "",
            name = name ?: "",
            number = number ?: "",
            org = org ?: "",
            start = start ?: "",
            startDisplay = startDisplay ?: "",
            startType = startType ?: "",
            end = end ?: "",
            coursewareAccess = coursewareAccess?.mapToRoomEntity()!!,
            media = MediaDb.createFrom(media),
            certificate = certificate?.mapToRoomEntity(),
            isSelfPaced = isSelfPaced ?: false

        )
    }
}
