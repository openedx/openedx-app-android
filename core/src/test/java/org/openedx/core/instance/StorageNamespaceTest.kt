package org.openedx.core.instance

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageNamespaceTest {

    private class InMemoryStorage(
        override var selectedInstanceKey: String? = null,
        override var legacyOwnerInstanceKey: String? = null,
    ) : InstanceSelectionStorage, LegacyNamespaceOwnerStorage

    private fun instance(key: String, host: String, client: String = "$key-client") = requireNotNull(
        Instance.fromJson(
            JsonObject().apply {
                addProperty("KEY", key)
                addProperty("NAME", key)
                addProperty("OAUTH_CLIENT_ID", client)
                addProperty("API_HOST_URL", host)
            }
        )
    )

    private val appSite = instance("app-site", "https://LMS.example.com/", client = "app-client")
    private val other = instance("other", "https://other.example.com")

    private fun namespaces(storage: InMemoryStorage, vararg instances: Instance): InstanceStorageNamespaces {
        val store = InstanceStore(storage) { InstancesConfig(instances.toList()) }
        return InstanceStorageNamespaces(store, storage, "https://lms.example.com:443", "app-client")
    }

    @Test
    fun `legacy namespace keeps raw keys and has no file segment`() {
        assertEquals("access_token", StorageNamespace.LEGACY.key("access_token"))
        assertNull(StorageNamespace.LEGACY.fileSegment)
        assertTrue(StorageNamespace.LEGACY.isLegacy)
    }

    @Test
    fun `instance namespace prefixes keys with the instance key`() {
        assertEquals("instance.acme.access_token", StorageNamespace("acme").key("access_token"))
    }

    @Test
    fun `file segments are safe and keep keys that sanitize alike apart`() {
        val slash = requireNotNull(StorageNamespace("a/b").fileSegment)
        val colon = requireNotNull(StorageNamespace("a:b").fileSegment)

        assertTrue(slash, slash.matches("[A-Za-z0-9_-]+".toRegex()))
        assertTrue(slash.startsWith("a_b_"))
        assertNotEquals(slash, colon)
        assertEquals(slash, StorageNamespace("a/b").fileSegment)
    }

    @Test
    fun `nothing selected uses the legacy namespace`() {
        val namespaces = namespaces(InMemoryStorage(), appSite, other)

        assertEquals(StorageNamespace.LEGACY, namespaces.current)
        assertEquals(StorageNamespace.LEGACY, namespaces.namespaceFor(null as String?))
    }

    @Test
    fun `the instance matching the app level host and client inherits the legacy namespace`() {
        val storage = InMemoryStorage()
        val namespaces = namespaces(storage, appSite, other)

        assertEquals(StorageNamespace.LEGACY, namespaces.namespaceFor(appSite))
        assertEquals(StorageNamespace("other"), namespaces.namespaceFor(other))
        assertEquals("app-site", storage.legacyOwnerInstanceKey)
    }

    @Test
    fun `same host with a different OAuth client is a different site`() {
        val sameHost = instance("same-host", "https://lms.example.com", client = "another-client")
        val namespaces = namespaces(InMemoryStorage(), sameHost)

        assertEquals(StorageNamespace("same-host"), namespaces.namespaceFor(sameHost))
    }

    @Test
    fun `different port or path is a different site`() {
        val port = instance("port", "https://lms.example.com:8443", client = "app-client")
        val path = instance("path", "https://lms.example.com/tenant", client = "app-client")
        val namespaces = namespaces(InMemoryStorage(), port, path)

        assertEquals(StorageNamespace("port"), namespaces.namespaceFor(port))
        assertEquals(StorageNamespace("path"), namespaces.namespaceFor(path))
    }

    @Test
    fun `the recorded owner never changes once decided`() {
        val storage = InMemoryStorage(legacyOwnerInstanceKey = "other")
        val namespaces = namespaces(storage, appSite, other)

        assertEquals(StorageNamespace.LEGACY, namespaces.namespaceFor(other))
        assertEquals(StorageNamespace("app-site"), namespaces.namespaceFor(appSite))
        assertEquals("other", storage.legacyOwnerInstanceKey)
    }

    @Test
    fun `catalog order does not decide ownership`() {
        val first = namespaces(InMemoryStorage(), other, appSite)
        val second = namespaces(InMemoryStorage(), appSite, other)

        assertEquals(first.namespaceFor(appSite), second.namespaceFor(appSite))
        assertEquals(first.namespaceFor(other), second.namespaceFor(other))
    }

    @Test
    fun `a key no longer in the catalog keeps its own namespace`() {
        val storage = InMemoryStorage(legacyOwnerInstanceKey = "app-site")
        val namespaces = namespaces(storage, other)

        assertEquals(StorageNamespace("gone"), namespaces.namespaceFor("gone"))
        assertEquals(StorageNamespace.LEGACY, namespaces.namespaceFor("app-site"))
    }

    @Test
    fun `current follows the selection`() {
        val storage = InMemoryStorage()
        val store = InstanceStore(storage) { InstancesConfig(listOf(appSite, other)) }
        val namespaces = InstanceStorageNamespaces(store, storage, "https://lms.example.com", "app-client")

        store.select(other)
        assertEquals(StorageNamespace("other"), namespaces.current)
        store.select(appSite)
        assertEquals(StorageNamespace.LEGACY, namespaces.current)
    }
}
