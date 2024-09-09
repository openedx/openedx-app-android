package org.openedx.course.presentation.download

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class DownloadErrorDialogType : Parcelable {
    NO_CONNECTION, WIFI_REQUIRED, DOWNLOAD_FAILED
}
