package org.openedx.core.system.notifier

data class DownloadProgressChanged(
    val value: Long, val size: Long
) : DownloadEvent
