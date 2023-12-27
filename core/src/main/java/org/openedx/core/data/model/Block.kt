package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.BlockType
import org.openedx.core.domain.model.Block

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
) {
    fun mapToDomain(blockData: Map<String, org.openedx.core.data.model.Block>): Block {
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

        return org.openedx.core.domain.model.Block(
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
            containsGatedContent = containsGatedContent ?: false
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
    fun mapToDomain(): org.openedx.core.domain.model.StudentViewData {
        return org.openedx.core.domain.model.StudentViewData(
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

    fun mapToDomain(): org.openedx.core.domain.model.EncodedVideos {
        return org.openedx.core.domain.model.EncodedVideos(
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
    var fileSize: Int?
) {
    fun mapToDomain(): org.openedx.core.domain.model.VideoInfo {
        return org.openedx.core.domain.model.VideoInfo(
            url = url ?: "",
            fileSize = fileSize ?: 0
        )
    }
}

data class BlockCounts(
    @SerializedName("video")
    var video: Int?
) {
    fun mapToDomain(): org.openedx.core.domain.model.BlockCounts {
        return org.openedx.core.domain.model.BlockCounts(
            video = video ?: 0
        )
    }
}