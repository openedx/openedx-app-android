package org.openedx.core.system.notifier

data class DownloadProgressChanged(
    val id: String,
    val value: Long,
    val size: Long
) : DownloadEvent
