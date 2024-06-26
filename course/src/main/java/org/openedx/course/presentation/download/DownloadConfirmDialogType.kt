package org.openedx.course.presentation.download

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class DownloadConfirmDialogType : Parcelable {
    DOWNLOAD_ON_CELLULAR, CONFIRM, REMOVE
}
