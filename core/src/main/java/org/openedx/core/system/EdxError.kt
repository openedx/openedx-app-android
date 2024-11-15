package org.openedx.core.system

import java.io.IOException

sealed class EdxError : IOException() {
    class InvalidGrantException : EdxError()
    class UserNotActiveException : EdxError()
    class ValidationException(val error: String) : EdxError()
    data class UnknownException(val error: String) : EdxError()
}
