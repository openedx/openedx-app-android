package com.raccoongang.course.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raccoongang.core.BlockType
import com.raccoongang.core.domain.model.*

@Entity(tableName = "course_blocks_table")
data class BlockDbEntity(
    @ColumnInfo("courseId")
    val courseId: String,
    @PrimaryKey
    @ColumnInfo("id")
    val id: String,
    @ColumnInfo("blockId")
    val blockId: String,
    @ColumnInfo("lmsWebUrl")
    val lmsWebUrl: String,
    @ColumnInfo("legacyWebUrl")
    val legacyWebUrl: String,
    @ColumnInfo("studentViewUrl")
    val studentViewUrl: String,
    @ColumnInfo("type")
    val type: String,
    @ColumnInfo("displayName")
    val displayName: String,
    @ColumnInfo("graded")
    val graded: Boolean,
    @Embedded
    val studentViewData: StudentViewDataDb?,
    @ColumnInfo("studentViewMultiDevice")
    val studentViewMultiDevice: Boolean,
    @Embedded
    val blockCounts: BlockCountsDb,
    @ColumnInfo("descendants")
    val descendants: List<String>,
    @ColumnInfo("completion")
    val completion: Double
) {
    fun mapToDomain(): Block {
        return Block(
            id = id,
            blockId = blockId,
            lmsWebUrl = lmsWebUrl,
            legacyWebUrl = legacyWebUrl,
            studentViewUrl = studentViewUrl,
            type = BlockType.getBlockType(type),
            displayName = displayName,
            graded = graded,
            studentViewData = studentViewData?.mapToDomain(),
            studentViewMultiDevice = studentViewMultiDevice,
            blockCounts = blockCounts.mapToDomain(),
            descendants = descendants,
            completion = completion,
        )
    }

    companion object {

        fun createFrom(
            block: com.raccoongang.core.data.model.Block,
            courseId: String
        ): BlockDbEntity {
            with(block) {
                return BlockDbEntity(
                    courseId = courseId,
                    id = id ?: "",
                    blockId = blockId ?: "",
                    lmsWebUrl = lmsWebUrl ?: "",
                    legacyWebUrl = legacyWebUrl ?: "",
                    studentViewUrl = studentViewUrl ?: "",
                    type = type ?: "",
                    displayName = displayName ?: "",
                    descendants = descendants ?: emptyList(),
                    graded = graded ?: false,
                    studentViewData = StudentViewDataDb.createFrom(studentViewData),
                    studentViewMultiDevice = studentViewMultiDevice ?: false,
                    blockCounts = BlockCountsDb.createFrom(blockCounts),
                    completion = completion ?: 0.0
                )
            }
        }
    }
}

data class StudentViewDataDb(
    @ColumnInfo("onlyOnWeb")
    val onlyOnWeb: Boolean,
    @ColumnInfo("duration")
    val duration: String,
    @ColumnInfo("topicId")
    val topicId: String,
    @Embedded
    val transcripts: TranscriptsDb?,
    @Embedded
    val encodedVideos: EncodedVideosDb?
) {
    fun mapToDomain(): StudentViewData {
        return StudentViewData(
            onlyOnWeb,
            duration,
            transcripts?.mapToDomain(),
            encodedVideos?.mapToDomain(),
            topicId
        )
    }

    companion object {

        fun createFrom(studentViewData: com.raccoongang.core.data.model.StudentViewData?): StudentViewDataDb {
            return StudentViewDataDb(
                onlyOnWeb = studentViewData?.onlyOnWeb ?: false,
                duration = studentViewData?.duration.toString(),
                transcripts = TranscriptsDb.createFrom(studentViewData?.transcripts),
                encodedVideos = EncodedVideosDb.createFrom(studentViewData?.encodedVideos),
                topicId = studentViewData?.topicId ?: ""
            )
        }

    }
}

data class TranscriptsDb(
    @ColumnInfo("en")
    val en: String
) {
    fun mapToDomain() = Transcripts(en)

    companion object {
        fun createFrom(transcripts: com.raccoongang.core.data.model.Transcripts?): TranscriptsDb {
            return TranscriptsDb(transcripts?.en ?: "")
        }
    }
}

data class EncodedVideosDb(
    @ColumnInfo("youtube")
    val youtube: VideoInfoDb?,
    @ColumnInfo("hls")
    var hls: VideoInfoDb?,
    @ColumnInfo("fallback")
    var fallback: VideoInfoDb?,
    @ColumnInfo("desktopMp4")
    var desktopMp4: VideoInfoDb?,
    @ColumnInfo("mobileHigh")
    var mobileHigh: VideoInfoDb?,
    @ColumnInfo("mobileLow")
    var mobileLow: VideoInfoDb?
) {
    fun mapToDomain(): EncodedVideos {
        return EncodedVideos(
            youtube?.mapToDomain(),
            hls = hls?.mapToDomain(),
            fallback = fallback?.mapToDomain(),
            desktopMp4 = desktopMp4?.mapToDomain(),
            mobileHigh = mobileHigh?.mapToDomain(),
            mobileLow = mobileLow?.mapToDomain(),
        )
    }

    companion object {
        fun createFrom(encodedVideos: com.raccoongang.core.data.model.EncodedVideos?): EncodedVideosDb {
            return EncodedVideosDb(
                youtube = VideoInfoDb.createFrom(encodedVideos?.videoInfo),
                hls = VideoInfoDb.createFrom(encodedVideos?.hls),
                fallback = VideoInfoDb.createFrom(encodedVideos?.fallback),
                desktopMp4 = VideoInfoDb.createFrom(encodedVideos?.desktopMp4),
                mobileHigh = VideoInfoDb.createFrom(encodedVideos?.mobileHigh),
                mobileLow = VideoInfoDb.createFrom(encodedVideos?.mobileLow),
            )
        }
    }

}

data class VideoInfoDb(
    @ColumnInfo("url")
    val url: String,
    @ColumnInfo("fileSize")
    val fileSize: Int
) {
    fun mapToDomain() = VideoInfo(url, fileSize)

    companion object {
        fun createFrom(videoInfo: com.raccoongang.core.data.model.VideoInfo?): VideoInfoDb? {
            if (videoInfo == null) return null
            return VideoInfoDb(
                videoInfo.url ?: "",
                videoInfo.fileSize ?: 0,
            )
        }
    }
}

data class BlockCountsDb(
    @ColumnInfo("video")
    val video: Int
) {
    fun mapToDomain() = BlockCounts(video)

    companion object {
        fun createFrom(blocksCounts: com.raccoongang.core.data.model.BlockCounts?): BlockCountsDb {
            return BlockCountsDb(blocksCounts?.video ?: 0)
        }
    }
}