package org.openedx.core.utils

import org.openedx.core.AppDataConstants.USER_MAX_YEAR
import org.openedx.core.AppDataConstants.defaultLocale
import org.openedx.core.domain.model.RegistrationField
import java.util.Calendar
import java.util.Locale

object LocaleUtils {

    private const val MIN_USER_AGE = 13

    fun getBirthYearsRange(): List<RegistrationField.Option> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return (currentYear - USER_MAX_YEAR..currentYear - 0).reversed().map {
            RegistrationField.Option(it.toString(), it.toString(), "")
        }.toList()
    }

    fun isProfileLimited(inputYear: String?): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return if (!inputYear.isNullOrEmpty()) {
            currentYear - inputYear.toInt() < MIN_USER_AGE
        } else {
            true
        }
    }

    fun getCountries() = getAvailableCountries()

    fun getLanguages() = getAvailableLanguages()

    fun getLanguages(languages: List<String>) = getAvailableLanguages().filter {
        languages.contains(it.value)
    }

    fun getCountryByCountryCode(code: String): String? {
        val countryISO = Locale.getISOCountries().firstOrNull { it == code }
        return countryISO?.let {
            Locale.Builder().setRegion(it).build().getDisplayCountry(defaultLocale)
        }
    }

    fun getLanguageByLanguageCode(code: String): String? {
        val countryISO = Locale.getISOLanguages().firstOrNull { it == code }
        return countryISO?.let {
            Locale.Builder().setLanguage(it).build().getDisplayLanguage(defaultLocale)
        }
    }

    private fun getAvailableCountries() = Locale.getISOCountries()
        .asSequence()
        .map {
            RegistrationField.Option(
                it,
                Locale.Builder().setRegion(it).build().getDisplayCountry(defaultLocale),
                ""
            )
        }
        .sortedBy { it.name }
        .toList()

    private fun getAvailableLanguages() = Locale.getISOLanguages()
        .asSequence()
        .filter { it.length == 2 }
        .map {
            RegistrationField.Option(
                it,
                Locale.Builder().setLanguage(it).build().getDisplayLanguage(defaultLocale),
                ""
            )
        }
        .sortedBy { it.name }
        .toList()

    fun getDisplayLanguage(languageCode: String): String {
        return Locale.Builder().setLanguage(languageCode).build().getDisplayLanguage(defaultLocale)
    }
}
