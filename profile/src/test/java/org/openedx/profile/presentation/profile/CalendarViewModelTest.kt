package org.openedx.profile.presentation.profile

import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openedx.core.data.storage.CalendarPreferences
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.CalendarInteractor
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncState
import org.openedx.core.system.CalendarManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.calendar.CalendarCreated
import org.openedx.core.system.notifier.calendar.CalendarNotifier
import org.openedx.core.system.notifier.calendar.CalendarSynced
import org.openedx.core.worker.CalendarSyncScheduler
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.presentation.calendar.CalendarViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CalendarViewModel

    private val calendarSyncScheduler = mockk<CalendarSyncScheduler>(relaxed = true)
    private val calendarManager = mockk<CalendarManager>(relaxed = true)
    private val calendarPreferences = mockk<CalendarPreferences>(relaxed = true)
    private val calendarNotifier = mockk<CalendarNotifier>(relaxed = true)
    private val calendarInteractor = mockk<CalendarInteractor>(relaxed = true)
    private val corePreferences = mockk<CorePreferences>(relaxed = true)
    private val profileRouter = mockk<ProfileRouter>()
    private val networkConnection = mockk<NetworkConnection>()
    private val permissionLauncher = mockk<ActivityResultLauncher<Array<String>>>()
    private val fragmentManager = mockk<FragmentManager>()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { networkConnection.isOnline() } returns true
        viewModel = CalendarViewModel(
            calendarSyncScheduler = calendarSyncScheduler,
            calendarManager = calendarManager,
            calendarPreferences = calendarPreferences,
            calendarNotifier = calendarNotifier,
            calendarInteractor = calendarInteractor,
            corePreferences = corePreferences,
            profileRouter = profileRouter,
            networkConnection = networkConnection
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init triggers immediate sync and loads calendar data`() = runTest(dispatcher) {
        coVerify { calendarSyncScheduler.requestImmediateSync() }
        coVerify { calendarInteractor.getAllCourseCalendarStateFromCache() }
    }

    @Test
    fun `setUpCalendarSync launches permission request`() = runTest(dispatcher) {
        every { permissionLauncher.launch(calendarManager.permissions) } returns Unit
        viewModel.setUpCalendarSync(permissionLauncher)
        coVerify { permissionLauncher.launch(calendarManager.permissions) }
    }

    @Test
    fun `setCalendarSyncEnabled enables sync and triggers sync when isEnabled is true`() = runTest(dispatcher) {
        viewModel.setCalendarSyncEnabled(isEnabled = true, fragmentManager = fragmentManager)

        coVerify {
            calendarPreferences.isCalendarSyncEnabled = true
            calendarSyncScheduler.requestImmediateSync()
        }
        assertTrue(viewModel.uiState.value.isCalendarSyncEnabled)
    }

    @Test
    fun `setRelativeDateEnabled updates preference and UI state`() = runTest(dispatcher) {
        viewModel.setRelativeDateEnabled(true)

        coVerify { corePreferences.isRelativeDatesEnabled = true }
        assertTrue(viewModel.uiState.value.isRelativeDateEnabled)
    }

    @Test
    fun `network disconnection changes sync state to offline`() = runTest(dispatcher) {
        every { networkConnection.isOnline() } returns false
        viewModel = CalendarViewModel(
            calendarSyncScheduler,
            calendarManager,
            calendarPreferences,
            calendarNotifier,
            calendarInteractor,
            corePreferences,
            profileRouter,
            networkConnection
        )

        assertEquals(CalendarSyncState.OFFLINE, viewModel.uiState.value.calendarSyncState)
    }

    @Test
    fun `successful calendar sync updates sync state to SYNCED`() = runTest(dispatcher) {
        viewModel = CalendarViewModel(
            calendarSyncScheduler,
            calendarManager,
            calendarPreferences,
            calendarNotifier.apply {
                every { notifier } returns flowOf(CalendarSynced)
            },
            calendarInteractor,
            corePreferences,
            profileRouter,
            networkConnection
        )

        assertEquals(CalendarSyncState.SYNCED, viewModel.uiState.value.calendarSyncState)
    }

    @Test
    fun `calendar creation updates calendar existence state`() = runTest(dispatcher) {
        every { calendarPreferences.calendarId } returns 1
        every { calendarManager.isCalendarExist(1) } returns true

        viewModel = CalendarViewModel(
            calendarSyncScheduler,
            calendarManager,
            calendarPreferences,
            calendarNotifier.apply {
                every { notifier } returns flowOf(CalendarCreated)
            },
            calendarInteractor,
            corePreferences,
            profileRouter,
            networkConnection
        )

        assertTrue(viewModel.uiState.value.isCalendarExist)
    }
}
