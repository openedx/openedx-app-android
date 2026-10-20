package org.openedx.app.room

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.openedx.core.data.model.room.CourseCalendarStateEntity
import org.openedx.core.data.model.room.OfflineXBlockProgress
import org.openedx.core.data.model.room.XBlockProgressData
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.instance.StorageNamespace
import org.openedx.core.instance.StorageNamespaceProvider
import org.openedx.core.module.db.DownloadModelEntity
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class InstanceDatabaseProviderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val runId = UUID.randomUUID().toString().take(8)

    private fun instance(key: String) = requireNotNull(
        Instance.fromJson(
            JsonObject().apply {
                addProperty("KEY", key)
                addProperty("NAME", key)
                addProperty("OAUTH_CLIENT_ID", "$key-client")
                addProperty("API_HOST_URL", "https://$key.example.com")
            }
        )
    )

    private val alpha = instance("alpha-$runId")
    private val beta = instance("beta-$runId")

    private class Selection(initial: Instance?) : InstanceProvider, StorageNamespaceProvider {
        val flow = MutableStateFlow(initial)
        override val currentInstance: Instance? get() = flow.value
        override val currentInstanceFlow: StateFlow<Instance?> = flow
        override val hasMultipleInstances = true
        override val current: StorageNamespace get() = namespaceFor(flow.value)
        override fun namespaceFor(instance: Instance?) = StorageNamespace(instance?.key)
        override fun namespaceFor(instanceKey: String?) = StorageNamespace(instanceKey)
    }

    private val selection = Selection(alpha)
    private val provider = InstanceDatabaseProvider(context, selection, selection)

    @After
    fun tearDown() {
        listOf(alpha, beta).forEach {
            val name = InstanceDatabaseProvider.databaseName(StorageNamespace(it.key))
            provider.database(StorageNamespace(it.key)).close()
            context.deleteDatabase(name)
        }
    }

    private fun download(id: String, title: String) = DownloadModelEntity(
        id = id,
        title = title,
        courseId = "course-v1:edX+DemoX+Demo_Course",
        size = 1L,
        path = "/tmp/$id",
        url = "https://cdn.example.com/$id.mp4",
        type = "VIDEO",
        downloadedState = "WAITING",
        lastModified = null,
    )

    @Test
    fun legacyNamespaceKeepsTheOriginalDatabaseFile() {
        assertEquals("OpenEdX_db", InstanceDatabaseProvider.databaseName(StorageNamespace.LEGACY))
        val name = InstanceDatabaseProvider.databaseName(StorageNamespace(alpha.key))
        assertTrue(name, name.startsWith("OpenEdX_db_alpha-$runId"))
    }

    @Test
    fun sameIdsOnTwoSitesNeverCollide() = runBlocking {
        val downloads = InstanceScopedDownloadDao(provider)

        downloads.insertDownloadModel(listOf(download("block-1", "Alpha video")))
        downloads.insertOfflineXBlockProgress(
            OfflineXBlockProgress("block-1", "course", XBlockProgressData("https://alpha/handler", "POST", "{}"))
        )
        selection.flow.value = beta
        downloads.insertDownloadModel(listOf(download("block-1", "Beta video")))

        assertEquals(listOf("Beta video"), downloads.readAllData().map { it.title })
        assertNull(downloads.getOfflineXBlockProgress("block-1"))

        downloads.clearOfflineProgress()
        downloads.removeDownloadModel("block-1")
        selection.flow.value = alpha

        assertEquals(listOf("Alpha video"), downloads.readAllData().map { it.title })
        assertEquals("block-1", downloads.getOfflineXBlockProgress("block-1")?.blockId)
    }

    @Test
    fun flowsFollowTheSelectedSite() = runBlocking {
        val downloads = InstanceScopedDownloadDao(provider)
        downloads.insertDownloadModel(listOf(download("a", "Alpha")))
        selection.flow.value = beta
        downloads.insertDownloadModel(listOf(download("b", "Beta")))

        assertEquals(listOf("Beta"), downloads.getAllDataFlow().first().map { it.title })
        selection.flow.value = alpha
        assertEquals(listOf("Alpha"), downloads.getAllDataFlow().first().map { it.title })
        assertEquals(listOf("Alpha"), downloads.readAllDataByIds(listOf("a", "b")).first().map { it.title })
    }

    @Test
    fun pinnedAccessReachesASiteThatIsNotSelected() = runBlocking {
        val downloads = InstanceScopedDownloadDao(provider)
        downloads.insertDownloadModel(listOf(download("a", "Alpha")))
        selection.flow.value = beta

        val alphaRows = provider.downloadDao(StorageNamespace(alpha.key)).readAllData()

        assertEquals(listOf("Alpha"), alphaRows.map { it.title })
        assertTrue(downloads.readAllData().isEmpty())
    }

    @Test
    fun transactionalCleanupOnOneSiteLeavesTheOtherIntact() = runBlocking {
        val calendar = InstanceScopedCalendarDao(provider)
        calendar.insertCourseCalendarStateEntity(CourseCalendarStateEntity("course", 7, true))
        selection.flow.value = beta
        calendar.insertCourseCalendarStateEntity(CourseCalendarStateEntity("course", 9, false))

        calendar.clearCachedData()
        assertTrue(calendar.readAllCourseCalendarState().isEmpty())

        selection.flow.value = alpha
        assertEquals(7, calendar.readCourseCalendarStateById("course")?.checksum)
    }
}
