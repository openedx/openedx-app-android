package com.raccoongang.core.module.db

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
    @ColumnInfo("size")
    val size: Int,
    @ColumnInfo("path")
    val path: String,
    @ColumnInfo("url")
    val url: String,
    @ColumnInfo("type")
    val type: String,
    @ColumnInfo("downloadedState")
    val downloadedState: String,
    @ColumnInfo("progress")
    val progress: Float?
) {

    fun mapToDomain() = DownloadModel(
        id,
        title,
        size,
        path,
        url,
        FileType.valueOf(type),
        DownloadedState.valueOf(downloadedState),
        progress
    )

    companion object {

        fun createFrom(downloadModel: DownloadModel): DownloadModelEntity {
            with(downloadModel) {
                return DownloadModelEntity(
                    id,
                    title,
                    size,
                    path,
                    url,
                    type.name,
                    downloadedState.name,
                    progress
                )
            }
        }

    }

}