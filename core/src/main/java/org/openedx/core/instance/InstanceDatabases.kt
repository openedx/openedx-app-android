package org.openedx.core.instance

import org.openedx.core.module.db.DownloadDao

/**
 * Per-instance database access for work that outlives a screen, such as the download worker:
 * it handles each queued file in the database of the site that queued it, whichever site is
 * selected by the time the file finishes.
 */
interface InstanceDatabases {
    fun downloadDao(namespace: StorageNamespace): DownloadDao
}
