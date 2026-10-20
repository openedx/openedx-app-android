package org.openedx.core.instance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the instance catalog and the selected instance.
 *
 * The selection is persisted by key, so it survives restarts and catalog reordering. A
 * catalog with exactly one instance selects it automatically: there is no picker to choose
 * it through.
 */
class InstanceStore(
    private val storage: InstanceSelectionStorage,
    private val localInstancesConfig: () -> InstancesConfig = { InstancesConfig() },
) : InstanceProvider {

    private val lock = Any()
    private var remoteInstancesConfig: InstancesConfig? = null
    private val _currentInstance = MutableStateFlow<Instance?>(null)

    override val currentInstance: Instance?
        get() = _currentInstance.value

    override val currentInstanceFlow: StateFlow<Instance?> = _currentInstance.asStateFlow()

    override val hasMultipleInstances: Boolean
        get() = instancesConfig.instances.size > 1

    /** The catalog in effect: the one handed to [updateInstancesConfig], else the bundled one. */
    val instancesConfig: InstancesConfig
        get() = synchronized(lock) { remoteInstancesConfig ?: localInstancesConfig() }

    init {
        restoreSelection()
    }

    fun instance(withKey: String): Instance? = instancesConfig.instance(withKey)

    /** Replaces the catalog in effect and re-resolves the persisted selection against it. */
    fun updateInstancesConfig(instancesConfig: InstancesConfig) {
        synchronized(lock) { remoteInstancesConfig = instancesConfig }
        restoreSelection()
    }

    /** Selects [instance], or clears the selection with `null`, and persists the choice. */
    fun select(instance: Instance?) {
        synchronized(lock) {
            storage.selectedInstanceKey = instance?.key
            _currentInstance.value = instance
        }
        // A deselect on a single-instance catalog must not leave nothing selected until the
        // next launch.
        if (instance == null) {
            restoreSelection()
        }
    }

    fun reset() = select(null)

    private fun restoreSelection() {
        val catalog = instancesConfig
        val resolved = storage.selectedInstanceKey?.let { catalog.instance(it) }
        val onlyInstance = catalog.instances.singleOrNull()
        if (resolved == null && onlyInstance != null) {
            select(onlyInstance)
            return
        }
        // A saved key that the catalog no longer contains resolves to nothing, but stays saved:
        // if a later catalog brings that site back, it is selected again.
        _currentInstance.value = resolved
    }
}
