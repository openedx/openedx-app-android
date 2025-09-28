package org.openedx.core.domain.model

import android.content.Context
import android.os.Parcelable
import android.webkit.URLUtil
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.openedx.core.AppDataConstants
import org.openedx.core.BlockType
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.FileType
import org.openedx.core.utils.PreviewHelper
import org.openedx.core.utils.VideoPreview
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
    val due: Date?,
    val offlineDownload: OfflineDownload?
) : Parcelable {
    val isDownloadable: Boolean
        get() {
            return (studentViewData != null && studentViewData.encodedVideos?.hasDownloadableVideo == true) || isxBlock
        }

    val isxBlock: Boolean
        get() = !offlineDownload?.fileUrl.isNullOrEmpty()

    val downloadableType: FileType?
        get() = if (type == BlockType.VIDEO) {
            FileType.VIDEO
        } else if (isxBlock) {
            FileType.X_BLOCK
        } else {
            null
        }

    fun isGated() = containsGatedContent

    fun isCompleted() = completion == 1.0

    fun getFirstDescendantBlock(blocks: List<Block>): Block? {
        return descendants.firstOrNull { descendant ->
            blocks.find { it.id == descendant } != null
        }?.let { descendant ->
            blocks.find { it.id == descendant }
        }
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

    fun getFileSize(): Long {
        return when {
            type == BlockType.VIDEO -> downloadModel?.size ?: 0L
            isxBlock -> offlineDownload?.fileSize ?: 0L
            else -> 0L
        }
    }

    fun getVideoPreview(context: Context, isOnline: Boolean, offlineUrl: String?): VideoPreview? {
        return if (studentViewData?.encodedVideos?.hasYoutubeUrl == true) {
            val youtubeUrl = studentViewData.encodedVideos.youtube?.url ?: ""
            VideoPreview.createYoutubePreview(
                PreviewHelper.getYouTubeThumbnailUrl(youtubeUrl)
            )
        } else if (studentViewData?.encodedVideos?.hasVideoUrl == true) {
            val videoUrl = if (studentViewData.encodedVideos.videoUrl.isNotEmpty() && isOnline) {
                studentViewData.encodedVideos.videoUrl
            } else {
                offlineUrl ?: ""
            }
            val bitmap = PreviewHelper.getVideoFrameBitmap(
                context = context,
                isOnline = isOnline,
                videoUrl = videoUrl
            )
            bitmap?.let { VideoPreview.createEncodedVideoPreview(it) }
        } else {
            null
        }
    }

    val videoUrl: String?
        get() = if (studentViewData?.encodedVideos?.hasVideoUrl == true) {
            studentViewData.encodedVideos.videoUrl
        } else {
            studentViewData?.encodedVideos?.youtube?.url
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
        get() = mobileHigh?.url != null ||
                mobileLow?.url != null ||
                desktopMp4?.url != null ||
                hls?.url != null ||
                fallback?.url != null

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
        return when {
            isPreferredVideoInfo(mobileLow) -> mobileLow
            isPreferredVideoInfo(mobileHigh) -> mobileHigh
            isPreferredVideoInfo(desktopMp4) -> desktopMp4
            fallback != null && isPreferredVideoInfo(fallback) &&
                    !VideoUtil.videoHasFormat(
                        fallback!!.url,
                        AppDataConstants.VIDEO_FORMAT_M3U8
                    ) -> fallback

            hls != null && isPreferredVideoInfo(hls) -> hls
            else -> null
        }
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
    val fileSize: Long,
) : Parcelable

@Parcelize
data class BlockCounts(
    val video: Int,
) : Parcelable

@Parcelize
data class OfflineDownload(
    var fileUrl: String,
    var lastModified: String?,
    var fileSize: Long,
) : Parcelable
