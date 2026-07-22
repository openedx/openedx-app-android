package org.openedx.core.lmsdirectory

import android.content.Context
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.openedx.core.config.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Koin module for the LMS registry catalog (search/browse + complaint reporting).
 * Builds a clean Retrofit client pointed at the directory URL from the config flag.
 */
val lmsDirectoryModule = module {

    single(qualifier = named("LmsDirectory")) {
        OkHttpClient.Builder()
            .connectTimeout(DIRECTORY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DIRECTORY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    single<LmsDirectoryApi> {
        val rawUrl = get<Config>().getLMSDirectoryConfig().directoryUrl
        Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(rawUrl))
            .client(get(qualifier = named("LmsDirectory")))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LmsDirectoryApi::class.java)
    }

    single {
        val context = get<Context>()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
        LmsDirectoryRepository(api = get(), appVersion = version)
    }
}

private const val DIRECTORY_TIMEOUT_SECONDS = 20L

/** Retrofit requires an absolute URL ending in "/". Blank config yields a safe stub. */
private fun normalizeBaseUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return "https://directory.invalid/"
    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
    return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
}
