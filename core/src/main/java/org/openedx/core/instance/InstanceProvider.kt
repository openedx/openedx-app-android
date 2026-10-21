package org.openedx.core.instance

import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only, thread-safe access to the selected instance.
 */
interface InstanceProvider {
    /** `null` when nothing is selected, e.g. a build with no `INSTANCES` catalog. */
    val currentInstance: Instance?

    /** Emits the selected instance on every change, including deselection. */
    val currentInstanceFlow: StateFlow<Instance?>

    /** Whether the catalog has more than one instance to switch between. */
    val hasMultipleInstances: Boolean
}
