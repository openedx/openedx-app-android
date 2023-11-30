package org.openedx.profile.domain.model

import org.openedx.core.config.AgreementUrlsConfig

data class AppConfig(
    val feedbackEmailAddress: String,
    val agreementUrlsConfig: AgreementUrlsConfig,
)
