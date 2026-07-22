package org.openedx.app.data.networking

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test
import org.openedx.core.data.storage.CorePreferences

/**
 * Regression coverage for the LMS Directory login fix: with a platform selected,
 * every request must be routed to that host (the Retrofit client is built once,
 * before selection). No selection → the request is untouched.
 */
class BaseUrlOverrideInterceptorTest {

    private val corePreferences = mockk<CorePreferences>()

    private fun proceededRequest(selected: String?, requestUrl: String): Request {
        every { corePreferences.selectedBaseUrl } returns selected
        val original = Request.Builder().url(requestUrl).build()
        val captured = slot<Request>()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns original
        every { chain.proceed(capture(captured)) } returns mockk<Response>(relaxed = true)

        BaseUrlOverrideInterceptor(corePreferences).intercept(chain)
        return captured.captured
    }

    @Test
    fun `rewrites host to the selected LMS`() {
        val request = proceededRequest(
            selected = "https://sandbox.openedx.org/",
            requestUrl = "http://localhost:8000/oauth2/access_token",
        )
        assertEquals("sandbox.openedx.org", request.url.host)
        assertEquals("https", request.url.scheme)
        assertEquals("/oauth2/access_token", request.url.encodedPath)
    }

    @Test
    fun `passes request through when nothing is selected`() {
        val request = proceededRequest(
            selected = null,
            requestUrl = "http://localhost:8000/oauth2/access_token",
        )
        assertEquals("localhost", request.url.host)
        assertEquals(8000, request.url.port)
    }

    @Test
    fun `passes request through when selection is blank`() {
        val request = proceededRequest(
            selected = "",
            requestUrl = "https://config-host.example.com/api/v1/x",
        )
        assertEquals("config-host.example.com", request.url.host)
    }
}
