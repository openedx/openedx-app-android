package org.openedx.core.module.download

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

class FileDownloader : AbstractDownloader(), ProgressListener {

    private var firstUpdate = true

    override val client: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(
            Interceptor { chain: Interceptor.Chain ->
                val originalResponse: Response = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(ProgressResponseBody(originalResponse.body!!, this))
                    .build()
            }
        )
        .build()
    var progressListener: CurrentProgress? = null

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
                progressListener?.progress(bytesRead, contentLength)
                Log.d("DownloadProgress", "${100 * bytesRead / contentLength} done")
            }
        }
    }
}

interface CurrentProgress {
    fun progress(value: Long, size: Long)
}

interface DownloadApi {

    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): retrofit2.Response<ResponseBody>
}
