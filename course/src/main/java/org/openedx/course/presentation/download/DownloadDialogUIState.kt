package org.openedx.course.presentation.download

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadDialogUIState(
    val downloadDialogItems: List<DownloadDialogItem> = emptyList(),
    val sizeSum: Long,
    val isAllBlocksDownloaded: Boolean,
    val removeDownloadModels: () -> Unit,
    val saveDownloadModels: () -> Unit
) : Parcelable