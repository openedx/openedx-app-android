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

enum class VideoQuality(val titleResId: Int) {
    AUTO(R.string.auto_recommended_text),
    OPTION_360P(R.string.video_quality_p360),
    OPTION_540P(R.string.video_quality_p540),
    OPTION_720P(R.string.video_quality_p720);

    val value: String = this.name.replace("OPTION_", "").lowercase()
}
