package org.openedx.core.presentation

enum class ListItemPosition {
    FIRST, MIDDLE, LAST, SINGLE;

    companion object {
        fun <T> detectPosition(index: Int, list: List<T>): ListItemPosition {
            return when {
                list.lastIndex == 0 -> SINGLE
                index == 0 -> FIRST
                index == list.lastIndex -> LAST
                else -> MIDDLE
            }
        }
    }
}
