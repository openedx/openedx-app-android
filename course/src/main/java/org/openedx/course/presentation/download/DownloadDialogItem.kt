package org.openedx.course.presentation.download

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadDialogItem(
    val title: String,
    val size: Long
) : Parcelable
