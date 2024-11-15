package org.openedx.core.module.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_model")
data class DownloadModelEntity(
    @PrimaryKey
    @ColumnInfo("id")
    val id: String,
    @ColumnInfo("title")
    val title: String,
    @ColumnInfo("courseId")
    val courseId: String,
    @ColumnInfo("size")
    val size: Long,
    @ColumnInfo("path")
    val path: String,
    @ColumnInfo("url")
    val url: String,
    @ColumnInfo("type")
    val type: String,
    @ColumnInfo("downloadedState")
    val downloadedState: String,
    @ColumnInfo("lastModified")
    val lastModified: String?
) {

    fun mapToDomain() = DownloadModel(
        id,
        title,
        courseId,
        size,
        path,
        url,
        FileType.valueOf(type),
        DownloadedState.valueOf(downloadedState),
        lastModified
    )

    companion object {

        fun createFrom(downloadModel: DownloadModel): DownloadModelEntity {
            with(downloadModel) {
                return DownloadModelEntity(
                    id,
                    title,
                    courseId,
                    size,
                    path,
                    url,
                    type.name,
                    downloadedState.name,
                    lastModified
                )
            }
        }
    }
}
