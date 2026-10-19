package org.openedx.core.instance

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstanceStoreTest {

    private class InMemorySelectionStorage(override var selectedInstanceKey: String? = null) :
        InstanceSelectionStorage

    private fun instance(key: String) = requireNotNull(
        Instance.fromJson(
            JsonObject().apply {
                addProperty("KEY", key)
                addProperty("NAME", key.uppercase())
                addProperty("OAUTH_CLIENT_ID", "$key-client")
                addProperty("API_HOST_URL", "https://$key.example.com")
            }
        )
    )

    private val alpha = instance("alpha")
    private val beta = instance("beta")

    private fun store(
        storage: InstanceSelectionStorage,
        vararg instances: Instance,
    ) = InstanceStore(storage) { InstancesConfig(instances.toList()) }

    @Test
    fun `single instance catalog with no prior selection auto selects it`() {
        val storage = InMemorySelectionStorage()

        val store = store(storage, alpha)

        assertEquals(alpha, store.currentInstance)
        assertEquals("alpha", storage.selectedInstanceKey)
        assertFalse(store.hasMultipleInstances)
    }

    @Test
    fun `multi instance catalog with no prior selection selects nothing`() {
        val store = store(InMemorySelectionStorage(), alpha, beta)

        assertNull(store.currentInstance)
        assertTrue(store.hasMultipleInstances)
    }

    @Test
    fun `persisted selection is restored by key`() {
        val store = store(InMemorySelectionStorage("beta"), alpha, beta)

        assertEquals(beta, store.currentInstance)
    }

    @Test
    fun `single instance catalog replaces a stale saved key`() {
        val storage = InMemorySelectionStorage("gone")

        val store = store(storage, alpha)

        assertEquals(alpha, store.currentInstance)
        assertEquals("alpha", storage.selectedInstanceKey)
    }

    @Test
    fun `empty catalog leaves nothing selected`() {
        val store = store(InMemorySelectionStorage())

        assertNull(store.currentInstance)
        assertFalse(store.hasMultipleInstances)
    }

    @Test
    fun `deselecting a single instance catalog selects it again`() {
        val store = store(InMemorySelectionStorage(), alpha)

        store.select(null)

        assertEquals(alpha, store.currentInstance)
    }

    @Test
    fun `deselecting a multi instance catalog stays deselected`() {
        val storage = InMemorySelectionStorage()
        val store = store(storage, alpha, beta)
        store.select(beta)

        store.reset()

        assertNull(store.currentInstance)
        assertNull(storage.selectedInstanceKey)
    }

    @Test
    fun `selection is published to the flow`() {
        val store = store(InMemorySelectionStorage(), alpha, beta)

        store.select(alpha)
        assertEquals(alpha, store.currentInstanceFlow.value)
        store.select(beta)
        assertEquals(beta, store.currentInstanceFlow.value)
    }

    @Test
    fun `a new catalog re-resolves the saved selection`() {
        val storage = InMemorySelectionStorage("beta")
        val store = store(storage, alpha)
        assertEquals(alpha, store.currentInstance)

        storage.selectedInstanceKey = "beta"
        store.updateInstancesConfig(InstancesConfig(listOf(beta, alpha)))

        assertEquals(beta, store.currentInstance)
        assertEquals(listOf(beta, alpha), store.instancesConfig.instances)
    }

    @Test
    fun `a site removed from the catalog resolves to nothing but stays saved`() {
        val storage = InMemorySelectionStorage()
        val store = store(storage, alpha, beta)
        store.select(beta)

        store.updateInstancesConfig(InstancesConfig(listOf(alpha, instance("gamma"))))
        assertNull(store.currentInstance)
        assertEquals("beta", storage.selectedInstanceKey)

        store.updateInstancesConfig(InstancesConfig(listOf(alpha, beta)))
        assertEquals(beta, store.currentInstance)
    }
}
