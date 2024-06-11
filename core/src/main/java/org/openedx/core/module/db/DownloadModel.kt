package org.openedx.core.module.db

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadModel(
    val id: String,
    val title: String,
    val size: Int,
    val path: String,
    val url: String,
    val type: FileType,
    val downloadedState: DownloadedState,
    val progress: Float?
) : Parcelable

enum class DownloadedState {
    WAITING, DOWNLOADING, DOWNLOADED, NOT_DOWNLOADED;

    val isWaitingOrDownloading: Boolean
        get() {
            return this == WAITING || this == DOWNLOADING
        }

    val isDownloaded: Boolean
        get() {
            return this == DOWNLOADED
        }
}

enum class FileType {
    VIDEO, UNKNOWN
}