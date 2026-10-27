package org.openedx.core.instance

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class InstanceConfigLoaderTest {

    private fun resource(name: String): String {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream("instances/$name"))
        return stream.reader().use { it.readText() }
    }

    private class FakeApi(private val result: () -> String) : InstanceApiService {
        var calls = 0
        override suspend fun fetchCatalog(): String {
            calls++
            return result()
        }
    }

    private class InMemoryCache(override var cachedCatalogJson: String? = null) : InstanceCatalogCache

    private val bundled = InstancesConfig.fromAppConfig(
        com.google.gson.JsonParser.parseString(
            """{"INSTANCES": [{"KEY": "bundled", "NAME": "Bundled", "OAUTH_CLIENT_ID": "c",
                "API_HOST_URL": "https://bundled.example.com"}]}"""
        ).asJsonObject
    )

    private val onlineCatalog = """[{"key": "online", "name": "Online", "oauth_client_id": "c",
        "api_host_url": "https://online.example.com"}]"""

    private fun loader(api: InstanceApiService, cache: InstanceCatalogCache, bundledConfig: InstancesConfig = bundled) =
        InstanceConfigLoader(api, cache, { bundledConfig }, logger = {})

    // region Remote JSON

    @Test
    fun `remote snake_case keys are normalized at every depth`() {
        val catalog = InstancesConfig.fromRemoteJson(resource("remote_catalog.json"), languageCode = "en")
        val instance = requireNotNull(catalog.instance("niepd"))

        assertEquals("NIEPD", instance.name)
        assertEquals("National Institute", instance.instanceName)
        assertEquals("#1C355E", instance.color)
        assertEquals("niepd-client", instance.oAuthClientId)
        assertEquals("https://lms.niepd.example.com", instance.apiHostUrl)
        assertEquals("https://lms.niepd.example.com/auth/complete/tpa-saml", instance.ssoFinishedUrl)
        assertTrue(instance.isSwitchInstanceLoginEnabled)
        assertEquals("https://cdn.example.com/niepd/logo.png", instance.logoUrl)
        assertEquals("https://cdn.example.com/niepd/header.png", instance.headerBackgroundUrl)
        val uiComponents = instance.config.getAsJsonObject("UI_COMPONENTS")
        assertTrue(uiComponents.get("COURSE_UNIT_PROGRESS_ENABLED").asBoolean)
        assertFalse(uiComponents.get("LOGIN_REGISTRATION_ENABLED").asBoolean)
    }

    @Test
    fun `palette field names inside THEME are left as they are`() {
        val instance = InstancesConfig.fromRemoteJson(resource("remote_catalog.json")).instances.single()

        assertEquals(mapOf("accent_color" to "#3C68FF", "text_primary" to "#18202E"), instance.themeColors?.light)
        assertEquals(mapOf("accent_color" to "#1C355E"), instance.themeColors?.dark)
    }

    @Test
    fun `unmapped remote keys pass through unchanged`() {
        val instance = InstancesConfig.fromRemoteJson(resource("remote_catalog.json")).instances.single()

        assertEquals("help@niepd.example.com", instance.config.get("FEEDBACK_EMAIL_ADDRESS").asString)
        assertEquals(
            "ignored-because-unmapped@example.com",
            instance.config.get("feedback_email_address").asString
        )
    }

    @Test
    fun `bare and wrapped arrays are both accepted`() {
        val entry = """{"key": "a", "name": "A", "oauth_client_id": "c", "api_host_url": "https://a.example.com"}"""

        listOf("[$entry]", """{"instances": [$entry]}""", """{"INSTANCES": [$entry]}""").forEach { json ->
            assertEquals(listOf("a"), InstancesConfig.fromRemoteJson(json).instances.map { it.key })
        }
    }

    @Test
    fun `unrecognized shapes are rejected`() {
        listOf("""{"sites": []}""", "\"text\"", "{not json").forEach { json ->
            try {
                InstancesConfig.fromRemoteJson(json)
                fail("$json should be rejected")
            } catch (_: InstancesJsonException) {
                // expected
            }
        }
    }

    @Test
    fun `remote entries follow the same required field rules`() {
        val catalog = InstancesConfig.fromRemoteJson(resource("remote_catalog.json"))

        assertNull(catalog.instance("broken"))
    }

    // endregion

    // region Loader

    @Test
    fun `non empty live catalog replaces the baseline and is cached`() = runBlocking {
        val cache = InMemoryCache()

        val result = loader(FakeApi { onlineCatalog }, cache).load()

        assertEquals(listOf("online"), result.instances.map { it.key })
        assertEquals(onlineCatalog, cache.cachedCatalogJson)
    }

    @Test
    fun `failed fetch falls back to the cache before the bundled catalog`() = runBlocking {
        val cache = InMemoryCache(onlineCatalog)

        val result = loader(FakeApi { throw IOException("offline") }, cache).load()

        assertEquals(listOf("online"), result.instances.map { it.key })
    }

    @Test
    fun `failed fetch without a cache uses the bundled catalog`() = runBlocking {
        val result = loader(FakeApi { throw IOException("timeout") }, InMemoryCache()).load()

        assertEquals(listOf("bundled"), result.instances.map { it.key })
    }

    @Test
    fun `failed fetch with nothing cached or bundled gives an empty catalog`() = runBlocking {
        val result = loader(FakeApi { throw IOException("offline") }, InMemoryCache(), InstancesConfig()).load()

        assertTrue(result.instances.isEmpty())
    }

    @Test
    fun `reachable but empty catalog never wipes a good cache`() = runBlocking {
        val cache = InMemoryCache(onlineCatalog)

        listOf("[]", """{"instances": []}""", """[{"key": "x"}]""").forEach { empty ->
            val result = loader(FakeApi { empty }, cache).load()
            assertEquals(listOf("online"), result.instances.map { it.key })
        }
        assertEquals(onlineCatalog, cache.cachedCatalogJson)
    }

    @Test
    fun `malformed response keeps the baseline`() = runBlocking {
        val cache = InMemoryCache(onlineCatalog)

        val result = loader(FakeApi { "<html>502</html>" }, cache).load()

        assertEquals(listOf("online"), result.instances.map { it.key })
        assertEquals(onlineCatalog, cache.cachedCatalogJson)
    }

    @Test
    fun `unparseable cache is ignored`() = runBlocking {
        val result = loader(FakeApi { throw IOException("offline") }, InMemoryCache("{broken")).load()

        assertEquals(listOf("bundled"), result.instances.map { it.key })
    }

    @Test
    fun `no configured URL uses the baseline without a network call`() = runBlocking {
        val service = OkHttpInstanceApiService(
            catalogUrl = "",
            client = OkHttpClient.Builder()
                .addInterceptor { throw AssertionError("no request expected") }
                .build()
        )

        val result = loader(service, InMemoryCache()).load()

        assertEquals(listOf("bundled"), result.instances.map { it.key })
    }

    // endregion

    // region Service

    private fun capturingClient(code: Int, body: String, captured: MutableList<Request>) = OkHttpClient.Builder()
        .addInterceptor(
            Interceptor { chain ->
            captured += chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("status $code")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
        )
        .build()

    @Test
    fun `catalog request is anonymous and bypasses caches`() = runBlocking {
        val captured = mutableListOf<Request>()
        val service = OkHttpInstanceApiService(
            catalogUrl = "https://catalog.example.com/instances.json",
            client = capturingClient(200, onlineCatalog, captured),
        )

        assertEquals(onlineCatalog, service.fetchCatalog())
        val request = captured.single()
        assertEquals("https://catalog.example.com/instances.json", request.url.toString())
        assertNull(request.header("Authorization"))
        assertNull(request.header("Cookie"))
        assertTrue(request.cacheControl.noCache)
    }

    @Test
    fun `non 2xx responses are errors`() = runBlocking {
        val service = OkHttpInstanceApiService(
            catalogUrl = "https://catalog.example.com/instances.json",
            client = capturingClient(503, "{}", mutableListOf()),
        )

        try {
            service.fetchCatalog()
            fail("503 must throw")
        } catch (e: InstanceCatalogHttpException) {
            assertEquals(503, e.code)
        }
    }

    @Test
    fun `default client times out after ten seconds`() {
        val client = OkHttpInstanceApiService.defaultClient()

        assertEquals(10_000, client.callTimeoutMillis)
        assertTrue(client.interceptors.isEmpty())
        assertTrue(client.networkInterceptors.isEmpty())
    }

    // endregion
}
