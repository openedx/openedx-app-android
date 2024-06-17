package org.openedx.core.domain.model

/**
 * Data class with information about user agreements URLs
 *
 * @param agreementUrls Map with keys from SUPPORTED_LANGUAGES config
 * @param defaultAgreementUrls AgreementUrls for default language ('en')
 */
internal data class Agreement(
    private val agreementUrls: Map<String, AgreementUrls> = mapOf(),
    private val defaultAgreementUrls: AgreementUrls
) {
    fun getAgreementForLocale(locale: String): AgreementUrls {
        return agreementUrls.getOrDefault(locale, defaultAgreementUrls)
    }
}

data class AgreementUrls(
    val privacyPolicyUrl: String = "",
    val contactSupportUrl: String = "",
    val cookiePolicyUrl: String = "",
    val dataSellConsentUrl: String = "",
    val tosUrl: String = "",
    val eulaUrl: String = "",
    val supportedLanguages: List<String> = emptyList(),
)
