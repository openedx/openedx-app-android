package org.openedx.core

import java.util.Locale

object AppDataConstants {
    const val USER_MIN_YEAR = 13
    const val USER_MAX_YEAR = 77
    const val DEFAULT_MIME_TYPE = "image/jpeg"
    val defaultLocale = Locale("en")

    const val VIDEO_FORMAT_M3U8 = ".m3u8"
    const val VIDEO_FORMAT_MP4 = ".mp4"

    // Equal 1GB
    const val DOWNLOADS_CONFIRMATION_SIZE = 1024 * 1024 * 1024L
}
