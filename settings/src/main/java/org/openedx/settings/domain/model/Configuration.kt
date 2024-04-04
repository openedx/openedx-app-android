package org.openedx.settings.domain.model

import org.openedx.core.domain.model.AgreementUrls

/**
 * @param agreementUrls User agreement urls
 * @param faqUrl FAQ url
 * @param supportEmail Email address of support
 * @param versionName Version of the application (1.0.0)
 */
data class Configuration(
    val agreementUrls: AgreementUrls,
    val faqUrl: String,
    val supportEmail: String,
    val versionName: String,
)
