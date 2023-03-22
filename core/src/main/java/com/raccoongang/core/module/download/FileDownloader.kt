package com.raccoongang.core.module.download

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FileDownloader : ProgressListener {

    private var firstUpdate = true

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(Interceptor { chain: Interceptor.Chain ->
            val originalResponse: Response = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body!!, this))
                .build()
        })
        .build()

    private val downloadApi = Retrofit.Builder()
        .baseUrl(com.raccoongang.core.BuildConfig.BASE_URL)
        .client(client)
        .build()
        .create(DownloadApi::class.java)

    private var currentDownloadingFilePath: String? = null

    var progressListener: CurrentProgress? = null

    var isCanceled = false

    private var input: InputStream? = null

    suspend fun download(
        url: String,
        path: String
    ): Boolean {
        isCanceled = false
        return try {
            val response = downloadApi.downloadFile(url).body()
            if (response != null) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                file.createNewFile()
                input = response.byteStream()
                currentDownloadingFilePath = path
                val fos = FileOutputStream(file)
                fos.use { output ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (input!!.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            input?.close()
        }
    }


    suspend fun cancelDownloading() {
        isCanceled = true
        withContext(Dispatchers.IO) {
            input?.close()
        }
        currentDownloadingFilePath?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        if (done) {
            Log.d("DownloadProgress", "done")
        } else {
            if (firstUpdate) {
                firstUpdate = false
                if (contentLength == -1L) {
                    Log.d("DownloadProgress", "content-length: unknown")
                } else {
                    Log.d("DownloadProgress", "content-length: $contentLength \n")
                }
            }
            Log.d("DownloadProgress", "$bytesRead")
            if (contentLength != -1L) {
                progressListener?.progress(100 * bytesRead / contentLength)
                Log.d("DownloadProgress", "${100 * bytesRead / contentLength} done")
            }
        }
    }

}

interface CurrentProgress {
    fun progress(value: Long)
}

interface DownloadApi {

    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): retrofit2.Response<ResponseBody>

}