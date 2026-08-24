package org.openedx.course.domain.interactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.openedx.core.CoreMocks
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.course.data.repository.CourseRepository

@OptIn(ExperimentalCoroutinesApi::class)
class CourseInteractorFreshTest {

    private lateinit var repository: CourseRepository
    private lateinit var networkConnection: NetworkConnection
    private lateinit var interactor: CourseInteractor

    @Before
    fun setUp() {
        repository = mockk()
        networkConnection = mockk()
        interactor = CourseInteractor(repository, networkConnection)
    }

    @Test
    fun `offline refresh returns cached data without using the fresh path`() = runTest {
        val cachedStructure = CoreMocks.mockCourseStructure.copy(name = "cached")
        every { networkConnection.isOnline() } returns false
        every {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true)
        } returns flowOf(cachedStructure)

        val result = interactor.getCourseStructure(COURSE_ID, isNeedRefresh = true)

        assertSame(cachedStructure, result)
        coVerify(exactly = 0) { repository.getCourseStructureFresh(any()) }
    }

    @Test
    fun `offline refresh without cached data throws the cache exception`() = runTest {
        every { networkConnection.isOnline() } returns false
        every {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true)
        } returns flow { throw NoCachedDataException() }

        val failure = try {
            interactor.getCourseStructure(COURSE_ID, isNeedRefresh = true)
            fail("Expected NoCachedDataException")
            error("unreachable")
        } catch (throwable: Throwable) {
            throwable
        }

        assertTrue(failure is NoCachedDataException)
        coVerify(exactly = 0) { repository.getCourseStructureFresh(any()) }
    }

    @Test
    fun `online refresh uses the fresh repository path`() = runTest {
        val freshStructure = CoreMocks.mockCourseStructure.copy(name = "fresh")
        every { networkConnection.isOnline() } returns true
        coEvery { repository.getCourseStructureFresh(COURSE_ID) } returns freshStructure

        val result = interactor.getCourseStructure(COURSE_ID, isNeedRefresh = true)

        assertSame(freshStructure, result)
        coVerify(exactly = 1) { repository.getCourseStructureFresh(COURSE_ID) }
        verify(exactly = 0) { repository.getCourseStructureFlow(any(), any()) }
    }

    @Test
    fun `cache-first request keeps Flow behavior`() = runTest {
        val cachedStructure = CoreMocks.mockCourseStructure.copy(name = "cached")
        every {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = false)
        } returns flowOf(cachedStructure)

        val result = interactor.getCourseStructure(COURSE_ID, isNeedRefresh = false)

        assertSame(cachedStructure, result)
        verify(exactly = 1) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = false)
        }
        coVerify(exactly = 0) { repository.getCourseStructureFresh(any()) }
        verify(exactly = 0) { networkConnection.isOnline() }
    }

    private companion object {
        const val COURSE_ID = "course-v1:TestX+Freshness+2026"
    }
}
