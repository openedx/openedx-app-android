package com.raccoongang.core.extension

inline fun <T> List<T>.indexOfFirstFromIndex(startIndex: Int, predicate: (T) -> Boolean): Int {
    var index = 0
    for ((i, item) in this.withIndex()) {
        if (i >= startIndex) {
            if (predicate(item))
                return index
        }
        index++
    }
    return -1
}