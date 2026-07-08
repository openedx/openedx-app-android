package org.openedx.auth.presentation.lmsselection

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.lmsdirectory.DirectoryConfig
import org.openedx.core.lmsdirectory.LmsDetail
import org.openedx.core.lmsdirectory.LmsDirectoryRepository
import org.openedx.core.lmsdirectory.LmsSummary
import org.openedx.core.lmsdirectory.LmsThemeController
import org.openedx.foundation.system.ResourceManager

/**
 * Covers the QR path: scanning a platform's URL resolves it against the registry and
 * emits the routing signal (sign-in vs pre-login Discovery) that matches the LMS's
 * settings — the behavior that replaced the old "prefill the search box" QR flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SiteSelectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val corePreferences = mockk<CorePreferences>(relaxed = true)
    private val resourceManager = mockk<ResourceManager>(relaxed = true)
    private val config = mockk<Config>(relaxed = true)
    private val repository = mockk<LmsDirectoryRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.fetchConfig() } returns DirectoryConfig.SEARCH_DEFAULT
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        LmsThemeController.clear()
    }

    @Test
    fun `onUrlScanned resolves registry detail and routes to discovery`() = runTest(dispatcher) {
        val actions = givenScan(preLoginDiscovery = true)
        assertEquals(1, actions.size)
        val success = actions.first() as SiteSelectionViewModel.SiteSelectionAction.Success
        assertTrue("Discovery LMS must route to pre-login Discovery", success.preLoginDiscovery)
        // The scanned platform got its full record + brand, not a bare URL.
        coVerify { repository.fetchDetail("4") }
    }

    @Test
    fun `onUrlScanned resolves registry detail and routes to sign-in`() = runTest(dispatcher) {
        val actions = givenScan(preLoginDiscovery = false)
        assertEquals(1, actions.size)
        val success = actions.first() as SiteSelectionViewModel.SiteSelectionAction.Success
        assertTrue("Non-discovery LMS must route to sign-in", !success.preLoginDiscovery)
    }

    private fun kotlinx.coroutines.test.TestScope.givenScan(
        preLoginDiscovery: Boolean,
    ): List<SiteSelectionViewModel.SiteSelectionAction> {
        val summary = LmsSummary(
            id = "4",
            title = "Sandbox Env",
            shortDescription = "",
            baseUrl = "https://sandbox.openedx.org",
            logoUrl = null,
            accentColor = "#6a2e7b",
        )
        val detail = LmsDetail(
            id = "4",
            title = "Sandbox Env",
            shortDescription = "",
            baseUrl = "https://sandbox.openedx.org",
            logoUrl = null,
            accentColor = "#6a2e7b",
            oauthClientId = "client-id",
            feedbackEmail = null,
            loginBackgroundUrl = null,
            preLoginDiscovery = preLoginDiscovery,
        )
        coEvery { repository.search("sandbox.openedx.org") } returns Result.success(listOf(summary))
        coEvery { repository.fetchDetail("4") } returns Result.success(detail)

        val viewModel = SiteSelectionViewModel(corePreferences, resourceManager, config, repository)
        val actions = mutableListOf<SiteSelectionViewModel.SiteSelectionAction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.actions.toList(actions)
        }

        viewModel.onUrlScanned("https://sandbox.openedx.org")
        advanceUntilIdle()
        return actions
    }
}
