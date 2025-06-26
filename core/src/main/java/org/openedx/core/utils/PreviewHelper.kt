package org.openedx.core.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever

object PreviewHelper {

    fun getYouTubeThumbnailUrl(url: String): String {
        val videoId = extractYouTubeVideoId(url)
        return "https://img.youtube.com/vi/$videoId/0.jpg"
    }

    private fun extractYouTubeVideoId(url: String): String {
        val regex = Regex(
            "^(?:https?://)?(?:www\\.)?(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})",
            RegexOption.IGNORE_CASE
        )
        val matchResult = regex.find(url)
        return matchResult?.groups?.get(1)?.value ?: ""
    }


    fun getVideoFrameBitmap(isOnline: Boolean, videoUrl: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            if (!isOnline && !isLocalFile(videoUrl)) return null
            if (isLocalFile(videoUrl)) {
                retriever.setDataSource(videoUrl)
            } else {
                retriever.setDataSource(videoUrl, HashMap())
            }
            return retriever.getFrameAtTime(0)
        } catch (_: Exception) {
            return null
        } finally {
            retriever.release()
        }
    }

    private fun isLocalFile(url: String): Boolean {
        return url.startsWith("/") || url.startsWith("file://")
    }
}