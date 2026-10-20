package org.openedx.profile.data.repository

import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.instance.InstanceSessionManager
import org.openedx.core.instance.RequestInstance
import org.openedx.profile.data.api.ProfileApi

class ProfileRepositoryLogoutTest {

    private class Selection(instance: Instance?) : InstanceProvider {
        val flow = MutableStateFlow(instance)
        override val currentInstance: Instance? get() = flow.value
        override val currentInstanceFlow: StateFlow<Instance?> = flow
        override val hasMultipleInstances = true
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

    private val selection = Selection(site("alpha"))
    private val api = mockk<ProfileApi>()
    private val preferences = mockk<CorePreferences>(relaxed = true) {
        every { refreshToken("alpha") } returns "refresh-a"
    }
    private val sessionManager = mockk<InstanceSessionManager>(relaxed = true)
    private val config = mockk<Config> { every { getOAuthClientId() } returns "alpha-client" }

    private fun repository() = ProfileRepository(
        config,
        api,
        mockk(relaxed = true),
        preferences,
        sessionManager,
        selection
    )

    @Test
    fun `a sign-out that was in flight during a switch signs out only the site it started on`() = runTest {
        coEvery { api.revokeAccessToken(any(), any(), any(), any()) } answers {
            selection.flow.value = site("beta")
            "".toResponseBody()
        }

        repository().logout()

        coVerify { api.revokeAccessToken("alpha-client", "refresh-a", any(), RequestInstance("alpha")) }
        coVerify { sessionManager.logout("alpha") }
        coVerify(exactly = 0) { sessionManager.logoutCurrentInstance() }
        coVerify(exactly = 0) { sessionManager.logout("beta") }
    }

    @Test
    fun `a failed revoke still signs the site out`() = runTest {
        coEvery { api.revokeAccessToken(any(), any(), any(), any()) } throws java.io.IOException("offline")

        runCatching { repository().logout() }

        coVerify { sessionManager.logout("alpha") }
    }
}
