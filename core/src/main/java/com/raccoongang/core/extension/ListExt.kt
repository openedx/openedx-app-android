package com.raccoongang.core.extension

inline fun <T> List<T>.indexOfFirstFromIndex(startIndex: Int, predicate: (T) -> Boolean): Int {
    var index = 0
    for ((i, item) in this.withIndex()) {
        if (i > startIndex) {
            if (predicate(item))
                return index
        }
        index++
    }
    return -1
}

fun <T> ArrayList<T>.clearAndAddAll(collection: Collection<T>): ArrayList<T> {
    this.clear()
    this.addAll(collection)
    return this
}

fun <T> MutableList<T>.clearAndAddAll(collection: Collection<T>): MutableList<T> {
    this.clear()
    this.addAll(collection)
    return this
}
