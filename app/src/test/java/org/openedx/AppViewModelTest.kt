package org.openedx

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.openedx.core.data.storage.PreferencesManager
import org.openedx.core.domain.model.User
import org.openedx.app.room.AppDatabase
import org.openedx.app.system.notifier.AppNotifier
import org.openedx.app.system.notifier.LogoutEvent
import org.openedx.app.AppAnalytics
import org.openedx.app.AppViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@ExperimentalCoroutinesApi
class AppViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()//UnconfinedTestDispatcher()

    private val notifier = mockk<AppNotifier>()
    private val room = mockk<AppDatabase>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val analytics = mockk<AppAnalytics>()

    private val user = User(0, "", "", "")

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun setIdSuccess() = runTest {
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { preferencesManager.user } returns user
        every { notifier.notifier } returns flow { }
        val viewModel = AppViewModel(notifier, room, preferencesManager, dispatcher, analytics)

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        advanceUntilIdle()

        verify(exactly = 1) { analytics.setUserIdForSession(any()) }
    }

    @Test
    fun forceLogout() = runTest {
        every { notifier.notifier } returns flow {
            emit(LogoutEvent())
        }
        every { preferencesManager.clear() } returns Unit
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { preferencesManager.user } returns user
        every { room.clearAllTables() } returns Unit
        every { analytics.logoutEvent(true) } returns Unit
        val viewModel = AppViewModel(notifier, room, preferencesManager, dispatcher, analytics)

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
            emit(LogoutEvent())
            emit(LogoutEvent())
        }
        every { preferencesManager.clear() } returns Unit
        every { analytics.setUserIdForSession(any()) } returns Unit
        every { preferencesManager.user } returns user
        every { room.clearAllTables() } returns Unit
        every { analytics.logoutEvent(true) } returns Unit
        val viewModel = AppViewModel(notifier, room, preferencesManager, dispatcher, analytics)

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        advanceUntilIdle()

         verify(exactly = 1) { analytics.logoutEvent(true) }
         verify(exactly = 1) { preferencesManager.clear() }
         verify(exactly = 1) { analytics.setUserIdForSession(any()) }
         verify(exactly = 1) { preferencesManager.user }
         verify(exactly = 1) { room.clearAllTables() }
         verify(exactly = 1) { analytics.logoutEvent(true) }
    }

}