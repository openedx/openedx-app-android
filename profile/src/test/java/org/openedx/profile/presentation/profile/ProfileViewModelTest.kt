package org.openedx.profile.presentation.profile

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.R
import org.openedx.core.config.Config
import org.openedx.core.domain.model.AgreementUrls
import org.openedx.core.domain.model.ProfileImage
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.system.notifier.account.AccountUpdated
import org.openedx.profile.system.notifier.profile.ProfileNotifier
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>()
    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<ProfileInteractor>()
    private val notifier = mockk<ProfileNotifier>()
    private val analytics = mockk<ProfileAnalytics>()
    private val router = mockk<ProfileRouter>()

    private val account = org.openedx.profile.domain.model.Account(
        username = "",
        bio = "",
        requiresParentalConsent = false,
        name = "",
        country = "",
        isActive = true,
        profileImage = ProfileImage("", "", "", "", false),
        yearOfBirth = 2000,
        levelOfEducation = "",
        goals = "",
        languageProficiencies = emptyList(),
        gender = "",
        mailingAddress = "",
        email = "",
        dateJoined = null,
        accountPrivacy = org.openedx.profile.domain.model.Account.Privacy.PRIVATE
    )

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { config.isPreLoginExperienceEnabled() } returns false
        every { config.getFeedbackEmailAddress() } returns ""
        every { config.getAgreement(Locale.current.language) } returns AgreementUrls()
        every { config.getFaqUrl() } returns ""
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAccount no internetConnection and cache is null`() = runTest {
        val viewModel = ProfileViewModel(
            interactor,
            resourceManager,
            notifier,
            analytics,
            router
        )
        coEvery { interactor.getCachedAccount() } returns null
        coEvery { interactor.getAccount() } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAccount() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(viewModel.uiState.value is ProfileUIState.Loading)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `getAccount no internetConnection and cache is not null`() = runTest {
        val viewModel = ProfileViewModel(
            interactor,
            resourceManager,
            notifier,
            analytics,
            router
        )
        coEvery { interactor.getCachedAccount() } returns account
        coEvery { interactor.getAccount() } throws UnknownHostException()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAccount() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(viewModel.uiState.value is ProfileUIState.Data)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `getAccount unknown exception`() = runTest {
        val viewModel = ProfileViewModel(
            interactor,
            resourceManager,
            notifier,
            analytics,
            router
        )
        coEvery { interactor.getCachedAccount() } returns null
        coEvery { interactor.getAccount() } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAccount() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(viewModel.uiState.value is ProfileUIState.Loading)
        assertEquals(somethingWrong, message?.message)
    }

    @Test
    fun `getAccount success`() = runTest {
        val viewModel = ProfileViewModel(
            interactor,
            resourceManager,
            notifier,
            analytics,
            router
        )
        coEvery { interactor.getCachedAccount() } returns null
        coEvery { interactor.getAccount() } returns account
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getAccount() }

        assert(viewModel.uiState.value is ProfileUIState.Data)
        assert(viewModel.uiMessage.value == null)
    }

    @Test
    fun `AccountUpdated notifier test`() = runTest {
        val viewModel = ProfileViewModel(
            interactor,
            resourceManager,
            notifier,
            analytics,
            router
        )
        coEvery { interactor.getCachedAccount() } returns null
        every { notifier.notifier } returns flow { emit(AccountUpdated()) }
        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getAccount() }
    }
}
