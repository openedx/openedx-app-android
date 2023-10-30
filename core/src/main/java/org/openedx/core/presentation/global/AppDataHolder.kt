package org.openedx.core.presentation.global

/** Deprecated. Use koin to inject AppData **/
interface AppDataHolder {
    val appData: AppData
    fun shouldShowWhatsNew(): Boolean
}

data class AppData(
    val versionName: String
)