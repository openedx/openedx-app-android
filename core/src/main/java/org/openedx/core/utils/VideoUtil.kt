package org.openedx.core.utils

import org.openedx.core.AppDataConstants.VIDEO_FORMAT_M3U8
import org.openedx.core.AppDataConstants.VIDEO_FORMAT_MP4

object VideoUtil {

    val SUPPORTED_VIDEO_FORMATS = arrayOf(
        VIDEO_FORMAT_MP4,
        VIDEO_FORMAT_M3U8
    )

    /**
     * Check the validity of video url.
     *
     * @param videoUrl Url which needs to be validated.
     * @return `true` if video url is valid, `false` otherwise.
     */
    fun isValidVideoUrl(videoUrl: String): Boolean {
        return videoHasFormat(videoUrl, *SUPPORTED_VIDEO_FORMATS)
    }

    /**
     * Check if format of video url exists in specified list of video formats.
     *
     * @param videoUrl         Video url whose format needs to be matched.
     * @param supportedFormats List of video formats.
     * @return `true` if video url format exist in specified formats list, `false` otherwise.
     */
    fun videoHasFormat(videoUrl: String, vararg supportedFormats: String): Boolean {
        for (format in supportedFormats) {
            /*
             * Its better to find a video format extension in the whole url because there is a
             * possibility that extension exists somewhere in between of url for e.g.
             * https://player.vimeo.com/external/225003478.m3u8?s=6438b130458bd0eb38f7625ffa26623caee8ff7c
             */
            if (videoUrl.contains(format)) {
                return true
            }
        }
        return false
    }
}
