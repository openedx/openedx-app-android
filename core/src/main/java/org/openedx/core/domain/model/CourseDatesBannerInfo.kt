package org.openedx.core.domain.model

import org.openedx.core.R
import org.openedx.core.domain.model.CourseBannerType.BLANK
import org.openedx.core.domain.model.CourseBannerType.INFO_BANNER
import org.openedx.core.domain.model.CourseBannerType.RESET_DATES
import org.openedx.core.domain.model.CourseBannerType.UPGRADE_TO_GRADED
import org.openedx.core.domain.model.CourseBannerType.UPGRADE_TO_RESET

data class CourseDatesBannerInfo(
    private val missedDeadlines: Boolean,
    private val missedGatedContent: Boolean,
    private val verifiedUpgradeLink: String,
    private val contentTypeGatingEnabled: Boolean,
    private val hasEnded: Boolean,
) {
    val bannerType by lazy { getCourseBannerType() }

    fun isBannerAvailableForUserType(isSelfPaced: Boolean): Boolean {
        if (hasEnded) return false

        val selfPacedAvailable = isSelfPaced && bannerType != BLANK
        val instructorPacedAvailable = !isSelfPaced && bannerType == UPGRADE_TO_GRADED

        return selfPacedAvailable || instructorPacedAvailable
    }

    fun isBannerAvailableForDashboard(): Boolean {
        return hasEnded.not() && bannerType == RESET_DATES
    }

    private fun getCourseBannerType(): CourseBannerType = when {
        canUpgradeToGraded() -> UPGRADE_TO_GRADED
        canUpgradeToReset() -> UPGRADE_TO_RESET
        canResetDates() -> RESET_DATES
        infoBanner() -> INFO_BANNER
        else -> BLANK
    }

    private fun infoBanner(): Boolean = !missedDeadlines

    private fun canUpgradeToGraded(): Boolean = contentTypeGatingEnabled && !missedDeadlines

    private fun canUpgradeToReset(): Boolean =
        !canUpgradeToGraded() && missedDeadlines && missedGatedContent

    private fun canResetDates(): Boolean =
        !canUpgradeToGraded() && missedDeadlines && !missedGatedContent
}

enum class CourseBannerType(
    val headerResId: Int = 0,
    val bodyResId: Int = 0,
    val buttonResId: Int = 0
) {
    BLANK,
    INFO_BANNER(bodyResId = R.string.core_dates_info_banner_body),
    UPGRADE_TO_GRADED(bodyResId = R.string.core_dates_upgrade_to_graded_banner_body),
    UPGRADE_TO_RESET(bodyResId = R.string.core_dates_upgrade_to_reset_banner_body),
    RESET_DATES(
        headerResId = R.string.core_dates_reset_dates_banner_header,
        bodyResId = R.string.core_dates_reset_dates_banner_body,
        buttonResId = R.string.core_dates_reset_dates_banner_button
    )
}
