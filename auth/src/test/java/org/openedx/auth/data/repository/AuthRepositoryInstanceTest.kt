package org.openedx.auth.data.repository

import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.openedx.auth.data.api.AuthApi
import org.openedx.auth.data.model.AuthResponse
import org.openedx.core.config.Config
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.instance.InstanceSessionManager
import org.openedx.core.instance.InstanceSwitchedException
import org.openedx.core.instance.RequestInstance

class AuthRepositoryInstanceTest {

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

    private val alpha = site("alpha")
    private val beta = site("beta")
    private val selection = Selection(alpha)
    private val api = mockk<AuthApi>()
    private val preferences = mockk<CorePreferences>(relaxed = true)
    private val sessionManager = mockk<InstanceSessionManager>(relaxed = true)
    private val config = mockk<Config> {
        every { getOAuthClientId() } returns "alpha-client"
        every { getAccessTokenType() } returns "JWT"
    }
    private val tokens = AuthResponse("access-a", "JWT", 3600, null, null, "refresh-a")
    private val learner = User(1, "learner_a", "a@example.com", "Learner A")

    private fun repository() = AuthRepository(config, api, preferences, sessionManager, selection)

    @Test
    fun `sign-in pins every request and write to the site it started on`() = runTest {
        coEvery {
            api.getAccessToken(any(), any(), any(), any(), any(), any(), RequestInstance("alpha"))
        } returns tokens
        coEvery { api.getProfile(RequestInstance("alpha")) } returns learner

        repository().login("learner_a", "secret")

        verify { preferences.saveTokens("alpha", "access-a", "refresh-a", any()) }
        verify { preferences.saveUser("alpha", learner) }
        verify { sessionManager.onSignedIn("alpha", "learner_a") }
    }

    @Test
    fun `a sign-in answer that arrives after another site was selected writes nothing`() = runTest {
        coEvery { api.getAccessToken(any(), any(), any(), any(), any(), any(), any()) } answers {
            selection.flow.value = beta
            tokens
        }

        try {
            repository().login("learner_a", "secret")
            fail("the sign-in must not complete on another site")
        } catch (e: InstanceSwitchedException) {
            assertEquals("alpha", e.instanceKey)
        }

        verify(exactly = 0) { preferences.saveTokens(any(), any(), any(), any()) }
        verify(exactly = 0) { preferences.saveUser(any(), any()) }
        verify(exactly = 0) { sessionManager.onSignedIn(any(), any()) }
        coVerify(exactly = 0) { api.getProfile(any()) }
    }

    @Test
    fun `a switch while the profile loads leaves the new site without a user`() = runTest {
        coEvery { api.getAccessToken(any(), any(), any(), any(), any(), any(), any()) } returns tokens
        coEvery { api.getProfile(RequestInstance("alpha")) } answers {
            selection.flow.value = beta
            learner
        }

        try {
            repository().login("learner_a", "secret")
            fail("the sign-in must not complete on another site")
        } catch (_: InstanceSwitchedException) {
            // expected
        }

        verify(exactly = 0) { preferences.saveUser(any(), any()) }
        verify(exactly = 0) { preferences.saveTokens("beta", any(), any(), any()) }
    }
}
