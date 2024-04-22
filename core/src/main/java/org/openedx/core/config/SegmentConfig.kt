package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class SegmentConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = false,

    @SerializedName("SEGMENT_IO_WRITE_KEY")
    val segmentWriteKey: String = "",
)
