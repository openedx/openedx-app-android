package org.openedx.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

data class VideoPreview(
    val link: String? = null,
    val bitmap: Bitmap? = null
) {
    companion object {
        fun createYoutubePreview(link: String): VideoPreview {
            return VideoPreview(link = link)
        }

        fun createEncodedVideoPreview(bitmap: Bitmap): VideoPreview {
            return VideoPreview(bitmap = bitmap)
        }
    }
}

object PreviewHelper {

    private const val TIMEOUT_MS = 5000L // 5 seconds
    private val executor = Executors.newSingleThreadExecutor()

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

    fun getVideoFrameBitmap(context: Context, isOnline: Boolean, videoUrl: String): Bitmap? {
        var result: Bitmap? = null
        if (isOnline || isLocalFile(videoUrl)) {
            // Check cache first
            val cacheFile = getCacheFile(context, videoUrl)
            result = if (cacheFile.exists()) {
                try {
                    BitmapFactory.decodeFile(cacheFile.absolutePath)
                } catch (_: Exception) {
                    // If cache file is corrupted, try to extract from video with timeout
                    extractBitmapFromVideoWithTimeout(videoUrl, context)
                }
            } else {
                // Extract from video with timeout
                extractBitmapFromVideoWithTimeout(videoUrl, context)
            }
        }
        return result
    }

    private fun extractBitmapFromVideoWithTimeout(videoUrl: String, context: Context): Bitmap? {
        return try {
            val future = executor.submit<Bitmap?> {
                extractBitmapFromVideo(videoUrl, context)
            }
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            // Server didn't respond within timeout, return null immediately
            e.printStackTrace()
            null
        } catch (e: Exception) {
            // Any other exception, return null immediately
            e.printStackTrace()
            null
        }
    }

    private fun extractBitmapFromVideo(videoUrl: String, context: Context): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            if (isLocalFile(videoUrl)) {
                retriever.setDataSource(videoUrl)
            } else {
                retriever.setDataSource(videoUrl, HashMap())
            }
            val bitmap = retriever.getFrameAtTime(0)

            // Save bitmap to cache if it was successfully retrieved
            bitmap?.let {
                saveBitmapToCache(context, videoUrl, it)
            }

            return bitmap
        } catch (e: Exception) {
            // Log the exception for debugging but don't crash
            e.printStackTrace()
            return null
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

    private fun getCacheFile(context: Context, videoUrl: String): File {
        val cacheDir = context.cacheDir
        val fileName = generateFileName(videoUrl)
        return File(cacheDir, "video_thumbnails/$fileName")
    }

    private fun generateFileName(videoUrl: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(videoUrl.toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".jpg"
    }

    private fun saveBitmapToCache(context: Context, videoUrl: String, bitmap: Bitmap) {
        try {
            val cacheFile = getCacheFile(context, videoUrl)
            cacheFile.parentFile?.mkdirs() // Create directories if they don't exist

            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clear the bitmap cache to free storage
     */
    fun clearCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "video_thumbnails")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Remove a specific bitmap from cache
     */
    fun removeFromCache(context: Context, videoUrl: String) {
        try {
            val cacheFile = getCacheFile(context, videoUrl)
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
