package org.openedx.core.presentation.global

interface AppDataHolder {
    val appData: AppData
    fun checkWhatsNew(): Boolean
}

data class AppData(
    val versionName: String
)