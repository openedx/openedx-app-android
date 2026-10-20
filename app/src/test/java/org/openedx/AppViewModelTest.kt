package org.openedx

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.app.AppAnalytics
import org.openedx.app.AppViewModel
import org.openedx.app.data.storage.PreferencesManager
import org.openedx.app.deeplink.DeepLinkRouter
import org.openedx.app.system.push.PushRegistrations
import org.openedx.core.CoreMocks
import org.openedx.core.config.Config
import org.openedx.core.config.FirebaseConfig
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceConfigLoader
import org.openedx.core.instance.InstanceSelectionStorage
import org.openedx.core.instance.InstanceSessionManager
import org.openedx.core.instance.InstanceStore
import org.openedx.core.instance.InstancesConfig
import org.openedx.core.instance.LegacySessionAdoptionStorage
import org.openedx.core.instance.StorageNamespace
import org.openedx.core.instance.StorageNamespaceProvider
import org.openedx.core.system.notifier.DownloadNotifier
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.LogoutEvent
import org.openedx.foundation.system.ResourceManager
import org.openedx.foundation.utils.FileUtil

@ExperimentalCoroutinesApi
class AppViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher() // UnconfinedTestDispatcher()

    private val config = mockk<Config>()
    private val notifier = mockk<AppNotifier>()
    private val sessionManager = mockk<InstanceSessionManager>(relaxed = true)
    private val instanceStore = mockk<InstanceStore>(relaxed = true)
    private val instanceConfigLoader = mockk<InstanceConfigLoader>()
    private val pushRegistrations = mockk<PushRegistrations>(relaxed = true)
    private val namespaces = mockk<StorageNamespaceProvider>(relaxed = true)
    private val legacySessionAdoption = object : LegacySessionAdoptionStorage {
        override var hasCheckedLegacySession = false
    }
    private val preferencesManager = mockk<PreferencesManager>()
    private val analytics = mockk<AppAnalytics>()
    private val fileUtil = mockk<FileUtil>()
    private val deepLinkRouter = mockk<DeepLinkRouter>()
    private val context = mockk<Context>()
    private val downloadNotifier = mockk<DownloadNotifier>()
    private val resourceManager = mockk<ResourceManager>()

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
        every { downloadNotifier.notifier } returns flow { }
        coEvery { instanceConfigLoader.load() } returns InstancesConfig()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun setIdSuccess() = runTest {
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { preferencesManager.user } returns CoreMocks.mockUser
        every { notifier.notifier } returns flow { }
        every { preferencesManager.canResetAppDirectory } returns false
        every { preferencesManager.pushToken } returns ""

        val viewModel = AppViewModel(
            config,
            notifier,
            preferencesManager,
            sessionManager,
            instanceStore,
            instanceConfigLoader,
            pushRegistrations,
            namespaces,
            legacySessionAdoption,
            analytics,
            deepLinkRouter,
            fileUtil,
            downloadNotifier,
            context,
            resourceManager,
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        advanceUntilIdle()

        verify(exactly = 1) { analytics.setUserIdForSession(any()) }
    }

    @Test
    fun `routing waits for the instance catalog`() = runTest {
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { preferencesManager.user } returns CoreMocks.mockUser
        every { notifier.notifier } returns flow { }
        every { preferencesManager.canResetAppDirectory } returns false
        every { preferencesManager.pushToken } returns ""
        val catalog = InstancesConfig()
        coEvery { instanceConfigLoader.load() } returns catalog

        val viewModel = AppViewModel(
            config,
            notifier,
            preferencesManager,
            sessionManager,
            instanceStore,
            instanceConfigLoader,
            pushRegistrations,
            namespaces,
            legacySessionAdoption,
            analytics,
            deepLinkRouter,
            fileUtil,
            downloadNotifier,
            context,
            resourceManager,
        )
        assert(!viewModel.isInstanceCatalogResolved.value)
        advanceUntilIdle()

        assert(viewModel.isInstanceCatalogResolved.value)
        coVerifyOrder {
            instanceStore.updateInstancesConfig(catalog)
            sessionManager.applyThemeForCurrentInstance()
        }
    }

    private fun site(key: String) = requireNotNull(
        Instance.fromJson(
            JsonObject().apply {
                addProperty("KEY", key)
                addProperty("NAME", key)
                addProperty("OAUTH_CLIENT_ID", "$key-client")
                addProperty("API_HOST_URL", "https://$key.example.com")
            }
        )
    )

    private fun launch(store: InstanceStore): AppViewModel {
        every { notifier.notifier } returns flow { }
        every { namespaces.namespaceFor(any<Instance>()) } answers {
            val instance = firstArg<Instance>()
            if (instance.key == "legacy") StorageNamespace.LEGACY else StorageNamespace(instance.key)
        }
        coEvery { instanceConfigLoader.load() } returns store.instancesConfig
        return AppViewModel(
            config, notifier, preferencesManager, sessionManager, store, instanceConfigLoader, pushRegistrations,
            namespaces, legacySessionAdoption, analytics, deepLinkRouter, fileUtil, downloadNotifier,
            context, resourceManager,
        )
    }

    private fun storeOf(vararg sites: Instance) = InstanceStore(
        object : InstanceSelectionStorage {
            override var selectedInstanceKey: String? = null
        }
    ) { InstancesConfig(sites.toList()) }

    @Test
    fun `first multi-site launch opens the site an upgraded learner was signed in to`() = runTest {
        val legacy = site("legacy")
        val store = storeOf(site("other"), legacy)
        every { preferencesManager.hasSession(any()) } answers { firstArg<String>() == "legacy" }

        launch(store)
        advanceUntilIdle()

        assert(store.currentInstance == legacy)
        assert(legacySessionAdoption.hasCheckedLegacySession)
    }

    @Test
    fun `the legacy session is adopted only on the first launch`() = runTest {
        val store = storeOf(site("other"), site("legacy"))
        every { preferencesManager.hasSession(any()) } returns true
        legacySessionAdoption.hasCheckedLegacySession = true

        launch(store)
        advanceUntilIdle()

        assert(store.currentInstance == null)
    }

    @Test
    fun `a saved site without a session is cleared so launch starts at site selection`() = runTest {
        val other = site("other")
        val store = storeOf(other, site("legacy"))
        store.select(other)
        every { preferencesManager.hasSession(any()) } returns false

        launch(store)
        advanceUntilIdle()

        assert(store.currentInstance == null)
    }

    @Test
    fun forceLogout() = runTest {
        every { notifier.notifier } returns flow {
            emit(LogoutEvent(true))
        }
        coEvery { sessionManager.logoutCurrentInstance() } returns Unit
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { preferencesManager.user } returns CoreMocks.mockUser
        every { analytics.logoutEvent(true) } returns Unit
        every { preferencesManager.canResetAppDirectory } returns false
        every { preferencesManager.pushToken } returns ""
        every { config.getFirebaseConfig() } returns FirebaseConfig()

        val viewModel = AppViewModel(
            config,
            notifier,
            preferencesManager,
            sessionManager,
            instanceStore,
            instanceConfigLoader,
            pushRegistrations,
            namespaces,
            legacySessionAdoption,
            analytics,
            deepLinkRouter,
            fileUtil,
            downloadNotifier,
            context,
            resourceManager,
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        advanceUntilIdle()

        verify(exactly = 1) { analytics.logoutEvent(true) }
        assert(viewModel.logoutUser.value != null)
    }

    @Test
    fun forceLogoutTwice() = runTest {
        every { notifier.notifier } returns flow {
            emit(LogoutEvent(true))
            emit(LogoutEvent(true))
        }
        coEvery { sessionManager.logoutCurrentInstance() } returns Unit
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { preferencesManager.user } returns CoreMocks.mockUser
        every { analytics.logoutEvent(true) } returns Unit
        every { preferencesManager.canResetAppDirectory } returns false
        every { preferencesManager.pushToken } returns ""
        every { config.getFirebaseConfig() } returns FirebaseConfig()

        val viewModel = AppViewModel(
            config,
            notifier,
            preferencesManager,
            sessionManager,
            instanceStore,
            instanceConfigLoader,
            pushRegistrations,
            namespaces,
            legacySessionAdoption,
            analytics,
            deepLinkRouter,
            fileUtil,
            downloadNotifier,
            context,
            resourceManager,
        )

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        advanceUntilIdle()

        verify(exactly = 1) { analytics.logoutEvent(true) }
        coVerify(exactly = 1) { sessionManager.logoutCurrentInstance() }
        verify(exactly = 1) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { preferencesManager.user }
        verify(exactly = 1) { analytics.logoutEvent(true) }
    }
}
