package org.openedx.course.data.repository

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * A cache with request coalescing support.
 *
 * When multiple callers request the same data simultaneously,
 * only one fetch operation is performed and all callers receive the same result.
 *
 * @param K the type of cache keys
 * @param V the type of cached values
 * @param fetch the suspend function to fetch data for a given key
 * @param persist optional callback invoked after successful fetch (e.g., to save to database)
 */
class CoalescingCache<K, V>(
    private val fetch: suspend (K) -> V,
    private val persist: (suspend (K, V) -> Unit)? = null
) {
    private val cache = ConcurrentHashMap<K, V>()
    private val pending = ConcurrentHashMap<K, CompletableDeferred<V>>()

    /**
     * Returns cached value for the key, or null if not cached.
     */
    fun getCached(key: K): V? = cache[key]

    /**
     * Manually sets a cached value.
     */
    fun setCached(key: K, value: V) {
        cache[key] = value
    }

    /**
     * Removes all cached values.
     */
    fun clear() {
        cache.clear()
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
            cache[key]?.let { return it }
        }

        val (deferred, isOwner) = getOrCreateDeferred(key)
        return if (isOwner) {
            try {
                val result = fetch(key)
                cache[key] = result
                persist?.invoke(key, result)
                deferred.complete(result)
                result
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                pending.remove(key)
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
}
