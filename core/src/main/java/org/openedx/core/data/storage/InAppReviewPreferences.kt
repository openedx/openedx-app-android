package org.openedx.core.data.storage

interface InAppReviewPreferences {
    var lastReviewMajorVersion: Int
    var lastReviewMinorVersion: Int
    var wasPositiveRated: Boolean

    fun setVersion(version: String) {
        version
            .split(".")
            .also {
                lastReviewMajorVersion = it[0].toInt()
                lastReviewMinorVersion = it[1].toInt()
            }
    }
}