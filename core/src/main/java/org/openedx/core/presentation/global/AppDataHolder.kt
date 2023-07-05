package org.openedx.core.presentation.global

interface AppDataHolder {
    val appData: AppData
}

data class AppData(
    val versionName: String
)