package com.raccoongang.profile.data.repository

import androidx.room.RoomDatabase
import com.raccoongang.core.ApiConstants
import com.raccoongang.core.BuildConfig
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.profile.data.api.ProfileApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileRepository(
    private val api: ProfileApi,
    private val room: RoomDatabase,
    private val preferencesManager: PreferencesManager
) {

    suspend fun getAccount(): com.raccoongang.core.domain.model.Account {
        return api.getAccount(preferencesManager.user?.username!!).mapToDomain()
    }

    suspend fun updateAccount(fields: Map<String, Any?>): com.raccoongang.core.domain.model.Account {
        return api.updateAccount(preferencesManager.user?.username!!, fields).mapToDomain()
    }

    suspend fun setProfileImage(file: File, mimeType: String) {
        api.setProfileImage(
            preferencesManager.user?.username!!,
            "attachment;filename=filename.${file.extension}",
            true,
            file.asRequestBody(mimeType.toMediaType())
        )
    }

    suspend fun deleteProfileImage() {
        api.deleteProfileImage(preferencesManager.user?.username!!)
    }

    suspend fun deactivateAccount(password: String) = api.deactivateAccount(password)

    suspend fun logout() {
        api.revokeAccessToken(
            BuildConfig.CLIENT_ID,
            preferencesManager.refreshToken,
            ApiConstants.TOKEN_TYPE_REFRESH
        )
        preferencesManager.clear()
        room.clearAllTables()
    }
}