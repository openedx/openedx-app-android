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
            "^(?:https?://)?(?:www\\.)?(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)|.*[?&]v=)|youtu\\.be/)" +
                    "([^\"&?/\\s]{11})",
            RegexOption.IGNORE_CASE
        )
        val matchResult = regex.find(url)
        return matchResult?.groups?.get(1)?.value ?: ""
    }

    fun getVideoFrameBitmap(isOnline: Boolean, videoUrl: String): Bitmap? {
        if (!isOnline && !isLocalFile(videoUrl)) {
            return null
        }

        val retriever = MediaMetadataRetriever()
        return try {
            if (isLocalFile(videoUrl)) {
                retriever.setDataSource(videoUrl)
            } else {
                retriever.setDataSource(videoUrl, HashMap())
            }
            retriever.getFrameAtTime(0)
        } catch (e: Exception) {
            // Log the exception for debugging but don't crash
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release exceptions
                e.printStackTrace()
            }
        }
    }

    private fun isLocalFile(url: String): Boolean {
        return url.startsWith("/") || url.startsWith("file://")
    }
}
