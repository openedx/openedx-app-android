package org.openedx.profile.presentation.profile

import org.openedx.core.domain.model.AgreementUrls
import org.openedx.profile.domain.model.Account

sealed class ProfileUIState {
    /**
     * @param account User account data
     * @param agreementUrls User agreement urls
     * @param faqUrl FAQ url
     * @param supportEmail Email address of support
     * @param versionName Version of the application (1.0.0)
     */
    data class Data(
        val account: Account,
        val agreementUrls: AgreementUrls,
        val faqUrl: String,
        val supportEmail: String,
        val versionName: String,
    ) : ProfileUIState()

    object Loading : ProfileUIState()
}
