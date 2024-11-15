package org.openedx.app.data.networking

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.openedx.app.BuildConfig
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.core.utils.TimeUtils
import java.util.Date

class AppUpgradeInterceptor(
    private val appNotifier: AppNotifier
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val responseCode = response.code
        val latestAppVersion = response.header(HEADER_APP_LATEST_VERSION) ?: ""
        val lastSupportedDateString = response.header(HEADER_APP_VERSION_LAST_SUPPORTED_DATE) ?: ""
        val lastSupportedDateTime = TimeUtils.iso8601WithTimeZoneToDate(lastSupportedDateString)?.time ?: 0L
        runBlocking {
            when {
                responseCode == 426 -> {
                    appNotifier.send(AppUpgradeEvent.UpgradeRequiredEvent)
                }

                BuildConfig.VERSION_NAME != latestAppVersion && lastSupportedDateTime > Date().time -> {
                    appNotifier.send(AppUpgradeEvent.UpgradeRecommendedEvent(latestAppVersion))
                }

                latestAppVersion.isNotEmpty() &&
                        BuildConfig.VERSION_NAME != latestAppVersion &&
                        lastSupportedDateTime < Date().time -> {
                    appNotifier.send(AppUpgradeEvent.UpgradeRequiredEvent)
                }
            }
        }
        return response
    }

    companion object {
        const val HEADER_APP_LATEST_VERSION = "EDX-APP-LATEST-VERSION"
        const val HEADER_APP_VERSION_LAST_SUPPORTED_DATE = "EDX-APP-VERSION-LAST-SUPPORTED-DATE"
    }
}
