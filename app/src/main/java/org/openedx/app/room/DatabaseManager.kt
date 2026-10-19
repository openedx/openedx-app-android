package org.openedx.app.room

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openedx.core.DatabaseManager

class DatabaseManager(
    private val databases: InstanceDatabaseProvider,
) : DatabaseManager {
    override fun clearTables() {
        // Resolved now, not when the coroutine runs: a switch in between must not clear the
        // other site's cache.
        val database = databases.current
        CoroutineScope(Dispatchers.IO).launch {
            database.courseDao().clearCachedData()
            database.dashboardDao().clearCachedData()
            database.downloadDao().clearOfflineProgress()
            database.discoveryDao().clearCachedData()
        }
    }
}
