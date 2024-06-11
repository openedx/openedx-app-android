package org.openedx.core.domain.model

import android.os.Parcelable
import android.webkit.URLUtil
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.openedx.core.AppDataConstants
import org.openedx.core.BlockType
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.utils.VideoUtil
import java.util.Date

@Parcelize
data class Block(
    val id: String,
    val blockId: String,
    val lmsWebUrl: String,
    val legacyWebUrl: String,
    val studentViewUrl: String,
    val type: BlockType,
    val displayName: String,
    val graded: Boolean,
    val studentViewData: StudentViewData?,
    val studentViewMultiDevice: Boolean,
    val blockCounts: BlockCounts,
    val descendants: List<String>,
    val descendantsType: BlockType,
    val completion: Double,
    val containsGatedContent: Boolean = false,
    val downloadModel: DownloadModel? = null,
    val assignmentProgress: AssignmentProgress?,
    val due: Date?
) : Parcelable {
    val isDownloadable: Boolean
        get() {
            return studentViewData != null && studentViewData.encodedVideos?.hasDownloadableVideo == true
        }

    val downloadableType: FileType
        get() = when (type) {
            BlockType.VIDEO -> {
                FileType.VIDEO
            }

            else -> {
                FileType.UNKNOWN
            }
        }

    fun isDownloading(): Boolean {
        return downloadModel?.downloadedState == DownloadedState.DOWNLOADING ||
                downloadModel?.downloadedState == DownloadedState.WAITING
    }

    fun isDownloaded() = downloadModel?.downloadedState == DownloadedState.DOWNLOADED

    fun isGated() = containsGatedContent

    fun isCompleted() = completion == 1.0

    fun getFirstDescendantBlock(blocks: List<Block>): Block? {
        if (blocks.isEmpty()) return null
        descendants.forEach { descendant ->
            blocks.find { it.id == descendant }?.let { descendantBlock ->
                return descendantBlock
            }
        }
        return null
    }

    fun getDownloadsCount(blocks: List<Block>): Int {
        if (blocks.isEmpty()) return 0
        var count = 0
        descendants.forEach { id ->
            blocks.find { it.id == id }?.let { descendantBlock ->
                count += blocks.filter { descendantBlock.descendants.contains(it.id) && it.isDownloadable }.size
            }
        }
        return count
    }

    val isVideoBlock get() = type == BlockType.VIDEO
    val isDiscussionBlock get() = type == BlockType.DISCUSSION
    val isHTMLBlock get() = type == BlockType.HTML
    val isProblemBlock get() = type == BlockType.PROBLEM
    val isOpenAssessmentBlock get() = type == BlockType.OPENASSESSMENT
    val isDragAndDropBlock get() = type == BlockType.DRAG_AND_DROP_V2
    val isWordCloudBlock get() = type == BlockType.WORD_CLOUD
    val isLTIConsumerBlock get() = type == BlockType.LTI_CONSUMER
    val isSurveyBlock get() = type == BlockType.SURVEY
}

@Parcelize
data class StudentViewData(
    val onlyOnWeb: Boolean,
    val duration: @RawValue Any,
    val transcripts: HashMap<String, String>?,
    val encodedVideos: EncodedVideos?,
    val topicId: String,
) : Parcelable

@Parcelize
data class EncodedVideos(
    val youtube: VideoInfo?,
    var hls: VideoInfo?,
    var fallback: VideoInfo?,
    var desktopMp4: VideoInfo?,
    var mobileHigh: VideoInfo?,
    var mobileLow: VideoInfo?,
) : Parcelable {
    val hasDownloadableVideo: Boolean
        get() = isPreferredVideoInfo(hls) ||
                isPreferredVideoInfo(fallback) ||
                isPreferredVideoInfo(desktopMp4) ||
                isPreferredVideoInfo(mobileHigh) ||
                isPreferredVideoInfo(mobileLow)

    val hasNonYoutubeVideo: Boolean
        get() = mobileHigh?.url != null
                || mobileLow?.url != null
                || desktopMp4?.url != null
                || hls?.url != null
                || fallback?.url != null

    val videoUrl: String
        get() = fallback?.url
            ?: hls?.url
            ?: desktopMp4?.url
            ?: mobileHigh?.url
            ?: mobileLow?.url
            ?: ""

    val hasVideoUrl: Boolean
        get() = videoUrl.isNotEmpty()

    val hasYoutubeUrl: Boolean
        get() = youtube?.url?.isNotEmpty() == true

    fun getPreferredVideoInfoForDownloading(preferredVideoQuality: VideoQuality): VideoInfo? {
        var preferredVideoInfo = when (preferredVideoQuality) {
            VideoQuality.OPTION_360P -> mobileLow
            VideoQuality.OPTION_540P -> mobileHigh
            VideoQuality.OPTION_720P -> desktopMp4
            else -> null
        }
        if (preferredVideoInfo == null) {
            preferredVideoInfo = getDefaultVideoInfoForDownloading()
        }
        return if (isPreferredVideoInfo(preferredVideoInfo)) {
            preferredVideoInfo
        } else {
            null
        }
    }

    private fun getDefaultVideoInfoForDownloading(): VideoInfo? {
        if (isPreferredVideoInfo(mobileLow)) {
            return mobileLow
        }
        if (isPreferredVideoInfo(mobileHigh)) {
            return mobileHigh
        }
        if (isPreferredVideoInfo(desktopMp4)) {
            return desktopMp4
        }
        fallback?.let {
            if (isPreferredVideoInfo(it) &&
                !VideoUtil.videoHasFormat(it.url, AppDataConstants.VIDEO_FORMAT_M3U8)
            ) {
                return fallback
            }
        }
        hls?.let {
            if (isPreferredVideoInfo(it)
            ) {
                return hls
            }
        }
        return null
    }

    private fun isPreferredVideoInfo(videoInfo: VideoInfo?): Boolean {
        return videoInfo != null &&
                URLUtil.isNetworkUrl(videoInfo.url) &&
                VideoUtil.isValidVideoUrl(videoInfo.url)
    }

}

@Parcelize
data class VideoInfo(
    val url: String,
    val fileSize: Int,
) : Parcelable

@Parcelize
data class BlockCounts(
    val video: Int,
) : Parcelable
