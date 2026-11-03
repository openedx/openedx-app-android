package org.openedx.core.instance

import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openedx.core.DatabaseManager
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.system.AppCookieManager
import org.openedx.core.ui.theme.InstanceThemeManager

class InstanceSessionManagerTest {

    private class InMemorySelection(override var selectedInstanceKey: String? = null) : InstanceSelectionStorage

    private fun instance(key: String, color: String = "#123456") = requireNotNull(
        Instance.fromJson(
            JsonObject().apply {
                addProperty("KEY", key)
                addProperty("NAME", key)
                addProperty("COLOR", color)
                addProperty("OAUTH_CLIENT_ID", "$key-client")
                addProperty("API_HOST_URL", "https://$key.example.com")
            }
        )
    )

    private val alpha = instance("alpha", "#3C68FF")
    private val beta = instance("beta", "#1C355E")
    private val store = InstanceStore(InMemorySelection()) { InstancesConfig(listOf(alpha, beta)) }
    private val preferences = mockk<CorePreferences>(relaxed = true)
    private val downloads = mockk<DownloadWorkerController>(relaxed = true)
    private val databaseManager = mockk<DatabaseManager>(relaxed = true)
    private val push = mockk<PushTokenUnregistering>(relaxed = true)
    private val cookies = mockk<AppCookieManager>(relaxed = true)

    private fun manager(pushTokenUnregistrar: PushTokenUnregistering? = push) =
        DefaultInstanceSessionManager(store, preferences, downloads, databaseManager, cookies, pushTokenUnregistrar)

    @After
    fun tearDown() {
        InstanceThemeManager.reset()
    }

    @Test
    fun `switching selects the site and tears nothing down`() = runBlocking {
        manager().switchActiveInstance(beta)

        assertEquals(beta, store.currentInstance)
        assertEquals("beta", InstanceThemeManager.theme.value?.key)
        verify { cookies.invalidateSessionCookie() }
        coVerify(exactly = 0) { preferences.clearCorePreferences() }
        coVerify(exactly = 0) { preferences.clearSession(any()) }
        coVerify(exactly = 0) { downloads.cancelUnfinishedDownloads() }
        verify(exactly = 0) { databaseManager.clearTables() }
    }

    @Test
    fun `logout ends the session in order and keeps cached content`() = runBlocking {
        store.select(alpha)

        manager().logoutCurrentInstance()

        coVerifyOrder {
            downloads.cancelUnfinishedDownloads()
            push.unregisterPushToken("alpha")
            preferences.clearSession("alpha")
        }
        assertNull(store.currentInstance)
        assertNull(InstanceThemeManager.theme.value)
        verify { cookies.clearWebViewCookie() }
        verify(exactly = 0) { databaseManager.clearTables() }
    }

    @Test
    fun `logout completes without push unregistration wired`() = runBlocking {
        store.select(alpha)

        manager(pushTokenUnregistrar = null).logoutCurrentInstance()

        coVerify { preferences.clearSession("alpha") }
        assertNull(store.currentInstance)
    }

    @Test
    fun `clearing the selection happens after the session is cleared`() = runBlocking {
        store.select(alpha)
        var selectedWhileClearing: Instance? = null
        coEvery { preferences.clearSession("alpha") } answers { selectedWhileClearing = store.currentInstance }

        manager().logoutCurrentInstance()

        assertEquals(alpha, selectedWhileClearing)
    }

    @Test
    fun `logging out of another site leaves the active one alone`() = runBlocking {
        store.select(alpha)

        manager().logout(beta)

        coVerify { push.unregisterPushToken("beta") }
        coVerify { preferences.clearSession("beta") }
        coVerify(exactly = 0) { preferences.clearCorePreferences() }
        coVerify(exactly = 0) { downloads.cancelUnfinishedDownloads() }
        assertEquals(alpha, store.currentInstance)
    }

    @Test
    fun `logging out of the active site is a full logout`() = runBlocking {
        store.select(beta)

        manager().logout(beta)

        coVerify { preferences.clearSession("beta") }
        assertNull(store.currentInstance)
    }

    @Test
    fun `switching sites while push unregisters signs out only the site that started it`() = runBlocking {
        val sessions = mutableSetOf("alpha", "beta")
        every { preferences.hasSession(any()) } answers { firstArg<String?>() in sessions }
        coEvery { preferences.clearSession(any()) } answers { sessions.remove(firstArg<String?>()) }
        val unregisterStarted = CompletableDeferred<Unit>()
        val unregisterDone = CompletableDeferred<Unit>()
        coEvery { push.unregisterPushToken("alpha") } coAnswers {
            unregisterStarted.complete(Unit)
            unregisterDone.await()
        }
        val manager = manager()
        manager.switchActiveInstance(alpha)

        val logout = launch { manager.logoutCurrentInstance() }
        unregisterStarted.await()
        manager.switchActiveInstance(beta)
        unregisterDone.complete(Unit)
        logout.join()

        assertFalse(preferences.hasSession("alpha"))
        assertTrue(preferences.hasSession("beta"))
        coVerify(exactly = 0) { preferences.clearSession("beta") }
        coVerify(exactly = 0) { preferences.clearCorePreferences() }
        assertEquals(beta, store.currentInstance)
        assertEquals("beta", InstanceThemeManager.theme.value?.key)
        verify(exactly = 0) { cookies.clearWebViewCookie() }
    }

    @Test
    fun `signing out of another site while it becomes selected leaves the selection alone`() = runBlocking {
        val unregisterStarted = CompletableDeferred<Unit>()
        val unregisterDone = CompletableDeferred<Unit>()
        coEvery { push.unregisterPushToken("beta") } coAnswers {
            unregisterStarted.complete(Unit)
            unregisterDone.await()
        }
        val manager = manager()
        manager.switchActiveInstance(alpha)

        val logout = launch { manager.logout(beta) }
        unregisterStarted.await()
        manager.switchActiveInstance(beta)
        unregisterDone.complete(Unit)
        logout.join()

        coVerify { preferences.clearSession("beta") }
        coVerify(exactly = 0) { downloads.cancelUnfinishedDownloads() }
        assertEquals(beta, store.currentInstance)
        verify(exactly = 0) { cookies.clearWebViewCookie() }
    }

    @Test
    fun `a different learner signing in clears the previous learner's cache on that site`() {
        every { preferences.lastSignedInUsername("beta") } returns "first"

        manager().onSignedIn("beta", "second")

        verify { databaseManager.clearTables("beta") }
        verify { preferences.setLastSignedInUsername("beta", "second") }
    }

    @Test
    fun `the same learner signing back in keeps the cache`() {
        every { preferences.lastSignedInUsername("beta") } returns "first"
        manager().onSignedIn("beta", "first")
        every { preferences.lastSignedInUsername("alpha") } returns ""
        manager().onSignedIn("alpha", "first")

        verify(exactly = 0) { databaseManager.clearTables(any<String>()) }
    }

    @Test
    fun `a sign-out that started on another site finishes there only`() = runBlocking {
        store.select(beta)

        manager().logout("alpha")

        coVerifyOrder {
            push.unregisterPushToken("alpha")
            preferences.clearSession("alpha")
        }
        coVerify(exactly = 0) { preferences.clearCorePreferences() }
        assertEquals(beta, store.currentInstance)
    }

    @Test
    fun `theme follows the selected instance and resets without one`() {
        store.select(beta)
        manager().applyThemeForCurrentInstance()
        assertEquals("#1C355E", InstanceThemeManager.theme.value?.accentColorHex)

        store.select(null)
        manager().applyThemeForCurrentInstance()
        assertNull(InstanceThemeManager.theme.value)
    }
}
