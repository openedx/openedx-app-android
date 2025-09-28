package org.openedx.core.module.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.core.config.Config
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

abstract class AbstractDownloader : KoinComponent {

    private val config by inject<Config>()

    protected abstract val client: OkHttpClient

    private val downloadApi: DownloadApi by lazy {
        Retrofit.Builder()
            .baseUrl(config.getApiHostURL())
            .client(client)
            .build()
            .create(DownloadApi::class.java)
    }

    private var currentDownloadingFilePath: String? = null

    var isCanceled = false

    private var input: InputStream? = null
    private var fos: FileOutputStream? = null

    open suspend fun download(
        url: String,
        path: String
    ): DownloadResult {
        isCanceled = false
        return try {
            val responseBody = downloadApi.downloadFile(url).body() ?: return DownloadResult.ERROR
            initializeFile(path)
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(File(path)).use { outputStream ->
                    writeToFile(inputStream, outputStream)
                }
            }
            DownloadResult.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            if (isCanceled) DownloadResult.CANCELED else DownloadResult.ERROR
        } finally {
            closeResources()
        }
    }

    private fun initializeFile(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
        file.createNewFile()
        currentDownloadingFilePath = path
    }

    private fun writeToFile(inputStream: InputStream, outputStream: FileOutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.flush()
    }

    private fun closeResources() {
        fos?.close()
        input?.close()
        currentDownloadingFilePath = null
    }

    suspend fun cancelDownloading() {
        isCanceled = true
        withContext(Dispatchers.IO) {
            try {
                fos?.close()
                input?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        currentDownloadingFilePath?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    enum class DownloadResult {
        SUCCESS, CANCELED, ERROR
    }

    companion object {
        private const val BUFFER_SIZE = 4 * 1024
    }
}
