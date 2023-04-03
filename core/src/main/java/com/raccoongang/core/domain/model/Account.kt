package com.raccoongang.core.domain.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.raccoongang.core.AppDataConstants.USER_MIN_YEAR
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Account(
    @SerializedName("username")
    val username: String,
    @SerializedName("bio")
    val bio: String,
    @SerializedName("requires_parental_consent")
    val requiresParentalConsent: Boolean,
    @SerializedName("name")
    val name: String,
    @SerializedName("country")
    val country: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("profile_image")
    val profileImage: ProfileImage,
    @SerializedName("year_of_birth")
    val yearOfBirth: Int?,
    @SerializedName("level_of_education")
    val levelOfEducation: String,
    @SerializedName("goals")
    val goals: String,
    @SerializedName("language_proficiencies")
    val languageProficiencies: List<LanguageProficiency>,
    @SerializedName("gender")
    val gender: String,
    @SerializedName("mailing_address")
    val mailingAddress: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("date_joined")
    val dateJoined: Date?,
    @SerializedName("account_privacy")
    val accountPrivacy: Privacy
) : Parcelable {

    enum class Privacy {
        @SerializedName("private")
        PRIVATE,
        @SerializedName("all_users")
        ALL_USERS
    }

    fun isLimited() = accountPrivacy == Privacy.PRIVATE

    fun isOlderThanMinAge() : Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return yearOfBirth != null && currentYear - yearOfBirth > USER_MIN_YEAR
    }

}
