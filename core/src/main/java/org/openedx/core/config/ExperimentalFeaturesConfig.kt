package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class ExperimentalFeaturesConfig(
    @SerializedName("APP_LEVEL_DOWNLOADS")
    val appLevelDownloadsConfig: AppLevelDownloadsConfig = AppLevelDownloadsConfig(),
)
