package org.openedx.app.room

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openedx.core.DatabaseManager
import org.openedx.core.instance.StorageNamespaceProvider

class DatabaseManager(
    private val databases: InstanceDatabaseProvider,
    private val namespaces: StorageNamespaceProvider,
) : DatabaseManager {
    override fun clearTables() {
        // Resolved now, not when the coroutine runs: a switch in between must not clear the
        // other site's cache.
        clear(databases.current)
    }

    override fun clearTables(instanceKey: String?) {
        clear(databases.database(namespaces.namespaceFor(instanceKey)))
    }

    private fun clear(database: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            database.courseDao().clearCachedData()
            database.dashboardDao().clearCachedData()
            database.downloadDao().clearOfflineProgress()
            database.discoveryDao().clearCachedData()
        }
    }
}
