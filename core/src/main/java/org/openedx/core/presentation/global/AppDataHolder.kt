package org.openedx.core.presentation.global

interface AppDataHolder {
    val appData: AppData
    fun shouldShowWhatsNew(): Boolean
}

data class AppData(
    val versionName: String
)