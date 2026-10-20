package org.openedx.auth.presentation.learningsites

import androidx.fragment.app.FragmentManager
import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.openedx.auth.presentation.AuthRouter
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceSelectionStorage
import org.openedx.core.instance.InstanceSessionManager
import org.openedx.core.instance.InstanceStore
import org.openedx.core.instance.InstancesConfig

@OptIn(ExperimentalCoroutinesApi::class)
class LearningSitesViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class InMemorySelection(override var selectedInstanceKey: String? = null) : InstanceSelectionStorage

    private fun site(key: String, name: String) = requireNotNull(
        Instance.fromJson(
            JsonObject().apply {
                addProperty("KEY", key)
                addProperty("NAME", name)
                addProperty("OAUTH_CLIENT_ID", "$key-client")
                addProperty("API_HOST_URL", "https://$key.example.com")
            }
        )
    )

    private val zeta = site("zeta", "Zeta University")
    private val alpha = site("alpha", "Alpha College")
    private val beta = site("beta", "Beta Institute")
    private val store = InstanceStore(InMemorySelection()) { InstancesConfig(listOf(zeta, alpha, beta)) }
    private val preferences = mockk<CorePreferences>()
    private val sessionManager = mockk<InstanceSessionManager>(relaxed = true)
    private val router = mockk<AuthRouter>(relaxed = true)
    private val config = mockk<Config> { every { isPreLoginExperienceEnabled() } returns false }
    private val fragmentManager = mockk<FragmentManager>(relaxed = true)
    private var signedIn = setOf<String>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { preferences.hasSession(any()) } answers { firstArg<String>() in signedIn }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(isSwitching: Boolean) = LearningSitesViewModel(
        isSwitching = isSwitching,
        instanceStore = store,
        preferences = preferences,
        sessionManager = sessionManager,
        router = router,
        config = config,
        dispatcher = dispatcher,
    )

    @Test
    fun `sites are grouped by session and sorted by name`() = runTest {
        signedIn = setOf("zeta", "beta")
        store.select(beta)

        val state = viewModel(isSwitching = true).uiState.value

        assertEquals(beta, state.currentSite)
        assertEquals(listOf(zeta), state.otherSignedInSites)
        assertEquals(listOf(alpha), state.addableSites)
    }

    @Test
    fun `a selected site without a session is not shown as current`() = runTest {
        store.select(alpha)

        val state = viewModel(isSwitching = false).uiState.value

        assertNull(state.currentSite)
        assertEquals(listOf(alpha, beta, zeta), state.addableSites)
    }

    @Test
    fun `search filters addable sites by name or host`() = runTest {
        val viewModel = viewModel(isSwitching = false)

        viewModel.onSearchQueryChanged("institute")
        assertEquals(listOf(beta), viewModel.uiState.value.addableSites)
        viewModel.onSearchQueryChanged("zeta.example")
        assertEquals(listOf(zeta), viewModel.uiState.value.addableSites)
        viewModel.onSearchQueryChanged("nothing like it")
        assertEquals(emptyList<Instance>(), viewModel.uiState.value.addableSites)
    }

    @Test
    fun `choosing a signed in site opens its dashboard without signing in again`() = runTest {
        signedIn = setOf("alpha", "beta")
        store.select(beta)
        val viewModel = viewModel(isSwitching = true)

        viewModel.select(fragmentManager, alpha)

        verifyOrder {
            router.clearBackStack(fragmentManager)
            fragmentManager.executePendingTransactions()
            sessionManager.switchActiveInstance(alpha)
            router.navigateToMain(fragmentManager, null, null)
        }
        verify(exactly = 0) { router.navigateToSignIn(any(), any(), any()) }
    }

    @Test
    fun `choosing a new site on first launch opens its sign in over the list`() = runTest {
        val viewModel = viewModel(isSwitching = false)

        viewModel.select(fragmentManager, zeta)

        verify { sessionManager.switchActiveInstance(zeta) }
        verify { router.navigateToSignIn(fragmentManager, null, null) }
        verify(exactly = 0) { router.clearBackStack(any()) }
    }

    @Test
    fun `switching to a new site starts over with the list under its sign in`() = runTest {
        signedIn = setOf("beta")
        store.select(beta)
        val viewModel = viewModel(isSwitching = true)

        viewModel.select(fragmentManager, alpha)

        verifyOrder {
            router.clearBackStack(fragmentManager)
            sessionManager.switchActiveInstance(alpha)
            router.navigateToLearningSites(fragmentManager, isSwitching = false)
            router.navigateToSignIn(fragmentManager, null, null)
        }
    }

    @Test
    fun `a double tap starts only one navigation`() = runTest {
        val viewModel = viewModel(isSwitching = false)

        viewModel.select(fragmentManager, zeta)
        viewModel.select(fragmentManager, alpha)

        verify(exactly = 1) { sessionManager.switchActiveInstance(any()) }
    }

    @Test
    fun `logging out of another site only refreshes the list`() = runTest {
        signedIn = setOf("alpha", "beta")
        store.select(beta)
        coEvery { sessionManager.logout(alpha) } answers { signedIn = setOf("beta") }
        val viewModel = viewModel(isSwitching = true)

        viewModel.logOut(fragmentManager, alpha)

        coVerify { sessionManager.logout(alpha) }
        verify(exactly = 0) { router.navigateToLearningSites(any(), any()) }
        assertEquals(emptyList<Instance>(), viewModel.uiState.value.otherSignedInSites)
        assertEquals(listOf(alpha, zeta), viewModel.uiState.value.addableSites)
    }

    @Test
    fun `logging out of the current site starts over at site selection`() = runTest {
        signedIn = setOf("beta")
        store.select(beta)
        val viewModel = viewModel(isSwitching = true)

        viewModel.logOut(fragmentManager, beta)

        coVerify { sessionManager.logout(beta) }
        verify { router.navigateToLearningSites(fragmentManager, isSwitching = false) }
    }

    @Test
    fun `no site can be picked while a sign-out is still in progress`() = runTest {
        signedIn = setOf("alpha", "beta")
        store.select(alpha)
        val signOutDone = CompletableDeferred<Unit>()
        coEvery { sessionManager.logout(alpha) } coAnswers {
            signOutDone.await()
            signedIn = setOf("beta")
        }
        val viewModel = viewModel(isSwitching = true)

        viewModel.logOut(fragmentManager, alpha)
        viewModel.select(fragmentManager, beta)
        viewModel.logOut(fragmentManager, beta)

        verify(exactly = 0) { sessionManager.switchActiveInstance(any()) }
        coVerify(exactly = 0) { sessionManager.logout(beta) }

        signOutDone.complete(Unit)

        verify { router.navigateToLearningSites(fragmentManager, isSwitching = false) }
    }

    @Test
    fun `sites can be picked again once a sign-out of another site finishes`() = runTest {
        signedIn = setOf("alpha", "beta")
        store.select(beta)
        val signOutDone = CompletableDeferred<Unit>()
        coEvery { sessionManager.logout(alpha) } coAnswers {
            signOutDone.await()
            signedIn = setOf("beta")
        }
        val viewModel = viewModel(isSwitching = true)

        viewModel.logOut(fragmentManager, alpha)
        signOutDone.complete(Unit)
        viewModel.select(fragmentManager, beta)

        verify(exactly = 1) { sessionManager.switchActiveInstance(beta) }
    }
}
