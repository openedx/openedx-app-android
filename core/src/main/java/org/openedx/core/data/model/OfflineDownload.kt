package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.OfflineDownloadDb
import org.openedx.core.domain.model.OfflineDownload

data class OfflineDownload(
    @SerializedName("file_url")
    var fileUrl: String?,
    @SerializedName("last_modified")
    var lastModified: String?,
    @SerializedName("file_size")
    var fileSize: Long?,
) {
    fun mapToDomain() = OfflineDownload(
        fileUrl = fileUrl ?: "",
        lastModified = lastModified,
        fileSize = fileSize ?: 0
    )

    fun mapToRoomEntity() = OfflineDownloadDb(
        fileUrl = fileUrl ?: "",
        lastModified = lastModified,
        fileSize = fileSize ?: 0
    )
}
