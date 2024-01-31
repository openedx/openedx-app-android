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
    AUTO(R.string.video_quality_auto, R.string.video_quality_auto_description, 0, 0),
    OPTION_360P(R.string.video_quality_p360, R.string.video_quality_p360_description, 640, 360),
    OPTION_540P(R.string.video_quality_p540, 0, 960, 540),
    OPTION_720P(R.string.video_quality_p720, R.string.video_quality_p720_description, 1280, 720);

    val value: String = this.name.replace("OPTION_", "").lowercase()
}
