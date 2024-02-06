package org.openedx.core.domain.model

import org.openedx.core.R

data class VideoSettings(
    val wifiDownloadOnly: Boolean,
    val videoQuality: VideoQuality
) {
    companion object {
        val default = VideoSettings(true, VideoQuality.AUTO)
    }
}

enum class VideoQuality(
    val titleResId: Int,
    val desResId: Int = 0,
    val width: Int,
    val height: Int
) {
    AUTO(
        titleResId = R.string.core_video_quality_auto,
        desResId = R.string.core_video_quality_auto_description,
        width = 0,
        height = 0
    ),
    OPTION_360P(
        titleResId = R.string.core_video_quality_p360,
        desResId = R.string.core_video_quality_p360_description,
        width = 640,
        height = 360
    ),
    OPTION_540P(
        titleResId = R.string.core_video_quality_p540,
        desResId = 0,
        width = 960,
        height = 540
    ),
    OPTION_720P(
        titleResId = R.string.core_video_quality_p720,
        desResId = R.string.core_video_quality_p720_description,
        width = 1280,
        height = 720
    );
}
