package org.openedx.core.config

import android.net.Uri
import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.Agreement
import org.openedx.core.domain.model.AgreementUrls

internal data class AgreementUrlsConfig(
    @SerializedName("PRIVACY_POLICY_URL")
    private val privacyPolicyUrl: String = "",
    @SerializedName("COOKIE_POLICY_URL")
    private val cookiePolicyUrl: String = "",
    @SerializedName("DATA_SELL_CONSENT_URL")
    private val dataSellConsentUrl: String = "",
    @SerializedName("TOS_URL")
    private val tosUrl: String = "",
    @SerializedName("CONTACT_SUPPORT_URL")
    private val contactSupportUrl: String = "",
    @SerializedName("EULA_URL")
    private val eulaUrl: String = "",
    @SerializedName("SUPPORTED_LANGUAGES")
    private val supportedLanguages: List<String> = emptyList(),
) {
    fun mapToDomain(): Agreement {
        val defaultAgreementUrls = AgreementUrls(
            privacyPolicyUrl = privacyPolicyUrl,
            cookiePolicyUrl = cookiePolicyUrl,
            dataSellConsentUrl = dataSellConsentUrl,
            tosUrl = tosUrl,
            contactSupportUrl = contactSupportUrl,
            eulaUrl = eulaUrl,
            supportedLanguages = supportedLanguages,
        )
        val agreementUrls = if (supportedLanguages.isNotEmpty()) {
            supportedLanguages.associateWith {
                AgreementUrls(
                    privacyPolicyUrl = privacyPolicyUrl.appendLocale(it),
                    cookiePolicyUrl = cookiePolicyUrl.appendLocale(it),
                    dataSellConsentUrl = dataSellConsentUrl.appendLocale(it),
                    tosUrl = tosUrl.appendLocale(it),
                    contactSupportUrl = contactSupportUrl.appendLocale(it),
                    eulaUrl = eulaUrl.appendLocale(it),
                    supportedLanguages = supportedLanguages,
                )
            }
        } else {
            mapOf()
        }
        return Agreement(agreementUrls, defaultAgreementUrls)
    }

    private fun String.appendLocale(locale: String): String {
        if (this.isBlank()) return this
        val uri = Uri.parse(this)
        return Uri.Builder().scheme(uri.scheme)
            .authority(uri.authority)
            .appendPath(locale + uri.encodedPath)
            .build()
            .toString()
    }
}
