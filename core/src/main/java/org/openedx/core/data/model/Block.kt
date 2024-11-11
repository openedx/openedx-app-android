package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.BlockType
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.Block as DomainBlock
import org.openedx.core.domain.model.BlockCounts as DomainBlockCounts
import org.openedx.core.domain.model.EncodedVideos as DomainEncodedVideos
import org.openedx.core.domain.model.StudentViewData as DomainStudentViewData
import org.openedx.core.domain.model.VideoInfo as DomainVideoInfo

data class Block(
    @SerializedName("id")
    val id: String?,
    @SerializedName("block_id")
    val blockId: String?,
    @SerializedName("lms_web_url")
    val lmsWebUrl: String?,
    @SerializedName("legacy_web_url")
    val legacyWebUrl: String?,
    @SerializedName("student_view_url")
    val studentViewUrl: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("display_name")
    val displayName: String?,
    @SerializedName("graded")
    val graded: Boolean?,
    @SerializedName("descendants")
    val descendants: List<String>?,
    @SerializedName("student_view_data")
    val studentViewData: StudentViewData?,
    @SerializedName("student_view_multi_device")
    val studentViewMultiDevice: Boolean?,
    @SerializedName("block_counts")
    val blockCounts: BlockCounts?,
    @SerializedName("completion")
    val completion: Double?,
    @SerializedName("contains_gated_content")
    val containsGatedContent: Boolean?,
    @SerializedName("assignment_progress")
    val assignmentProgress: AssignmentProgress?,
    @SerializedName("due")
    val due: String?,
    @SerializedName("offline_download")
    val offlineDownload: OfflineDownload?,
) {
    fun mapToDomain(blockData: Map<String, Block>): DomainBlock {
        val blockType = BlockType.getBlockType(type ?: "")
        val descendantsType = if (blockType == BlockType.VERTICAL) {
            val types = descendants?.map { descendant ->
                BlockType.getBlockType(blockData[descendant]?.type ?: "")
            } ?: emptyList()
            val sortedBlockTypes = BlockType.sortByPriority(types)
            sortedBlockTypes.firstOrNull() ?: blockType
        } else {
            blockType
        }

        return DomainBlock(
            id = id ?: "",
            blockId = blockId ?: "",
            lmsWebUrl = lmsWebUrl ?: "",
            legacyWebUrl = legacyWebUrl ?: "",
            studentViewUrl = studentViewUrl ?: "",
            type = blockType,
            displayName = displayName ?: "",
            descendants = descendants ?: emptyList(),
            descendantsType = descendantsType,
            graded = graded ?: false,
            studentViewData = studentViewData?.mapToDomain(),
            studentViewMultiDevice = studentViewMultiDevice ?: false,
            blockCounts = blockCounts?.mapToDomain()!!,
            completion = completion ?: 0.0,
            containsGatedContent = containsGatedContent ?: false,
            assignmentProgress = assignmentProgress?.mapToDomain(),
            due = TimeUtils.iso8601ToDate(due ?: ""),
            offlineDownload = offlineDownload?.mapToDomain()
        )
    }
}

data class StudentViewData(
    @SerializedName("only_on_web")
    var onlyOnWeb: Boolean?,
    @SerializedName("duration")
    var duration: Any?,
    @SerializedName("transcripts")
    var transcripts: HashMap<String, String>?,
    @SerializedName("encoded_videos")
    var encodedVideos: EncodedVideos?,
    @SerializedName("all_sources")
    var allSources: List<Any?>?,
    @SerializedName("topic_id")
    val topicId: String?
) {
    fun mapToDomain(): DomainStudentViewData {
        return DomainStudentViewData(
            onlyOnWeb = onlyOnWeb ?: false,
            duration = duration ?: "",
            transcripts = transcripts,
            encodedVideos = encodedVideos?.mapToDomain(),
            topicId = topicId ?: ""
        )
    }
}

data class EncodedVideos(
    @SerializedName("youtube")
    var videoInfo: VideoInfo?,
    @SerializedName("hls")
    var hls: VideoInfo?,
    @SerializedName("fallback")
    var fallback: VideoInfo?,
    @SerializedName("desktop_mp4")
    var desktopMp4: VideoInfo?,
    @SerializedName("mobile_high")
    var mobileHigh: VideoInfo?,
    @SerializedName("mobile_low")
    var mobileLow: VideoInfo?
) {

    fun mapToDomain(): DomainEncodedVideos {
        return DomainEncodedVideos(
            youtube = videoInfo?.mapToDomain(),
            hls = hls?.mapToDomain(),
            fallback = fallback?.mapToDomain(),
            desktopMp4 = desktopMp4?.mapToDomain(),
            mobileHigh = mobileHigh?.mapToDomain(),
            mobileLow = mobileLow?.mapToDomain()
        )
    }
}

data class VideoInfo(
    @SerializedName("url")
    var url: String?,
    @SerializedName("file_size")
    var fileSize: Long?
) {
    fun mapToDomain(): DomainVideoInfo {
        return DomainVideoInfo(
            url = url ?: "",
            fileSize = fileSize ?: 0
        )
    }
}

data class BlockCounts(
    @SerializedName("video")
    var video: Int?
) {
    fun mapToDomain(): DomainBlockCounts {
        return DomainBlockCounts(
            video = video ?: 0
        )
    }
}
