package com.raccoongang.core.presentation.global

interface AppDataHolder {
    val appData: AppData
}

data class AppData(
    val versionName: String
)