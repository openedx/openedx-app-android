package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_progress_table")
data class VideoProgressEntity(
    @PrimaryKey
    @ColumnInfo("block_id")
    val blockId: String,
    @ColumnInfo("video_url")
    val videoUrl: String,
    @ColumnInfo("video_time")
    val videoTime: Long,
    @ColumnInfo("duration")
    val duration: Long,
)
