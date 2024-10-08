package org.openedx.auth.presentation

import androidx.compose.ui.text.intl.Locale
import org.openedx.auth.R
import org.openedx.core.config.Config
import org.openedx.foundation.system.ResourceManager

class AgreementProvider(
    private val config: Config,
    private val resourceManager: ResourceManager,
) {
    internal fun getAgreement(isSignIn: Boolean): String? {
        val agreementConfig = config.getAgreement(Locale.current.language)
        if (agreementConfig.eulaUrl.isBlank()) return null
        val platformName = config.getPlatformName()
        val agreementRes = if (isSignIn) {
            R.string.auth_agreement_signin_in
        } else {
            R.string.auth_agreement_creating_account
        }
        val eula = resourceManager.getString(
            R.string.auth_cdata_template,
            agreementConfig.eulaUrl,
            "$platformName ${resourceManager.getString(R.string.auth_agreement_eula)}"
        )
        val tos = resourceManager.getString(
            R.string.auth_cdata_template,
            agreementConfig.tosUrl,
            "$platformName ${resourceManager.getString(R.string.auth_agreement_tos)}"
        )
        val privacy = resourceManager.getString(
            R.string.auth_cdata_template,
            agreementConfig.privacyPolicyUrl,
            "$platformName ${resourceManager.getString(R.string.auth_agreement_privacy)}"
        )
        return resourceManager.getString(
            agreementRes,
            eula,
            tos,
            config.getPlatformName(),
            privacy,
        )
    }
}
