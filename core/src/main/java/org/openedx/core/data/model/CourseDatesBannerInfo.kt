package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.CourseDatesBannerInfo

data class CourseDatesBannerInfo(
    @SerializedName("dates_banner_info")
    val datesBannerInfo: DatesBannerInfo?,
    @SerializedName("has_ended")
    val hasEnded: Boolean?,
) {
    fun mapToDomain(): CourseDatesBannerInfo {
        return CourseDatesBannerInfo(
            missedDeadlines = datesBannerInfo?.missedDeadlines ?: false,
            missedGatedContent = datesBannerInfo?.missedGatedContent ?: false,
            verifiedUpgradeLink = datesBannerInfo?.verifiedUpgradeLink ?: "",
            contentTypeGatingEnabled = datesBannerInfo?.contentTypeGatingEnabled ?: false,
            hasEnded = hasEnded ?: false,
        )
    }
}
