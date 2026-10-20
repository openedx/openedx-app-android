package org.openedx.core.instance

/** The payload is neither a bare instance array nor an object wrapping one. */
class InstancesJsonException(message: String, cause: Throwable? = null) : Exception(message, cause)
