package org.openedx.core.extension

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

inline fun <T> CancellableContinuation<T>.safeResume(value: T, onExceptionCalled: () -> Unit) {
    if (isActive) {
        resume(value)
    } else {
        onExceptionCalled()
    }
}
