package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class BrazeConfig(
    @SerializedName("ENABLED")
    val isEnabled: Boolean = false,

    @SerializedName("PUSH_NOTIFICATIONS_ENABLED")
    val isPushNotificationsEnabled: Boolean = false
)
