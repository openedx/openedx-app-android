package org.openedx.app.system.push

import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.openedx.app.data.api.NotificationsApi
import org.openedx.core.config.Config
import org.openedx.core.config.FirebaseConfig
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceSelectionStorage
import org.openedx.core.instance.InstanceStore
import org.openedx.core.instance.InstancesConfig
import org.openedx.core.instance.RequestInstance
import java.io.IOException

class PushRegistrationsTest {

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

    private val alpha = site("alpha")
    private val beta = site("beta")
    private val api = mockk<NotificationsApi>(relaxed = true)
    private val preferences = mockk<CorePreferences> {
        every { pushToken } returns "device-token"
        every { hasSession(any()) } answers { firstArg<String?>() in setOf("alpha", "beta") }
    }
    private val config = mockk<Config> {
        every { getFirebaseConfig() } returns FirebaseConfig(isCloudMessagingEnabled = true)
    }

    private fun store(vararg sites: Instance) = InstanceStore(
        object : InstanceSelectionStorage {
            override var selectedInstanceKey: String? = null
        }
    ) { InstancesConfig(sites.toList()) }

    @Test
    fun `unregistering targets the site it is for, not the selected one`() = runBlocking {
        val store = store(alpha, beta)
        store.select(beta)

        PushRegistrations(api, preferences, config, store).unregisterPushToken("alpha")

        coVerify { api.syncFirebaseToken("device-token", active = false, instance = RequestInstance("alpha")) }
        coVerify(exactly = 0) { api.syncFirebaseToken(any(), any(), RequestInstance("beta")) }
    }

    @Test
    fun `an unreachable site does not fail the sign-out`() = runBlocking {
        coEvery { api.syncFirebaseToken(any(), any(), any()) } throws IOException("offline")

        PushRegistrations(api, preferences, config, store(alpha)).unregisterPushToken("alpha")
    }

    @Test
    fun `registering is pinned to its site and skipped without a session`() = runBlocking {
        val registrations = PushRegistrations(api, preferences, config, store(alpha, beta, site("gamma")))

        registrations.register("beta")
        registrations.register("gamma")

        coVerify { api.syncFirebaseToken("device-token", active = true, instance = RequestInstance("beta")) }
        coVerify(exactly = 0) { api.syncFirebaseToken(any(), any(), RequestInstance("gamma")) }
    }

    @Test
    fun `nothing is sent when cloud messaging is off`() = runBlocking {
        every { config.getFirebaseConfig() } returns FirebaseConfig(isCloudMessagingEnabled = false)

        PushRegistrations(api, preferences, config, store(alpha)).unregisterPushToken("alpha")

        coVerify(exactly = 0) { api.syncFirebaseToken(any(), any(), any()) }
    }

    @Test
    fun `signed in sites come from the catalog, or the single-site session without one`() {
        assertEquals(
            listOf("alpha", "beta"),
            PushRegistrations(api, preferences, config, store(alpha, site("gamma"), beta)).signedInSites()
        )
        every { preferences.hasSession(null) } returns true
        assertEquals(listOf<String?>(null), PushRegistrations(api, preferences, config, store()).signedInSites())
    }
}
