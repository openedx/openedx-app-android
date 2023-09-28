package org.openedx.profile.data.repository

import androidx.room.RoomDatabase
import org.openedx.core.ApiConstants
import org.openedx.profile.data.api.ProfileApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.openedx.core.data.storage.CorePreferences
import java.io.File

class ProfileRepository(
    private val api: ProfileApi,
    private val room: RoomDatabase,
    private val preferencesManager: CorePreferences
) {

    suspend fun getAccount(): org.openedx.profile.data.model.Account {
        return api.getAccount(preferencesManager.user?.username!!)
    }

    suspend fun updateAccount(fields: Map<String, Any?>): org.openedx.profile.domain.model.Account {
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
            org.openedx.core.BuildConfig.CLIENT_ID,
            preferencesManager.refreshToken,
            ApiConstants.TOKEN_TYPE_REFRESH
        )
        preferencesManager.clear()
        room.clearAllTables()
    }
}