package org.openedx.profile.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.ProfileImage
import org.openedx.profile.domain.model.Account
import java.util.Date
import org.openedx.profile.domain.model.Account as DomainAccount

data class Account(
    @SerializedName("username")
    val username: String?,
    @SerializedName("bio")
    val bio: String?,
    @SerializedName("requires_parental_consent")
    val requiresParentalConsent: Boolean?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("is_active")
    val isActive: Boolean?,
    @SerializedName("profile_image")
    val profileImage: ProfileImage?,
    @SerializedName("year_of_birth")
    val yearOfBirth: Int?,
    @SerializedName("level_of_education")
    val levelOfEducation: String?,
    @SerializedName("goals")
    val goals: String?,
    @SerializedName("language_proficiencies")
    val languageProficiencies: List<LanguageProficiency>?,
    @SerializedName("gender")
    val gender: String?,
    @SerializedName("mailing_address")
    val mailingAddress: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("date_joined")
    val dateJoined: Date?,
    @SerializedName("account_privacy")
    val accountPrivacy: Privacy?
) {

    enum class Privacy {
        @SerializedName("private")
        PRIVATE,

        @SerializedName("all_users")
        ALL_USERS
    }

    fun mapToDomain(): Account {
        return Account(
            username = username ?: "",
            bio = bio ?: "",
            requiresParentalConsent = requiresParentalConsent ?: false,
            name = name ?: "",
            country = country ?: "",
            isActive = isActive ?: true,
            profileImage = profileImage!!.mapToDomain(),
            yearOfBirth = yearOfBirth,
            levelOfEducation = levelOfEducation ?: "",
            goals = goals ?: "",
            languageProficiencies = languageProficiencies?.let { languageProficiencyList ->
                languageProficiencyList.map { it.mapToDomain() }
            } ?: emptyList(),
            gender = gender ?: "",
            mailingAddress = mailingAddress ?: "",
            email = email,
            dateJoined = dateJoined,
            accountPrivacy = if (accountPrivacy == Privacy.PRIVATE) {
                DomainAccount.Privacy.PRIVATE
            } else {
                DomainAccount.Privacy.ALL_USERS
            }
        )
    }
}
