package org.openedx.profile.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.AppDataConstants.USER_MIN_YEAR
import org.openedx.core.domain.model.LanguageProficiency
import org.openedx.core.domain.model.ProfileImage
import java.util.Calendar
import java.util.Date

@Parcelize
data class Account(
    val username: String,
    val bio: String,
    val requiresParentalConsent: Boolean,
    val name: String,
    val country: String,
    val isActive: Boolean,
    val profileImage: ProfileImage,
    val yearOfBirth: Int?,
    val levelOfEducation: String,
    val goals: String,
    val languageProficiencies: List<LanguageProficiency>,
    val gender: String,
    val mailingAddress: String,
    val email: String?,
    val dateJoined: Date?,
    val accountPrivacy: Privacy
) : Parcelable {

    enum class Privacy {
        PRIVATE,
        ALL_USERS
    }

    fun isLimited() = accountPrivacy == Privacy.PRIVATE

    fun isOlderThanMinAge(): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return yearOfBirth != null && currentYear - yearOfBirth > USER_MIN_YEAR
    }
}
