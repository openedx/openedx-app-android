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

enum class VideoQuality(val titleResId: Int, val width: Int, val height: Int) {
    AUTO(R.string.auto_recommended_text, 0, 0),
    OPTION_360P(R.string.video_quality_p360, 640, 360),
    OPTION_540P(R.string.video_quality_p540, 960, 540),
    OPTION_720P(R.string.video_quality_p720, 1280, 720);

    val value: String = this.name.replace("OPTION_", "").lowercase()
}
