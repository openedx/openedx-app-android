package org.openedx.core.extension

/**
 * Safely divides this Float by [divisor], returning 0f if:
 *  - [divisor] is zero,
 *  - the result is NaN.
 *
 * Workaround for accessibility issue:
 * https://github.com/openedx/openedx-app-android/issues/442
 */
fun Float.safeDivBy(divisor: Float): Float = try {
    var result = this / divisor
    if (result.isNaN()) {
        result = 0f
    }
    result
} catch (_: ArithmeticException) {
    0f
}
