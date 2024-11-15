package org.openedx.core.module.download

data class DownloadModelsSize(
    val isAllBlocksDownloadedOrDownloading: Boolean,
    val remainingCount: Int,
    val remainingSize: Long,
    val allCount: Int,
    val allSize: Long
)
