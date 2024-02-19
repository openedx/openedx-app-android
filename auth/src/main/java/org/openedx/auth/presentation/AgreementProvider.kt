package org.openedx.auth.presentation

import androidx.compose.ui.text.intl.Locale
import org.openedx.auth.R
import org.openedx.core.ApiConstants
import org.openedx.core.config.Config
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.domain.model.RegistrationFieldType
import org.openedx.core.system.ResourceManager

class AgreementProvider(
    private val config: Config,
    private val resourceManager: ResourceManager,
) {
    internal fun getAgreement(isSignIn: Boolean): RegistrationField? {
        val agreementConfig = config.getAgreement(Locale.current.language)
        if (agreementConfig.eulaUrl.isBlank()) return null
        val agreementRes = if (isSignIn) {
            R.string.auth_agreement_signin_in
        } else {
            R.string.auth_agreement_creating_account
        }
        val eula = resourceManager.getString(
            R.string.auth_cdata_template,
            agreementConfig.eulaUrl,
            resourceManager.getString(R.string.auth_agreement_eula)
        )
        val tos = resourceManager.getString(
            R.string.auth_cdata_template,
            agreementConfig.tosUrl,
            resourceManager.getString(R.string.auth_agreement_tos)
        )
        val privacy = resourceManager.getString(
            R.string.auth_cdata_template,
            agreementConfig.privacyPolicyUrl,
            resourceManager.getString(R.string.auth_agreement_privacy)
        )
        val text = resourceManager.getString(
            agreementRes,
            eula,
            tos,
            config.getPlatformName(),
            privacy,
        )
        return RegistrationField(
            name = ApiConstants.RegistrationFields.HONOR_CODE,
            label = text,
            type = RegistrationFieldType.PLAINTEXT,
            placeholder = "",
            instructions = "",
            exposed = false,
            required = false,
            restrictions = RegistrationField.Restrictions(),
            options = emptyList(),
            errorInstructions = ""
        )
    }
}
