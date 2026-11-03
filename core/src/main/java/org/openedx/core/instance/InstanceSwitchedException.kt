package org.openedx.core.instance

/**
 * The learner selected another site while an operation that belongs to one site was waiting on
 * the network. Its results were kept away from the newly selected site.
 */
class InstanceSwitchedException(val instanceKey: String?) :
    IllegalStateException("The selected site changed during an operation for $instanceKey")
