package org.openedx.profile

import org.openedx.core.domain.model.AgreementUrls
import org.openedx.core.domain.model.LanguageProficiency
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.presentation.global.AppData
import org.openedx.profile.domain.model.Account
import org.openedx.profile.domain.model.Configuration

object ProfileMocks {
    val account = Account(
        username = "jdoe",
        name = "John Doe",
        bio = "Preview user",
        requiresParentalConsent = false,
        country = "US",
        isActive = true,
        profileImage = ProfileImage(
            imageUrlFull = "",
            imageUrlLarge = "",
            imageUrlMedium = "",
            imageUrlSmall = "",
            hasImage = false
        ),
        yearOfBirth = 1990,
        levelOfEducation = "Bachelor",
        goals = "Learning Kotlin",
        languageProficiencies = emptyList<LanguageProficiency>(),
        gender = "Male",
        mailingAddress = "",
        email = "jdoe@example.com",
        dateJoined = null,
        accountPrivacy = Account.Privacy.ALL_USERS
    )

    val appData = AppData(
        appName = "OpenEdX",
        applicationId = "org.edx.mobile",
        versionName = "1.0.0"
    )

    val configuration = Configuration(
        agreementUrls = AgreementUrls(
            eulaUrl = "https://example.com/eula"
        ),
        faqUrl = "https://example.com/faq",
        supportEmail = "support@example.com",
        versionName = "1.0.0"
    )
}
