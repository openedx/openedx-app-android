package com.raccoongang.core.domain.model

import android.webkit.URLUtil
import com.raccoongang.core.AppDataConstants
import com.raccoongang.core.BlockType
import com.raccoongang.core.module.db.DownloadModel
import com.raccoongang.core.module.db.DownloadedState
import com.raccoongang.core.module.db.FileType
import com.raccoongang.core.utils.VideoUtil


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
    val completion: Double,
    val downloadModel: DownloadModel? = null
) {
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

}

data class StudentViewData(
    val onlyOnWeb: Boolean,
    val duration: Any,
    val transcripts: Transcripts?,
    val encodedVideos: EncodedVideos?,
    val topicId: String
)

data class Transcripts(
    val en: String
)

data class EncodedVideos(
    val youtube: VideoInfo?,
    var hls: VideoInfo?,
    var fallback: VideoInfo?,
    var desktopMp4: VideoInfo?,
    var mobileHigh: VideoInfo?,
    var mobileLow: VideoInfo?
) {
    val hasDownloadableVideo: Boolean
        get() = isPreferredVideoInfo(hls) ||
                isPreferredVideoInfo(fallback) ||
                isPreferredVideoInfo(desktopMp4) ||
                isPreferredVideoInfo(mobileHigh) ||
                isPreferredVideoInfo(mobileLow)

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

data class VideoInfo(
    val url: String,
    val fileSize: Int
)

data class BlockCounts(
    val video: Int
)