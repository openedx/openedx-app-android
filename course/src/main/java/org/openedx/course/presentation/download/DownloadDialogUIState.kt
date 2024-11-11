package org.openedx.course.presentation.download

import android.os.Parcelable
import androidx.fragment.app.FragmentManager
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class DownloadDialogUIState(
    val downloadDialogItems: List<DownloadDialogItem> = emptyList(),
    val sizeSum: Long,
    val isAllBlocksDownloaded: Boolean,
    val isDownloadFailed: Boolean,
    val fragmentManager: @RawValue FragmentManager,
    val removeDownloadModels: () -> Unit,
    val saveDownloadModels: () -> Unit
) : Parcelable
