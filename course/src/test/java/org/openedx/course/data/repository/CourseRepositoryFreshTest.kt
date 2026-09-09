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
import org.openedx.core.data.model.CourseStructureModel
import org.openedx.core.data.model.room.CourseEnrollmentDetailsEntity
import org.openedx.core.data.model.room.CourseProgressEntity
import org.openedx.core.data.model.room.CourseStructureEntity
import org.openedx.core.data.model.room.VideoProgressEntity
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.CourseDao
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.course.domain.interactor.CourseInteractor
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
    private lateinit var interactor: CourseInteractor
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
        interactor = CourseInteractor(repository, networkConnection)
    }

    @Test
    fun `online refresh returns the server response and updates Room and memory`() = runTest {
        val cachedV1 = fixture("v1")
        val originV2 = fixture("v2")
        courseDao.storedCourseStructure.set(cachedV1.roomEntity)
        assertSame(cachedV1.domain, repository.getCourseStructureFromCache(COURSE_ID))

        val readsBeforeRefresh = courseDao.readCount.get()
        val freshCall = async(start = CoroutineStart.UNDISPATCHED) {
            interactor.getCourseStructure(COURSE_ID, isNeedRefresh = true)
        }
        val request = apiRequests.receive()
        assertEquals(COURSE_ID, request.courseId)
        assertEquals("no-cache", request.cacheControl)
        request.response.complete(originV2.response)

        assertSame(originV2.domain, freshCall.await())
        assertEquals(readsBeforeRefresh, courseDao.readCount.get())
        assertSame(originV2.roomEntity, courseDao.storedCourseStructure.get())
        assertSame(originV2.domain, repository.getCourseStructureFromCache(COURSE_ID))
    }

    @Test
    fun `offline refresh returns saved course data without a network request`() = runTest {
        val cachedV1 = fixture("v1")
        courseDao.storedCourseStructure.set(cachedV1.roomEntity)
        every { networkConnection.isOnline() } returns false

        val result = interactor.getCourseStructure(COURSE_ID, isNeedRefresh = true)

        assertSame(cachedV1.domain, result)
        assertEquals(1, courseDao.readCount.get())
        assertTrue(apiRequests.tryReceive().isFailure)
        assertTrue(courseDao.insertCalls.isEmpty())
    }

    @Test
    fun `online refresh failures propagate without returning cached data`() = runTest {
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
                    interactor.getCourseStructure(COURSE_ID, isNeedRefresh = true)
                }
                val request = apiRequests.receive()
                request.response.completeExceptionally(failure)
                failureFrom(freshCall)
            }

            assertEquals(failure::class, actualFailure::class)
            assertEquals(failure.message, actualFailure.message)
            assertSame(cachedV1.domain, repository.getCourseStructureFromCache(COURSE_ID))
            assertSame(cachedV1.roomEntity, courseDao.storedCourseStructure.get())
            assertTrue(courseDao.insertCalls.isEmpty())
        }
    }

    @Test
    fun `ordinary reads remain cache-first and the Flow delivers the server update`() = runTest {
        val cachedV1 = fixture("v1")
        val serverV2 = fixture("v2")
        courseDao.storedCourseStructure.set(cachedV1.roomEntity)
        repository.getCourseStructureFromCache(COURSE_ID)

        assertSame(
            cachedV1.domain,
            interactor.getCourseStructure(COURSE_ID, isNeedRefresh = false),
        )
        assertTrue(apiRequests.tryReceive().isFailure)

        val collection = async(start = CoroutineStart.UNDISPATCHED) {
            interactor.getCourseStructureFlow(COURSE_ID, forceRefresh = true).toList()
        }
        val request = apiRequests.receive()
        assertEquals("stale-if-error=0", request.cacheControl)
        request.response.complete(serverV2.response)

        assertEquals(listOf(cachedV1.domain, serverV2.domain), collection.await())
        assertSame(serverV2.roomEntity, courseDao.storedCourseStructure.get())
    }

    @Test
    fun `concurrent fresh fetches share one request until the response is persisted`() = runTest {
        val firstResponse = fixture("first-response")
        val insertGate = courseDao.gateInsert(firstResponse.roomEntity)

        val firstCall = startFreshRequest()
        val secondCall = startFreshRequest()
        val request = apiRequests.receive()
        assertTrue(apiRequests.tryReceive().isFailure)
        assertFalse(firstCall.isCompleted)
        assertFalse(secondCall.isCompleted)

        request.response.complete(firstResponse.response)
        insertGate.started.await()

        val laterCall = startFreshRequest()
        assertTrue(apiRequests.tryReceive().isFailure)
        assertFalse(firstCall.isCompleted)
        assertFalse(secondCall.isCompleted)
        assertFalse(laterCall.isCompleted)

        insertGate.release.complete(Unit)
        assertSame(firstResponse.domain, firstCall.await())
        assertSame(firstResponse.domain, secondCall.await())
        assertSame(firstResponse.domain, laterCall.await())
        assertSame(firstResponse.roomEntity, courseDao.storedCourseStructure.get())
        assertEquals(1, courseDao.insertCalls.count { it === firstResponse.roomEntity })
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

        assertEquals("stale-if-error=0", nonFreshRequest.cacheControl)
        assertEquals("no-cache", freshRequest.cacheControl)
        assertEquals(COURSE_ID, nonFreshRequest.courseId)
        assertEquals(COURSE_ID, freshRequest.courseId)
        assertTrue(apiRequests.tryReceive().isFailure)

        freshRequest.response.complete(freshV2.response)
        assertSame(freshV2.domain, freshCall.await())
        assertSame(freshV2.domain, repository.getCourseStructureFromCache(COURSE_ID))

        nonFreshRequest.response.complete(staleV1.response)
        assertSame(freshV2.domain, nonFreshCall.await())
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

    private fun TestScope.startFreshRequest(): Deferred<CourseStructure> {
        return async(start = CoroutineStart.UNDISPATCHED) {
            repository.getCourseStructureFresh(COURSE_ID)
        }
    }

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

    private fun httpException(statusCode: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<CourseStructureModel>(statusCode, body))
    }

    /**
     * Captures the stored value before pausing a read. Tests use [CompletableDeferred] to control
     * when reads and inserts finish. This fake does not simulate Room transactions or HTTP behavior.
     */
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
