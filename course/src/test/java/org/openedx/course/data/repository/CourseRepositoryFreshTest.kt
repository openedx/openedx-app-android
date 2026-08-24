package org.openedx.course.data.repository

import com.google.gson.JsonSyntaxException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.openedx.core.CoreMocks
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.CourseComponentStatus
import org.openedx.core.data.model.CourseDates
import org.openedx.core.data.model.CourseProgressResponse
import org.openedx.core.data.model.CourseStructureModel
import org.openedx.core.data.model.room.CourseEnrollmentDetailsEntity
import org.openedx.core.data.model.room.CourseProgressEntity
import org.openedx.core.data.model.room.CourseStructureEntity
import org.openedx.core.data.model.room.VideoProgressEntity
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.CourseDao
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.system.connection.NetworkConnection
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
class CourseRepositoryFreshTest {

    private data class CourseStructureFixture(
        val domain: CourseStructure,
        val roomEntity: CourseStructureEntity,
        val response: CourseStructureModel,
    )

    private data class ApiRequest(
        val cacheControl: String,
        val courseId: String,
        val response: CompletableDeferred<CourseStructureModel>,
    )

    private lateinit var api: CourseApi
    private lateinit var courseDao: GatedCourseDao
    private lateinit var preferencesManager: CorePreferences
    private lateinit var networkConnection: NetworkConnection
    private lateinit var repository: CourseRepository
    private lateinit var apiRequests: Channel<ApiRequest>

    @Before
    fun setUp() {
        api = mockk()
        courseDao = GatedCourseDao()
        preferencesManager = mockk()
        networkConnection = mockk()
        apiRequests = Channel(Channel.UNLIMITED)

        every { preferencesManager.user } returns null
        every { networkConnection.isOnline() } returns true
        coEvery {
            api.getCourseStructure(any(), any(), any(), any())
        } coAnswers {
            val response = CompletableDeferred<CourseStructureModel>()
            val request = ApiRequest(
                cacheControl = firstArg(),
                courseId = arg(3),
                response = response,
            )
            apiRequests.send(request)
            response.await()
        }

        repository = CourseRepository(
            api = api,
            courseDao = courseDao,
            downloadDao = mockk<DownloadDao>(relaxed = true),
            preferencesManager = preferencesManager,
            networkConnection = networkConnection,
        )
    }

    @Test
    fun `fresh fetch returns the server response and updates Room and memory`() = runTest {
        val cachedV1 = fixture("v1")
        val originV2 = fixture("v2")
        courseDao.storedCourseStructure.set(cachedV1.roomEntity)
        assertSame(cachedV1.domain, repository.getCourseStructureFromCache(COURSE_ID))

        val freshCall = startFreshRequest()
        val request = apiRequests.receive()
        assertEquals(COURSE_ID, request.courseId)
        assertEquals("no-cache", request.cacheControl)
        request.response.complete(originV2.response)

        assertSame(originV2.domain, freshCall.await())
        assertSame(originV2.roomEntity, courseDao.storedCourseStructure.get())
        assertSame(originV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `fresh fetch failures propagate without returning cached data`() = runTest {
        val cachedV1 = fixture("v1")
        courseDao.storedCourseStructure.set(cachedV1.roomEntity)
        assertSame(cachedV1.domain, repository.getCourseStructureFromCache(COURSE_ID))

        val failures = listOf(
            UnknownHostException("offline"),
            httpException(500),
            httpException(403),
            JsonSyntaxException("invalid response"),
        )

        for (failure in failures) {
            val actualFailure = supervisorScope {
                val freshCall = async(start = CoroutineStart.UNDISPATCHED) {
                    repository.getCourseStructureFresh(COURSE_ID)
                }
                val request = apiRequests.receive()
                request.response.completeExceptionally(failure)
                failureFrom(freshCall)
            }

            assertEquals(failure::class, actualFailure::class)
            assertEquals(failure.message, actualFailure.message)
            assertSame(cachedV1.domain, repository.getCourseStructureFromCache(COURSE_ID))
        }
    }

    @Test
    fun `cache-first Flow emits the memory cache before the server result`() = runTest {
        val cachedV1 = fixture("v1")
        val serverV2 = fixture("v2")
        courseDao.storedCourseStructure.set(cachedV1.roomEntity)
        repository.getCourseStructureFromCache(COURSE_ID)

        val collection = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).toList()
        }
        val request = apiRequests.receive()
        assertEquals("stale-if-error=0", request.cacheControl)
        request.response.complete(serverV2.response)

        assertEquals(listOf(cachedV1.domain, serverV2.domain), collection.await())
        assertSame(serverV2.roomEntity, courseDao.storedCourseStructure.get())
    }

    @Test
    fun `concurrent fresh fetches share one request and persist once`() = runTest {
        val originV2 = fixture("v2")

        val firstCall = startFreshRequest()
        val secondCall = startFreshRequest()
        val request = apiRequests.receive()

        assertTrue(apiRequests.tryReceive().isFailure)
        request.response.complete(originV2.response)

        assertSame(originV2.domain, firstCall.await())
        assertSame(originV2.domain, secondCall.await())
        assertEquals(1, courseDao.insertCalls.count { it === originV2.roomEntity })
    }

    @Test
    fun `later fresh fetch waits for the first response to persist`() = runTest {
        val firstResponse = fixture("first-response")
        val insertGate = courseDao.gateInsert(firstResponse.roomEntity)

        val firstCall = startFreshRequest()
        apiRequests.receive().response.complete(firstResponse.response)
        insertGate.started.await()

        val laterCall = startFreshRequest()
        assertTrue(apiRequests.tryReceive().isFailure)

        insertGate.release.complete(Unit)
        assertSame(firstResponse.domain, firstCall.await())
        assertSame(firstResponse.domain, laterCall.await())
        assertSame(firstResponse.roomEntity, courseDao.storedCourseStructure.get())
        assertEquals(1, courseDao.insertCalls.count { it === firstResponse.roomEntity })
    }

    @Test
    fun `fresh and cache-first fetches send different cache headers`() = runTest {
        val nonFreshValue = fixture("non-fresh")
        val freshValue = fixture("fresh")

        val nonFreshCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        val nonFreshRequest = apiRequests.receive()
        assertEquals("stale-if-error=0", nonFreshRequest.cacheControl)
        nonFreshRequest.response.complete(nonFreshValue.response)
        assertSame(nonFreshValue.domain, nonFreshCall.await())

        val freshCall = startFreshRequest()
        val freshRequest = apiRequests.receive()
        assertEquals("no-cache", freshRequest.cacheControl)
        freshRequest.response.complete(freshValue.response)
        assertSame(freshValue.domain, freshCall.await())
    }

    @Test
    fun `fresh fetch never shares a pending cache-first request`() = runTest {
        val nonFreshValue = fixture("non-fresh")
        val freshValue = fixture("fresh")

        val nonFreshCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        val nonFreshRequest = apiRequests.receive()

        val freshCall = startFreshRequest()
        val freshRequest = apiRequests.receive()

        assertEquals("stale-if-error=0", nonFreshRequest.cacheControl)
        assertEquals("no-cache", freshRequest.cacheControl)
        nonFreshRequest.response.complete(nonFreshValue.response)
        freshRequest.response.complete(freshValue.response)

        assertSame(nonFreshValue.domain, nonFreshCall.await())
        assertSame(freshValue.domain, freshCall.await())
    }

    @Test
    fun `stale cache-first fetch returns the fresh result after fresh completion`() = runTest {
        val staleV1 = fixture("v1")
        val freshV2 = fixture("v2")

        val nonFreshCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        val nonFreshRequest = apiRequests.receive()
        val freshCall = startFreshRequest()
        val freshRequest = apiRequests.receive()

        freshRequest.response.complete(freshV2.response)
        assertSame(freshV2.domain, freshCall.await())
        assertSame(freshV2.domain, repository.getCourseStructureFromCache(COURSE_ID))

        nonFreshRequest.response.complete(staleV1.response)
        assertSame(freshV2.domain, nonFreshCall.await())
        assertSame(freshV2.roomEntity, courseDao.storedCourseStructure.get())
        assertSame(freshV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `fresh fetch replaces an earlier cache-first result`() = runTest {
        val nonFreshV1 = fixture("v1")
        val freshV2 = fixture("v2")

        val nonFreshCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        val nonFreshRequest = apiRequests.receive()
        val freshCall = startFreshRequest()
        val freshRequest = apiRequests.receive()

        nonFreshRequest.response.complete(nonFreshV1.response)
        assertSame(nonFreshV1.domain, nonFreshCall.await())
        assertSame(nonFreshV1.domain, repository.getCourseStructureFromCache(COURSE_ID))

        freshRequest.response.complete(freshV2.response)
        assertSame(freshV2.domain, freshCall.await())
        assertSame(freshV2.roomEntity, courseDao.storedCourseStructure.get())
        assertSame(freshV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `delayed Room read does not overwrite a completed fresh fetch`() = runTest {
        val roomV1 = fixture("v1")
        val freshV2 = fixture("v2")
        courseDao.storedCourseStructure.set(roomV1.roomEntity)
        val readGate = courseDao.gateNextRead()

        every { networkConnection.isOnline() } returns false
        val flowCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID).first()
        }
        readGate.started.await()

        every { networkConnection.isOnline() } returns true
        val freshCall = startFreshRequest()
        val freshRequest = apiRequests.receive()
        freshRequest.response.complete(freshV2.response)
        assertSame(freshV2.domain, freshCall.await())

        readGate.release.complete(Unit)
        assertSame(freshV2.domain, flowCall.await())
        assertSame(freshV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `delayed cache-only Room read returns the completed fresh result`() = runTest {
        val roomV1 = fixture("v1")
        val freshV2 = fixture("v2")
        courseDao.storedCourseStructure.set(roomV1.roomEntity)
        val readGate = courseDao.gateNextRead()

        val cacheCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFromCache(COURSE_ID)
        }
        readGate.started.await()

        val freshCall = startFreshRequest()
        val freshRequest = apiRequests.receive()
        freshRequest.response.complete(freshV2.response)
        assertSame(freshV2.domain, freshCall.await())

        readGate.release.complete(Unit)
        assertSame(freshV2.domain, cacheCall.await())
        assertSame(freshV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `old session does not write after reset when it reaches the write guard`() = runTest {
        val mutexHolder = fixture("mutex-holder")
        val lateOldValue = fixture("late-old")
        val insertGate = courseDao.gateInsert(mutexHolder.roomEntity)

        val holderCall = startFreshRequest()
        apiRequests.receive().response.complete(mutexHolder.response)
        insertGate.started.await()

        val lateCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        apiRequests.receive().response.complete(lateOldValue.response)

        repository.endCourseSession()
        repository.startCourseSession(COURSE_ID)
        insertGate.release.complete(Unit)

        holderCall.await()
        lateCall.await()
        assertFalse(courseDao.insertCalls.any { it === lateOldValue.roomEntity })
        courseDao.storedCourseStructure.set(null)
        assertFailureType<NoCachedDataException> {
            repository.getCourseStructureFromCache(COURSE_ID)
        }
    }

    @Test
    fun `new session write is final after an old Room insert resumes`() = runTest {
        val oldV1 = fixture("old-v1")
        val newV2 = fixture("new-v2")
        val oldInsertGate = courseDao.gateInsert(oldV1.roomEntity)

        val oldCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        apiRequests.receive().response.complete(oldV1.response)
        oldInsertGate.started.await()

        repository.endCourseSession()
        repository.startCourseSession(COURSE_ID)

        val newCall = startFreshRequest()
        apiRequests.receive().response.complete(newV2.response)
        assertFalse(newCall.isCompleted)

        oldInsertGate.release.complete(Unit)
        oldCall.await()
        assertSame(newV2.domain, newCall.await())

        assertTrue(courseDao.insertCalls.any { it === oldV1.roomEntity })
        assertSame(newV2.roomEntity, courseDao.storedCourseStructure.get())
        assertSame(newV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `fresh fetch does not read an old Room row`() = runTest {
        val staleRoomV1 = fixture("stale-room-v1")
        val originV2 = fixture("origin-v2")
        courseDao.storedCourseStructure.set(staleRoomV1.roomEntity)

        repository.endCourseSession()
        repository.startCourseSession(COURSE_ID)
        val readsBeforeFreshCall = courseDao.readCount.get()

        val freshCall = startFreshRequest()
        apiRequests.receive().response.complete(originV2.response)

        assertSame(originV2.domain, freshCall.await())
        assertEquals(readsBeforeFreshCall, courseDao.readCount.get())
        assertSame(originV2.roomEntity, courseDao.storedCourseStructure.get())
        assertSame(originV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `cache-first read may return old Room data until a fresh fetch replaces it`() = runTest {
        val staleRoomV1 = fixture("stale-room-v1")
        val originV2 = fixture("origin-v2")
        courseDao.storedCourseStructure.set(staleRoomV1.roomEntity)
        repository.endCourseSession()
        repository.startCourseSession(COURSE_ID)

        every { networkConnection.isOnline() } returns false
        val displayedStructure = repository.getCourseStructureFlow(COURSE_ID).first()
        assertSame(staleRoomV1.domain, displayedStructure)

        every { networkConnection.isOnline() } returns true
        val freshCall = startFreshRequest()
        apiRequests.receive().response.complete(originV2.response)
        assertSame(originV2.domain, freshCall.await())
        assertSame(originV2.roomEntity, courseDao.storedCourseStructure.get())
        assertSame(originV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `old session completion does not clear a new session refresh marker`() = runTest {
        val oldValue = fixture("old")
        val cachedV1 = fixture("cached-v1")
        val refreshedV2 = fixture("refreshed-v2")

        val oldCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        val oldRequest = apiRequests.receive()

        repository.endCourseSession()
        repository.startCourseSession(COURSE_ID)
        courseDao.storedCourseStructure.set(cachedV1.roomEntity)
        assertSame(cachedV1.domain, repository.getCourseStructureFromCache(COURSE_ID))

        oldRequest.response.complete(oldValue.response)
        assertSame(oldValue.domain, oldCall.await())

        val newFlow = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID).take(2).toList()
        }
        val newRequest = apiRequests.receive()
        assertEquals("stale-if-error=0", newRequest.cacheControl)
        newRequest.response.complete(refreshedV2.response)

        assertEquals(listOf(cachedV1.domain, refreshedV2.domain), newFlow.await())
        assertSame(refreshedV2.roomEntity, courseDao.storedCourseStructure.get())
    }

    @Test
    fun `status refresh marker survives an old course structure completion`() = runTest {
        val cachedStatus = CourseComponentStatus("cached")
        val refreshedStatus = CourseComponentStatus("refreshed")

        completeOldCourseStructureAfterStartingNewSession()
        coEvery { api.getCourseStatus(any(), COURSE_ID) } returns cachedStatus
        assertEquals(cachedStatus.mapToDomain(), repository.getCourseStatus(COURSE_ID))

        coEvery { api.getCourseStatus(any(), COURSE_ID) } returns refreshedStatus
        assertEquals(
            listOf(cachedStatus.mapToDomain(), refreshedStatus.mapToDomain()),
            repository.getCourseStatusFlow(COURSE_ID).take(2).toList(),
        )
    }

    @Test
    fun `dates refresh marker survives an old course structure completion`() = runTest {
        val cachedDates = courseDates(hasEnded = false)
        val refreshedDates = courseDates(hasEnded = true)

        completeOldCourseStructureAfterStartingNewSession()
        coEvery { api.getCourseDates(COURSE_ID, any(), any()) } returns cachedDates
        assertEquals(
            cachedDates.getCourseDatesResult(),
            repository.getCourseDates(COURSE_ID, forceRefresh = true),
        )

        coEvery { api.getCourseDates(COURSE_ID, any(), any()) } returns refreshedDates
        assertEquals(
            listOf(cachedDates.getCourseDatesResult(), refreshedDates.getCourseDatesResult()),
            repository.getCourseDatesFlow(COURSE_ID).take(2).toList(),
        )
    }

    @Test
    fun `progress refresh marker survives an old course structure completion`() = runTest {
        val cachedProgress = courseProgress("cached")
        val refreshedProgress = courseProgress("refreshed")

        completeOldCourseStructureAfterStartingNewSession()
        coEvery { api.getCourseProgress(COURSE_ID) } returns cachedProgress
        assertEquals(
            cachedProgress.mapToDomain(),
            repository.getCourseProgress(
                courseId = COURSE_ID,
                isRefresh = true,
                getOnlyCacheIfExist = false,
            ).first(),
        )

        coEvery { api.getCourseProgress(COURSE_ID) } returns refreshedProgress
        assertEquals(
            listOf(cachedProgress.mapToDomain(), refreshedProgress.mapToDomain()),
            repository.getCourseProgress(
                courseId = COURSE_ID,
                isRefresh = false,
                getOnlyCacheIfExist = true,
            ).take(2).toList(),
        )
    }

    @Test
    fun `cache-first fetch started before reset keeps its original session`() = runTest {
        val staleRoomValue = fixture("stale-room")
        val oldResponse = fixture("old-response")
        val refreshedValue = fixture("refreshed")
        courseDao.storedCourseStructure.set(staleRoomValue.roomEntity)
        val readGate = courseDao.gateNextRead()

        val oldCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        readGate.started.await()

        repository.endCourseSession()
        repository.startCourseSession(COURSE_ID)
        readGate.release.complete(Unit)

        val oldRequest = apiRequests.receive()
        oldRequest.response.complete(oldResponse.response)
        assertSame(oldResponse.domain, oldCall.await())
        assertFalse(courseDao.insertCalls.any { it === oldResponse.roomEntity })

        val newFlow = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID).take(2).toList()
        }
        val newRequest = apiRequests.tryReceive().getOrNull()
            ?: error("Expected the new session to issue a refresh request")
        newRequest.response.complete(refreshedValue.response)

        assertEquals(listOf(staleRoomValue.domain, refreshedValue.domain), newFlow.await())
        assertSame(refreshedValue.roomEntity, courseDao.storedCourseStructure.get())
    }

    @Test
    fun `old fresh completion does not advance the new session completion version`() = runTest {
        val oldFreshV1 = fixture("old-fresh-v1")
        val newNonFreshV2 = fixture("new-non-fresh-v2")
        val oldInsertGate = courseDao.gateInsert(oldFreshV1.roomEntity)

        val oldFreshCall = startFreshRequest()
        apiRequests.receive().response.complete(oldFreshV1.response)
        oldInsertGate.started.await()

        repository.endCourseSession()
        repository.startCourseSession(COURSE_ID)

        val newNonFreshCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        val newNonFreshRequest = apiRequests.receive()

        oldInsertGate.release.complete(Unit)
        oldFreshCall.await()
        newNonFreshRequest.response.complete(newNonFreshV2.response)
        assertSame(newNonFreshV2.domain, newNonFreshCall.await())

        assertSame(newNonFreshV2.roomEntity, courseDao.storedCourseStructure.get())
        assertSame(newNonFreshV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `fresh request started before reset cannot write to the new session`() = runTest {
        val oldFreshValue = fixture("old-fresh")
        val newFreshValue = fixture("new-fresh")

        val oldFreshCall = startFreshRequest()
        val oldFreshRequest = apiRequests.receive()
        repository.endCourseSession()
        repository.startCourseSession(COURSE_ID)

        oldFreshRequest.response.complete(oldFreshValue.response)
        assertSame(oldFreshValue.domain, oldFreshCall.await())
        assertFalse(courseDao.insertCalls.any { it === oldFreshValue.roomEntity })

        val newFreshCall = startFreshRequest()
        val newFreshRequest = apiRequests.receive()
        newFreshRequest.response.complete(newFreshValue.response)

        assertSame(newFreshValue.domain, newFreshCall.await())
        assertSame(newFreshValue.roomEntity, courseDao.storedCourseStructure.get())
    }

    private fun TestScope.startFreshRequest(): Deferred<CourseStructure> {
        return async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFresh(COURSE_ID)
        }
    }

    private suspend fun TestScope.completeOldCourseStructureAfterStartingNewSession() {
        val oldStructure = fixture("old")
        val oldCall = async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFlow(COURSE_ID, forceRefresh = true).first()
        }
        val oldRequest = apiRequests.receive()

        repository.endCourseSession()
        repository.startCourseSession(COURSE_ID)

        oldRequest.response.complete(oldStructure.response)
        assertSame(oldStructure.domain, oldCall.await())
    }

    private fun courseDates(hasEnded: Boolean) = CourseDates(
        courseDateBlocks = emptyList(),
        datesBannerInfo = null,
        hasEnded = hasEnded,
    )

    private fun courseProgress(verifiedMode: String) = CourseProgressResponse(
        verifiedMode = verifiedMode,
        accessExpiration = null,
        certificateData = null,
        completionSummary = null,
        courseGrade = null,
        creditCourseRequirements = null,
        end = null,
        enrollmentMode = null,
        gradingPolicy = null,
        hasScheduledContent = null,
        sectionScores = null,
        studioUrl = null,
        username = null,
        userHasPassingGrade = null,
        verificationData = null,
        disableProgressGraph = null,
    )

    private fun fixture(version: String): CourseStructureFixture {
        val domain = CoreMocks.mockCourseStructure.copy(
            id = COURSE_ID,
            name = version,
        )
        val roomEntity = mockk<CourseStructureEntity>()
        val response = mockk<CourseStructureModel>()
        every { roomEntity.mapToDomain() } returns domain
        every { response.mapToDomain() } returns domain
        every { response.mapToRoomEntity() } returns roomEntity
        return CourseStructureFixture(domain, roomEntity, response)
    }

    private suspend fun failureFrom(call: Deferred<*>): Throwable {
        return try {
            call.await()
            fail("Expected the request to fail")
            error("unreachable")
        } catch (throwable: Throwable) {
            throwable
        }
    }

    private suspend inline fun <reified T : Throwable> assertFailureType(
        crossinline block: suspend () -> Unit,
    ) {
        val failure = try {
            block()
            fail("Expected ${T::class.simpleName}")
            error("unreachable")
        } catch (throwable: Throwable) {
            throwable
        }
        assertTrue(failure is T)
    }

    private fun httpException(statusCode: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<CourseStructureModel>(statusCode, body))
    }

    private class GatedCourseDao : CourseDao {
        data class Gate(
            val started: CompletableDeferred<Unit> = CompletableDeferred(),
            val release: CompletableDeferred<Unit> = CompletableDeferred(),
        )

        val storedCourseStructure = AtomicReference<CourseStructureEntity?>()
        val insertCalls = Collections.synchronizedList(mutableListOf<CourseStructureEntity>())
        val readCount = AtomicInteger(0)

        private var nextReadGate: Gate? = null
        private var gatedInsertEntity: CourseStructureEntity? = null
        private var insertGate: Gate? = null

        fun gateNextRead(): Gate {
            return Gate().also { nextReadGate = it }
        }

        fun gateInsert(roomEntity: CourseStructureEntity): Gate {
            gatedInsertEntity = roomEntity
            return Gate().also { insertGate = it }
        }

        override suspend fun getCourseStructureById(id: String): CourseStructureEntity? {
            readCount.incrementAndGet()
            val structureAtReadStart = storedCourseStructure.get()
            val gate = nextReadGate
            nextReadGate = null
            if (gate != null) {
                gate.started.complete(Unit)
                gate.release.await()
            }
            return structureAtReadStart
        }

        override suspend fun insertCourseStructureEntity(
            vararg courseStructureEntity: CourseStructureEntity,
        ) {
            for (roomEntity in courseStructureEntity) {
                insertCalls.add(roomEntity)
                if (roomEntity === gatedInsertEntity) {
                    val gate = insertGate
                    gate?.started?.complete(Unit)
                    gate?.release?.await()
                }
                storedCourseStructure.set(roomEntity)
            }
        }

        override suspend fun clearCourseStructure() {
            storedCourseStructure.set(null)
        }

        override suspend fun clearVideoProgress() = Unit

        override suspend fun clearEnrollmentCachedData() = Unit

        override suspend fun clearCourseProgressData() = Unit

        override suspend fun insertCourseEnrollmentDetailsEntity(
            vararg courseEnrollmentDetailsEntity: CourseEnrollmentDetailsEntity,
        ) = Unit

        override suspend fun getCourseEnrollmentDetailsById(
            id: String,
        ): CourseEnrollmentDetailsEntity? = null

        override suspend fun insertVideoProgressEntity(
            vararg videoProgressEntity: VideoProgressEntity,
        ) = Unit

        override suspend fun getVideoProgressByBlockId(blockId: String): VideoProgressEntity? = null

        override suspend fun insertCourseProgressEntity(
            vararg courseProgressEntity: CourseProgressEntity,
        ) = Unit

        override suspend fun getCourseProgressById(id: String): CourseProgressEntity? = null
    }

    private companion object {
        const val COURSE_ID = "course-v1:TestX+Freshness+2026"
    }
}
