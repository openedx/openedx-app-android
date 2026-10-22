package org.openedx.course.data.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoalescingCacheTest {

    @Test
    fun `old generation write does not replace the current cache entry`() {
        var activeGeneration = 1L
        val cache = CoalescingCache<String, String>(
            fetch = { "unused" },
            activeGeneration = { activeGeneration },
        )

        cache.setCached(KEY, "generation-1", writeGeneration = 1L)
        activeGeneration = 2L
        assertNull(cache.getCached(KEY))

        cache.setCached(KEY, "generation-2", writeGeneration = 2L)
        cache.setCached(KEY, "late-generation-1", writeGeneration = 1L)
        assertEquals("generation-2", cache.getCached(KEY))
    }

    @Test
    fun `cancellation starts a replacement request that survives old fetch completion`() = runTest {
        val fetchGates = Channel<CompletableDeferred<String>>(Channel.UNLIMITED)
        var fetchCount = 0
        val cache = CoalescingCache<String, String>(
            fetch = {
                fetchCount += 1
                val gate = CompletableDeferred<String>()
                fetchGates.send(gate)
                gate.await()
            },
        )

        val oldFetch = async(start = CoroutineStart.UNDISPATCHED) {
            cache.getOrFetch(KEY, forceRefresh = true)
        }
        val oldGate = fetchGates.receive()
        // Start a new request while the waiting caller is being cancelled to verify it survives cancellation.
        val cancelledWaiter = async(UnconfinedTestDispatcher(testScheduler)) {
            cache.getOrFetch(KEY, forceRefresh = true)
        }
        val laterCall = CompletableDeferred<Deferred<String>>()
        cancelledWaiter.invokeOnCompletion {
            laterCall.complete(
                async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrFetch(KEY, forceRefresh = true)
                },
            )
        }

        cache.cancelPending()

        assertTrue(cancelledWaiter.isCancelled)
        val newFetch = laterCall.await()
        assertFalse(newFetch.isCancelled)
        val newGate = fetchGates.receive()

        oldGate.complete("old")
        assertEquals("old", oldFetch.await())

        val newWaiter = async(start = CoroutineStart.UNDISPATCHED) {
            cache.getOrFetch(KEY, forceRefresh = true)
        }
        assertFalse(newWaiter.isCompleted)
        assertTrue(fetchGates.tryReceive().isFailure)

        newGate.complete("new")
        assertEquals("new", newFetch.await())
        assertEquals("new", newWaiter.await())
        assertEquals(2, fetchCount)
    }

    private companion object {
        const val KEY = "course"
    }
}
