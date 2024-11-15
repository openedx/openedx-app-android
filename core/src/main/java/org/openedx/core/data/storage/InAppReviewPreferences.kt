package org.openedx.core.data.storage

interface InAppReviewPreferences {
    var lastReviewVersion: VersionName
    var wasPositiveRated: Boolean

    fun setVersion(version: String) {
        lastReviewVersion = formatVersionName(version)
    }

    fun formatVersionName(version: String) = version
            .split(".")
            .let {
                VersionName(
                    majorVersion = it[0].toInt(),
                    minorVersion = it[1].toInt()
                )
            }

    data class VersionName(
        var majorVersion: Int,
        var minorVersion: Int
    ) {
        companion object {
            val default = VersionName(Int.MIN_VALUE, Int.MIN_VALUE)
        }
    }
}
