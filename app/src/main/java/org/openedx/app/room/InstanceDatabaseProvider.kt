package org.openedx.app.room

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.openedx.core.instance.InstanceDatabases
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.instance.StorageNamespace
import org.openedx.core.instance.StorageNamespaceProvider
import org.openedx.core.module.db.DownloadDao

/**
 * One Room database file per instance, so cached courses, offline progress and download
 * records can't collide between sites, even when two sites use the same course and block IDs.
 *
 * The legacy namespace keeps the original `OpenEdX_db` file, so an upgraded single-site install
 * keeps its data without a migration. Databases open lazily and stay open for the process
 * lifetime: a catalog lists a handful of sites, and closing one could break a flow still
 * collecting from it.
 */
class InstanceDatabaseProvider(
    private val context: Context,
    instanceProvider: InstanceProvider,
    private val namespaces: StorageNamespaceProvider,
) : InstanceDatabases {

    private val databases = HashMap<StorageNamespace, AppDatabase>()

    /** The selected instance's database. Resolve it per operation; don't hold on to it. */
    val current: AppDatabase
        get() = database(namespaces.current)

    /** Emits the selected instance's database, again on every switch. */
    val currentFlow: Flow<AppDatabase> = instanceProvider.currentInstanceFlow
        .map { database(namespaces.namespaceFor(it)) }
        .distinctUntilChanged()

    fun database(namespace: StorageNamespace): AppDatabase = synchronized(databases) {
        databases.getOrPut(namespace) { build(namespace) }
    }

    override fun downloadDao(namespace: StorageNamespace): DownloadDao = database(namespace).downloadDao()

    private fun build(namespace: StorageNamespace): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, databaseName(namespace))
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    companion object {
        fun databaseName(namespace: StorageNamespace): String {
            return namespace.fileSegment?.let { "${DATABASE_NAME}_$it" } ?: DATABASE_NAME
        }
    }
}
