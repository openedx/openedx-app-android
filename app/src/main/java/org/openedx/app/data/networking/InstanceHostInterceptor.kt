package org.openedx.app.data.networking

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.instance.RequestInstance

/**
 * Re-targets requests built against the app's base URL at the selected instance: scheme, host
 * and port change, path and query stay. Requests to any other host (CDNs, third-party SDKs)
 * and every request made while no instance is selected pass through untouched.
 */
class InstanceHostInterceptor(
    appBaseUrl: String,
    private val instanceProvider: InstanceProvider,
    private val instanceByKey: (String) -> Instance? = { null },
) : Interceptor {

    private val appBase: HttpUrl? = appBaseUrl.toHttpUrlOrNull()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // A request pinned to a site by its caller goes there; any other goes to the selected one.
        val pinned = request.tag(RequestInstance::class.java)
        val target = if (pinned != null) pinned.instanceKey?.let(instanceByKey) else instanceProvider.currentInstance
        val rewritten = rewriteHostIfNeeded(request, target?.apiHostUrl)
            .newBuilder()
            .tag(RequestInstance::class.java, pinned ?: RequestInstance(target?.key))
            .build()
        return chain.proceed(rewritten)
    }

    internal fun rewriteHostIfNeeded(request: Request, instanceHostUrl: String?): Request {
        val target = instanceHostUrl?.toHttpUrlOrNull()
        val base = appBase
        val url = request.url
        if (target == null || base == null || !url.isSameOrigin(base) || url.isSameOrigin(target)) {
            return request
        }
        val newUrl = url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()
        return request.newBuilder().url(newUrl).build()
    }
}

internal fun HttpUrl.isSameOrigin(other: HttpUrl): Boolean {
    return scheme == other.scheme && host == other.host && port == other.port
}
