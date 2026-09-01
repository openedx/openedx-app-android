package org.openedx.course.presentation.section

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.openedx.core.CoreMocks
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModelEntity
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.presentation.captureUiMessage
import org.openedx.foundation.system.ResourceManager
import java.net.UnknownHostException
import org.openedx.foundation.R as foundationR

@OptIn(ExperimentalCoroutinesApi::class)
class CourseSectionViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<CourseInteractor>()
    private val downloadDao = mockk<DownloadDao>()
    private val workerController = mockk<DownloadWorkerController>()
    private val networkConnection = mockk<NetworkConnection>()
    private val preferencesManager = mockk<CorePreferences>()
    private val notifier = mockk<CourseNotifier>()
    private val analytics = mockk<CourseAnalytics>()
    private val coreAnalytics = mockk<CoreAnalytics>()

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
    private val cantDownload = "You can download content only from Wi-fi"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(foundationR.string.foundation_error_no_connection) } returns noInternet
        every { resourceManager.getString(foundationR.string.foundation_error_unknown_error) } returns somethingWrong
        every {
            resourceManager.getString(org.openedx.course.R.string.course_can_download_only_with_wifi)
        } returns cantDownload
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getBlocks no internet connection exception`() = runTest {
        every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )

        coEvery { interactor.getCourseStructure(any()) } throws UnknownHostException()
        coEvery { interactor.getCourseStructureForVideos(any()) } throws UnknownHostException()

        viewModel.getBlocks("", CourseViewMode.FULL)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any()) }
        coVerify(exactly = 0) { interactor.getCourseStructureForVideos(any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(noInternet, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is CourseSectionUIState.Loading)
    }

    @Test
    fun `getBlocks unknown exception`() = runTest {
        every { downloadDao.getAllDataFlow() } returns flow { emit(emptyList()) }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )

        coEvery { interactor.getCourseStructure(any()) } throws Exception()
        coEvery { interactor.getCourseStructureForVideos(any()) } throws Exception()

        viewModel.getBlocks("id2", CourseViewMode.FULL)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getCourseStructure(any()) }
        coVerify(exactly = 0) { interactor.getCourseStructureForVideos(any()) }

        val message = captureUiMessage(viewModel)
        assertEquals(somethingWrong, (message.await() as? UIMessage.SnackBarMessage)?.message)
        assert(viewModel.uiState.value is CourseSectionUIState.Loading)
    }

    @Test
    fun `getBlocks success`() = runTest {
        coEvery { downloadDao.getAllDataFlow() } returns flow {
            emit(listOf(DownloadModelEntity.createFrom(CoreMocks.mockDownloadModel)))
        }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )

        coEvery { downloadDao.getAllDataFlow() } returns flow {
            emit(listOf(DownloadModelEntity.createFrom(CoreMocks.mockDownloadModel)))
        }
        coEvery { interactor.getCourseStructure(any()) } returns CoreMocks.mockCourseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns CoreMocks.mockCourseStructure

        viewModel.getBlocks("id", CourseViewMode.VIDEOS)
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getCourseStructure(any()) }
        coVerify(exactly = 1) { interactor.getCourseStructureForVideos(any()) }

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
        assert(viewModel.uiState.value is CourseSectionUIState.Blocks)
    }

    @Test
    fun `saveDownloadModels test`() = runTest {
        coEvery { downloadDao.getAllDataFlow() } returns flow {
            emit(listOf(DownloadModelEntity.createFrom(CoreMocks.mockDownloadModel)))
        }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns false
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(any()) } returns Unit
        every { coreAnalytics.logEvent(any(), any()) } returns Unit

        advanceUntilIdle()

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
    }

    @Test
    fun `saveDownloadModels only wifi download, with connection`() = runTest {
        coEvery { downloadDao.getAllDataFlow() } returns flow {
            emit(listOf(DownloadModelEntity.createFrom(CoreMocks.mockDownloadModel)))
        }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )
        every { preferencesManager.videoSettings.wifiDownloadOnly } returns true
        every { networkConnection.isWifiConnected() } returns true
        coEvery { workerController.saveModels(any()) } returns Unit
        every { coreAnalytics.logEvent(any(), any()) } returns Unit

        advanceUntilIdle()

        val message = captureUiMessage(viewModel)
        assert(message.await() == null)
    }

    @Test
    fun `updateVideos success`() = runTest {
        every { downloadDao.getAllDataFlow() } returns flow {
            repeat(5) {
                delay(10000)
                emit(emptyList())
            }
        }
        val viewModel = CourseSectionViewModel(
            "",
            interactor,
            resourceManager,
            notifier,
            analytics,
        )

        coEvery { notifier.notifier } returns flow { }
        coEvery { interactor.getCourseStructure(any()) } returns CoreMocks.mockCourseStructure
        coEvery { interactor.getCourseStructureForVideos(any()) } returns CoreMocks.mockCourseStructure

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        viewModel.getBlocks("id", CourseViewMode.VIDEOS)
        advanceUntilIdle()

        assert(viewModel.uiState.value is CourseSectionUIState.Blocks)
    }
}
