package com.raccoongang.profile.presentation.edit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.Account
import com.raccoongang.core.domain.model.ProfileImage
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.profile.domain.interactor.ProfileInteractor
import com.raccoongang.profile.system.notifier.AccountUpdated
import com.raccoongang.profile.system.notifier.ProfileNotifier
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.io.File
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<ProfileInteractor>()
    private val notifier = mockk<ProfileNotifier>()

    private val account = Account(
        username = "thom84",
        bio = "He as compliment unreserved projecting. Between had observe pretend delight for believe. Do newspaper questions consulted sweetness do. Our sportsman his unwilling fulfilled departure law.",
        requiresParentalConsent = true,
        name = "Thomas",
        country = "Ukraine",
        isActive = true,
        profileImage = ProfileImage("", "", "", "", false),
        yearOfBirth = 2000,
        levelOfEducation = "Bachelor",
        goals = "130",
        languageProficiencies = emptyList(),
        gender = "male",
        mailingAddress = "",
        "",
        null,
        accountPrivacy = Account.Privacy.ALL_USERS
    )
    private val file = mockk<File>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateAccount no internet connection`() = runTest {
        val viewModel = EditProfileViewModel(interactor, resourceManager, notifier, account)
        coEvery { interactor.updateAccount(any()) } throws UnknownHostException()
        viewModel.updateAccount(emptyMap())
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.updateAccount(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.uiState.value?.isUpdating == false)
    }

    @Test
    fun `updateAccount unknown exception`() = runTest {
        val viewModel = EditProfileViewModel(interactor, resourceManager, notifier, account)
        coEvery { interactor.updateAccount(any()) } throws Exception()
        
        viewModel.updateAccount(emptyMap())
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.updateAccount(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.uiState.value?.isUpdating == false)
    }

    @Test
    fun `updateAccount success`() = runTest {
        val viewModel = EditProfileViewModel(interactor, resourceManager, notifier, account)
        coEvery { interactor.updateAccount(any()) } returns account
        coEvery { notifier.send(any<AccountUpdated>()) } returns Unit
        viewModel.updateAccount(emptyMap())
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.updateAccount(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value?.isUpdating == false)
    }

    @Test
    fun `updateAccountAndImage no internet connection`() = runTest {
        val viewModel = EditProfileViewModel(interactor, resourceManager, notifier, account)
        coEvery { interactor.setProfileImage(any(), any()) } throws UnknownHostException()
        coEvery { interactor.updateAccount(any()) } returns account
        coEvery { notifier.send(AccountUpdated()) } returns Unit
        
        viewModel.updateAccountAndImage(emptyMap(), file, "")
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.updateAccount(any()) }
        coVerify(exactly = 1) { interactor.setProfileImage(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(noInternet, message?.message)
        assert(viewModel.selectedImageUri.value == null)
        assert(viewModel.uiState.value?.isUpdating == false)
    }

    @Test
    fun `updateAccountAndImage unknown exception`() = runTest {
        val viewModel = EditProfileViewModel(interactor, resourceManager, notifier, account)
        coEvery { interactor.setProfileImage(any(), any()) } throws Exception()
        coEvery { interactor.updateAccount(any()) } returns account
        coEvery { notifier.send(AccountUpdated()) } returns Unit
        
        viewModel.updateAccountAndImage(emptyMap(), file, "")
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.updateAccount(any()) }
        coVerify(exactly = 1) { interactor.setProfileImage(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(somethingWrong, message?.message)
        assert(viewModel.selectedImageUri.value == null)
        assert(viewModel.uiState.value?.isUpdating == false)
    }

    @Test
    fun `updateAccountAndImage success`() = runTest {
        val viewModel = EditProfileViewModel(interactor, resourceManager, notifier, account)
        coEvery { interactor.setProfileImage(any(), any()) } returns Unit
        coEvery { interactor.updateAccount(any()) } returns account
        coEvery { notifier.send(any<AccountUpdated>()) } returns Unit
        
        viewModel.updateAccountAndImage(emptyMap(), file, "")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.updateAccount(any()) }
        coVerify(exactly = 1) { interactor.setProfileImage(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.selectedImageUri.value == null)
        assert(viewModel.uiState.value?.isUpdating == false)
    }

    @Test
    fun `setImageUri set new value`() {
        val viewModel = EditProfileViewModel(interactor, resourceManager, notifier, account)
        viewModel.setImageUri(mockk())

        assert(viewModel.selectedImageUri.value != null)
    }

}