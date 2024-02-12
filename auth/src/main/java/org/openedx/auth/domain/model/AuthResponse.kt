package org.openedx.auth.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.utils.TimeUtils

@Parcelize
data class AuthResponse(
    var accessToken: String?,
    var tokenType: String?,
    var expiresIn: Long?,
    var scope: String?,
    var error: String?,
    var refreshToken: String?,
) : Parcelable {
    fun getTokenExpiryTime(): Long {
        return (expiresIn ?: 0L) + TimeUtils.getCurrentTime()
    }
}
