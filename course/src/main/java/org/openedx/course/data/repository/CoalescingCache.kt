package org.openedx.course.data.repository

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores cached values and shares fetches for the same key.
 *
 * If a caller needs to fetch a value and another fetch for that key is already running,
 * it waits for that result instead of starting a duplicate request.
 *
 * @param K the type of cache keys
 * @param V the type of cached values
 * @param fetch the suspend function to fetch data for a given key
 * @param persist optional callback invoked after successful fetch (e.g., to save to database)
 * @param autoCache whether to store a successful fetch result in the cache before [persist] runs
 * @param activeGeneration optional function that returns the current cache version; values saved
 * under a different version are not returned
 */
class CoalescingCache<K, V>(
    private val fetch: suspend (K) -> V,
    private val persist: (suspend (K, V) -> Unit)? = null,
    private val autoCache: Boolean = true,
    private val activeGeneration: (() -> Long)? = null,
) {
    private data class CacheEntry<V>(
        val value: V,
        val generation: Long,
    )

    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val pending = ConcurrentHashMap<K, CompletableDeferred<V>>()

    /**
     * Returns the cached value for [key], or null if no value is stored.
     *
     * When [activeGeneration] is set, also returns null if the stored version is no longer current.
     */
    fun getCached(key: K): V? {
        val entry = cache[key] ?: return null
        val generationProvider = activeGeneration

        return when {
            generationProvider == null -> {
                entry.value
            }

            entry.generation == generationProvider() -> {
                entry.value
            }

            else -> {
                cache.remove(key, entry)
                null
            }
        }
    }

    /**
     * Stores a cached value.
     *
     * When [activeGeneration] is set and [writeGeneration] is supplied, stores the value only if
     * that version is still current.
     */
    fun setCached(
        key: K,
        value: V,
        writeGeneration: Long = UNSPECIFIED_GENERATION,
    ) {
        val generationProvider = activeGeneration
        if (generationProvider == null) {
            cache[key] = CacheEntry(value, DEFAULT_GENERATION)
            return
        }

        if (writeGeneration == UNSPECIFIED_GENERATION) {
            val currentGeneration = generationProvider()
            cache[key] = CacheEntry(value, currentGeneration)
            return
        }

        cache.compute(key) { _, currentEntry ->
            if (generationProvider() == writeGeneration) {
                CacheEntry(value, writeGeneration)
            } else {
                currentEntry
            }
        }
    }

    /**
     * Removes all cached values.
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Removes pending request entries and cancels callers waiting for their results.
     *
     * Remove each entry before cancellation so a waiting caller can start a replacement request
     * from its cancellation callback. Fetches already running can still finish. Their cleanup
     * removes only their own entry, preserving any replacement request for the same key.
     */
    fun cancelPending() {
        val pendingSnapshot = HashMap(pending)
        for ((key, deferred) in pendingSnapshot) {
            if (pending.remove(key, deferred)) {
                deferred.cancel()
            }
        }
    }

    /**
     * Gets the value from cache or fetches it.
     *
     * If [forceRefresh] is false and a cached value exists, returns it immediately.
     * Otherwise, fetches the value. If another fetch for the same key is already
     * in progress, waits for that result instead of making a duplicate request.
     */
    suspend fun getOrFetch(key: K, forceRefresh: Boolean = false): V {
        if (!forceRefresh) {
            getCached(key)?.let { return it }
        }

        val (deferred, startsFetch) = getOrCreateDeferred(key)
        return if (startsFetch) {
            try {
                val value = fetch(key)
                if (autoCache) {
                    setCached(key, value)
                }
                persist?.invoke(key, value)
                deferred.complete(value)
                value
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                pending.remove(key, deferred)
            }
        } else {
            deferred.await()
        }
    }

    private fun getOrCreateDeferred(key: K): Pair<CompletableDeferred<V>, Boolean> {
        pending[key]?.let { return it to false }
        val deferred = CompletableDeferred<V>()
        val existing = pending.putIfAbsent(key, deferred)
        return if (existing != null) existing to false else deferred to true
    }

    private companion object {
        const val DEFAULT_GENERATION = 0L
        const val UNSPECIFIED_GENERATION = Long.MIN_VALUE
    }
}
