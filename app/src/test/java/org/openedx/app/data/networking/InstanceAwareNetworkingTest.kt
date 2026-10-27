package org.openedx.app.data.networking

import com.google.gson.JsonObject
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceProvider
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.LogoutEvent

class InstanceAwareNetworkingTest {

    private class FakeInstanceProvider(instance: Instance?) : InstanceProvider {
        private val flow = MutableStateFlow(instance)
        override val currentInstance: Instance? get() = flow.value
        override val currentInstanceFlow: StateFlow<Instance?> = flow
        override val hasMultipleInstances: Boolean = true
        fun select(instance: Instance?) {
            flow.value = instance
        }
    }

    private fun instance(key: String, host: String) = requireNotNull(
        Instance.fromJson(
            JsonObject().apply {
                addProperty("KEY", key)
                addProperty("NAME", key)
                addProperty("OAUTH_CLIENT_ID", "$key-client")
                addProperty("API_HOST_URL", host)
            }
        )
    )

    private val appBase = "https://app.example.com"
    private val alpha = instance("alpha", "https://alpha.example.com")
    private val beta = instance("beta", "http://beta.example.com:8080")

    /** Runs [interceptor] on [request] and returns the request it forwarded. */
    private fun forward(interceptor: Interceptor, request: Request): Request {
        var forwarded: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                forwarded = chain.request()
                ok(chain.request(), "{}")
            }
            .build()
        client.newCall(request).execute().close()
        return requireNotNull(forwarded)
    }

    private fun ok(request: Request, body: String, code: Int = 200) = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("status $code")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()

    // region InstanceHostInterceptor

    @Test
    fun `app base requests are re-targeted at the selected instance, path and query preserved`() {
        val interceptor = InstanceHostInterceptor(appBase, FakeInstanceProvider(beta))

        val forwarded = forward(
            interceptor,
            Request.Builder().url("https://app.example.com/api/courses/v1/?page=2&size=10").build()
        )

        assertEquals("http://beta.example.com:8080/api/courses/v1/?page=2&size=10", forwarded.url.toString())
        assertEquals(RequestInstance("beta"), forwarded.tag(RequestInstance::class.java))
    }

    @Test
    fun `no selected instance passes requests through`() {
        val interceptor = InstanceHostInterceptor(appBase, FakeInstanceProvider(null))

        val forwarded = forward(interceptor, Request.Builder().url("https://app.example.com/api/x").build())

        assertEquals("https://app.example.com/api/x", forwarded.url.toString())
        assertEquals(RequestInstance(null), forwarded.tag(RequestInstance::class.java))
    }

    @Test
    fun `third party hosts are left untouched`() {
        val interceptor = InstanceHostInterceptor(appBase, FakeInstanceProvider(alpha))

        listOf(
            "https://cdn.example.net/video.mp4",
            "http://app.example.com/api/x",
            "https://app.example.com:8443/api/x",
        ).forEach { url ->
            assertEquals(url, forward(interceptor, Request.Builder().url(url).build()).url.toString())
        }
    }

    @Test
    fun `requests already built for the instance host stay as they are`() {
        val interceptor = InstanceHostInterceptor(appBase, FakeInstanceProvider(alpha))
        val request = Request.Builder().url("https://alpha.example.com/api/x").build()

        assertSame(request, interceptor.rewriteHostIfNeeded(request, alpha.apiHostUrl))
    }

    // endregion

    // region HeadersInterceptor

    private fun headersInterceptor(provider: InstanceProvider): HeadersInterceptor {
        val config = mockk<Config> {
            every { getAccessTokenType() } returns "JWT"
            every { getApiHostURL() } returns appBase
        }
        val preferences = mockk<CorePreferences> { every { accessToken } returns "token-a" }
        val appData = AppData(appName = "app", versionName = "1", applicationId = "org.openedx.app")
        return HeadersInterceptor(appData, config, preferences, provider)
    }

    @Test
    fun `token is sent to the active site only`() {
        val interceptor = headersInterceptor(FakeInstanceProvider(alpha))

        val toSite = forward(interceptor, Request.Builder().url("https://alpha.example.com/api/x").build())
        val elsewhere = forward(interceptor, Request.Builder().url("https://beta.example.com/api/x").build())

        assertEquals("JWT token-a", toSite.header("Authorization"))
        assertNull(elsewhere.header("Authorization"))
    }

    @Test
    fun `without a selection the app level host gets the token`() {
        val interceptor = headersInterceptor(FakeInstanceProvider(null))

        val forwarded = forward(interceptor, Request.Builder().url("https://app.example.com/api/x").build())

        assertEquals("JWT token-a", forwarded.header("Authorization"))
    }

    // endregion

    // region OauthRefreshTokenAuthenticator

    private class Prefs : CorePreferences by mockk(relaxed = true) {
        override var accessToken: String = "old-access"
        override var refreshToken: String = "old-refresh"
        override var accessTokenExpiresAt: Long = Long.MAX_VALUE
    }

    private fun unauthorized(request: Request) = ok(request, """{"detail": "Token has expired."}""", code = 401)

    private fun taggedRequest(url: String, key: String?) = Request.Builder()
        .url(url)
        .header("Authorization", "JWT old-access")
        .tag(RequestInstance::class.java, RequestInstance(key))
        .build()

    @Test
    fun `refresh uses the selected instance host and OAuth client`() {
        val tokenRequests = mutableListOf<Request>()
        val refreshClient = OkHttpClient.Builder().addInterceptor { chain ->
            tokenRequests += chain.request()
            ok(
                chain.request(),
                """{"access_token": "new-access", "refresh_token": "new-refresh", "expires_in": 3600}"""
            )
        }.build()
        val prefs = Prefs()
        val config = mockk<Config>(relaxed = true) {
            every { getAccessTokenType() } returns "JWT"
            every { getApiHostURL() } returns appBase
            every { getOAuthClientId() } returns "app-client"
        }
        val authenticator = OauthRefreshTokenAuthenticator(
            config,
            prefs,
            mockk(relaxed = true),
            FakeInstanceProvider(beta),
            refreshClient
        )

        val retry = authenticator.authenticate(
            null,
            unauthorized(taggedRequest("http://beta.example.com:8080/api/x", "beta"))
        )

        val tokenRequest = tokenRequests.single()
        assertEquals("http://beta.example.com:8080/oauth2/access_token/", tokenRequest.url.toString())
        val form = Buffer().also { tokenRequest.body!!.writeTo(it) }.readUtf8()
        assertTrue(form, form.contains("client_id=beta-client"))
        assertEquals("JWT new-access", retry?.header("Authorization"))
        assertEquals("new-access", prefs.accessToken)
    }

    @Test
    fun `a 401 for a request sent before a switch neither refreshes nor logs out`() {
        val refreshClient = OkHttpClient.Builder()
            .addInterceptor { throw AssertionError("no refresh expected") }
            .build()
        val notifier = mockk<AppNotifier>(relaxed = true)
        val prefs = Prefs()
        val authenticator = OauthRefreshTokenAuthenticator(
            mockk(relaxed = true),
            prefs,
            notifier,
            FakeInstanceProvider(beta),
            refreshClient
        )

        val retry = authenticator.authenticate(
            null,
            unauthorized(taggedRequest("https://alpha.example.com/api/x", "alpha"))
        )

        assertNull(retry)
        assertEquals("old-access", prefs.accessToken)
        coVerify(exactly = 0) { notifier.send(any<LogoutEvent>()) }
    }

    @Test
    fun `tokens refreshed while the learner switched sites are discarded`() {
        val provider = FakeInstanceProvider(alpha)
        val refreshClient = OkHttpClient.Builder().addInterceptor { chain ->
            provider.select(beta)
            ok(
                chain.request(),
                """{"access_token": "alpha-new", "refresh_token": "alpha-refresh", "expires_in": 3600}"""
            )
        }.build()
        val prefs = Prefs()
        val authenticator = OauthRefreshTokenAuthenticator(
            mockk(relaxed = true) { every { getAccessTokenType() } returns "JWT" },
            prefs,
            mockk(relaxed = true),
            provider,
            refreshClient
        )

        val retry = authenticator.authenticate(
            null,
            unauthorized(taggedRequest("https://alpha.example.com/api/x", "alpha"))
        )

        assertNull(retry)
        assertEquals("old-access", prefs.accessToken)
        assertNotNull(provider.currentInstance)
    }

    // endregion
}
