package org.openedx.core.module

import android.content.Context
import okhttp3.OkHttpClient
import org.openedx.core.module.download.AbstractDownloader
import org.openedx.core.utils.Directories
import org.openedx.core.utils.IOUtils
import org.openedx.core.utils.Logger
import org.openedx.core.utils.Sha1Util
import org.openedx.foundation.utils.FileUtil
import subtitleFile.FormatSRT
import subtitleFile.TimedTextObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class TranscriptManager(
    val context: Context,
    val fileUtil: FileUtil
) {

    private val logger = Logger(TAG)

    private val transcriptDownloader = object : AbstractDownloader() {
        override val client: OkHttpClient
            get() = OkHttpClient.Builder().build()
    }

    private var transcriptObject: TimedTextObject? = null

    private fun has(url: String): Boolean {
        val transcriptDir = getTranscriptDir() ?: return false
        val hash = Sha1Util.SHA1(url)
        val file = File(transcriptDir, hash)
        return file.exists() && System.currentTimeMillis() - file.lastModified() < TimeUnit.HOURS.toMillis(
            FILE_VALIDITY_DURATION_HOURS
        )
    }

    fun get(url: String): String? {
        val transcriptDir = getTranscriptDir()
            ?: throw IOException("Transcript directory not found")
        val hash = Sha1Util.SHA1(url)
        val file = File(transcriptDir, hash)
        if (!file.exists()) {
            return null
        }
        return IOUtils.toString(file, Charset.defaultCharset())
    }

    fun getInputStream(url: String): InputStream? {
        val transcriptDir = getTranscriptDir()
            ?: throw IOException("Transcript directory not found")
        val hash = Sha1Util.SHA1(url)
        val file = File(transcriptDir, hash)
        return if (!file.exists()) {
            // not in cache
            null
        } else {
            FileInputStream(file)
        }
    }

    private suspend fun startTranscriptDownload(downloadLink: String) {
        if (has(downloadLink)) return
        val file = File(getTranscriptDir(), Sha1Util.SHA1(downloadLink))
        val result = transcriptDownloader.download(
            downloadLink,
            file.path
        )
        if (result == AbstractDownloader.DownloadResult.SUCCESS) {
            getInputStream(downloadLink)?.let {
                try {
                    transcriptObject = convertIntoTimedTextObject(it)
                } catch (e: NullPointerException) {
                    logger.e(throwable = e, submitCrashReport = true)
                }
            }
        }
    }

    suspend fun downloadTranscriptsForVideo(transcriptUrl: String): TimedTextObject? {
        transcriptObject = null
        if (transcriptUrl.isEmpty()) return null
        val transcriptInputStream = fetchTranscriptResponse(transcriptUrl)
        if (transcriptInputStream != null) {
            try {
                transcriptObject = convertIntoTimedTextObject(transcriptInputStream)
            } catch (e: Exception) {
                logger.e(throwable = e, submitCrashReport = true)
            }
        } else {
            startTranscriptDownload(transcriptUrl)
        }
        return transcriptObject
    }

    suspend fun cancelTranscriptDownloading() {
        transcriptDownloader.cancelDownloading()
    }

    private fun convertIntoTimedTextObject(inputStream: InputStream): TimedTextObject? {
        val timedTextObject = FormatSRT().parseFile("temp.srt", inputStream)
        inputStream.close()
        return timedTextObject
    }

    private fun fetchTranscriptResponse(url: String?): InputStream? {
        if (url == null) return null

        return try {
            if (has(url)) getInputStream(url) else null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getTranscriptDir(): File? {
        val externalAppDir: File = fileUtil.getExternalAppDir()
        if (externalAppDir.exists()) {
            val videosDir = File(externalAppDir, Directories.VIDEOS.name)
            val transcriptDir = File(videosDir, Directories.SUBTITLES.name)
            transcriptDir.mkdirs()
            return transcriptDir
        }
        return null
    }

    companion object {
        private const val TAG = "TranscriptManager"
        private const val FILE_VALIDITY_DURATION_HOURS = 5L
    }
}
