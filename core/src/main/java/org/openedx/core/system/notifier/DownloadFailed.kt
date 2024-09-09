package org.openedx.core.system.notifier

import org.openedx.core.module.db.DownloadModel

data class DownloadFailed(
    val downloadModel: List<DownloadModel>
) : DownloadEvent
