package org.openedx.course.presentation.download

import android.os.Parcelable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class DownloadDialogItem(
    val title: String,
    val size: Long,
    val icon: @RawValue ImageVector? = null
) : Parcelable
