package org.openedx.core.instance

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstancesConfigTest {

    private fun fixture(): JsonObject {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream("instances/bundled_instances.json"))
        return stream.reader().use { JsonParser.parseReader(it).asJsonObject }
    }

    private fun requiredFields(): JsonObject = JsonObject().apply {
        addProperty("KEY", "test-instance")
        addProperty("NAME", "Test Instance")
        addProperty("OAUTH_CLIENT_ID", "client-id")
        addProperty("API_HOST_URL", "https://test-instance.example.com")
    }

    @Test
    fun `bundled catalog parses every field of a complete entry`() {
        val catalog = InstancesConfig.fromAppConfig(fixture(), languageCode = "en")
        val instance = requireNotNull(catalog.instance("example-instance"))

        assertEquals("Example Instance", instance.name)
        assertEquals("Example Instance", instance.instanceName)
        assertEquals("example-oauth-client-id", instance.oAuthClientId)
        assertEquals("https://example-instance.example.com", instance.apiHostUrl)
        assertEquals(
            "https://example-instance.example.com/auth/login/tpa-saml/?auth_entry=login",
            instance.ssoUrl
        )
        assertEquals(
            "https://example-instance.example.com/auth/login/tpa-saml/auth/complete/tpa-saml",
            instance.ssoFinishedUrl
        )
        assertEquals("Example Instance", instance.environmentDisplayName)
        assertTrue(instance.isSwitchInstanceLoginEnabled)
        assertEquals("exampleInstanceAppLogo", instance.logoUrl)
        assertNull(instance.headerBackgroundUrl)
        assertEquals(mapOf("accent_color" to "#3C68FF"), instance.themeColors?.light)
        assertEquals(mapOf("accent_color" to "#1C355E"), instance.themeColors?.dark)
        assertTrue(instance.config.has("UI_COMPONENTS"))
    }

    @Test
    fun `malformed entries are dropped instead of half populated`() {
        val catalog = InstancesConfig.fromAppConfig(fixture(), languageCode = "en")

        assertEquals(listOf("example-instance", "second-universite"), catalog.instances.map { it.key })
        assertNull(catalog.instance("missing-client"))
    }

    @Test
    fun `each required field is enforced`() {
        listOf("NAME", "API_HOST_URL", "OAUTH_CLIENT_ID").forEach { field ->
            val json = requiredFields().apply { remove(field) }
            assertNull("missing $field must be rejected", Instance.fromJson(json))
        }
        val blankName = requiredFields().apply { addProperty("NAME", " ") }
        assertNull(Instance.fromJson(blankName))
        val relativeHost = requiredFields().apply { addProperty("API_HOST_URL", "test-instance.example.com") }
        assertNull(Instance.fromJson(relativeHost))
        val numericClient = requiredFields().apply { addProperty("OAUTH_CLIENT_ID", 42) }
        assertNull(Instance.fromJson(numericClient))
    }

    @Test
    fun `key falls back to a slug of the name`() {
        val catalog = InstancesConfig.fromAppConfig(fixture(), languageCode = "en")

        assertNotNull(catalog.instance("second-universite"))
        assertEquals("acme-u-of-arts", Instance.slugify("  ACME U. of Arts! "))
    }

    @Test
    fun `display name follows the language then English then NAME`() {
        assertEquals("اسم المُزود", InstancesConfig.fromAppConfig(fixture(), "ar").instances[0].instanceName)
        assertEquals("Example Instance", InstancesConfig.fromAppConfig(fixture(), "uk").instances[0].instanceName)
        assertEquals("Second Université", InstancesConfig.fromAppConfig(fixture(), "ar").instances[1].instanceName)
    }

    @Test
    fun `optional fields use defaults when omitted`() {
        val instance = requireNotNull(Instance.fromJson(requiredFields()))

        assertEquals("#007AFF", instance.color)
        assertEquals("Test Instance", instance.platformName)
        assertNull(instance.ssoUrl)
        assertNull(instance.ssoFinishedUrl)
        assertNull(instance.themeColors)
        assertNull(instance.logoUrl)
        assertFalse(instance.isSwitchInstanceLoginEnabled)
        assertTrue(instance.isRoundedCorners)
        assertEquals(8.0, instance.buttonCornersRadius, 0.0)
    }

    @Test
    fun `blank optional strings count as unset`() {
        val json = requiredFields().apply {
            addProperty("SSO_URL", "")
            addProperty("SSO_URL_SUCCESSFUL_LOGIN", " ")
            addProperty("PLATFORM_NAME", "")
            add("THEME", JsonParser.parseString("""{"LOGO_URL": "", "HEADER_BACKGROUND_URL": ""}"""))
        }
        val instance = requireNotNull(Instance.fromJson(json))

        assertNull(instance.ssoUrl)
        assertNull(instance.ssoFinishedUrl)
        assertNull(instance.logoUrl)
        assertNull(instance.headerBackgroundUrl)
        assertEquals("Test Instance", instance.platformName)
    }

    @Test
    fun `instance level values win when provided`() {
        val json = requiredFields().apply {
            addProperty("PLATFORM_NAME", "Custom Platform Name")
            addProperty("SSO_FINISHED_URL", "https://test-instance.example.com/finished")
            addProperty("SSO_URL_SUCCESSFUL_LOGIN", "https://test-instance.example.com/ignored")
            add("THEME", JsonParser.parseString("""{"ROUNDED_CORNERS_STYLE": false, "BUTTON_CORNERS_RADIUS": 4}"""))
        }
        val instance = requireNotNull(Instance.fromJson(json))

        assertEquals("Custom Platform Name", instance.platformName)
        assertEquals("https://test-instance.example.com/finished", instance.ssoFinishedUrl)
        assertFalse(instance.isRoundedCorners)
        assertEquals(4.0, instance.buttonCornersRadius, 0.0)
        assertEquals(InstanceThemeColors(emptyMap(), emptyMap()), instance.themeColors)
    }

    @Test
    fun `fallback fills omitted fields and never overrides the entry`() {
        val fallback = JsonObject().apply {
            addProperty("OAUTH_CLIENT_ID", "fallback-client")
            addProperty("FEEDBACK_EMAIL_ADDRESS", "fallback@example.com")
        }
        val entry = requiredFields().apply { remove("OAUTH_CLIENT_ID") }
        val withClient = requiredFields().apply { addProperty("KEY", "with-client") }
        val array = JsonArray().apply {
            add(entry)
            add(withClient)
        }

        val catalog = InstancesConfig.fromJson(array, fallback)

        assertEquals("fallback-client", catalog.instance("test-instance")?.oAuthClientId)
        assertEquals("client-id", catalog.instance("with-client")?.oAuthClientId)
        val feedbackEmail = catalog.instance("with-client")?.config?.get("FEEDBACK_EMAIL_ADDRESS")?.asString
        assertEquals("fallback@example.com", feedbackEmail)
        assertFalse(fallback.has("KEY"))
    }

    @Test
    fun `identity is the key, not the position in the catalog`() {
        val fixture = fixture()
        val reversed = JsonArray().apply {
            fixture.getAsJsonArray("INSTANCES").reversed().forEach { add(it) }
        }

        val original = InstancesConfig.fromAppConfig(fixture, "en")
        val reordered = InstancesConfig.fromJson(reversed, languageCode = "en")

        assertEquals(original.instance("example-instance"), reordered.instance("example-instance"))
        assertEquals(original.instance("second-universite"), reordered.instance("second-universite"))
    }

    @Test
    fun `missing or non array INSTANCES gives an empty catalog`() {
        assertTrue(InstancesConfig.fromAppConfig(JsonObject()).instances.isEmpty())
        val notArray = JsonObject().apply { addProperty("INSTANCES", "nope") }
        assertTrue(InstancesConfig.fromAppConfig(notArray).instances.isEmpty())
        val junk = JsonObject().apply { add("INSTANCES", JsonParser.parseString("""[1, "two", null]""")) }
        assertTrue(InstancesConfig.fromAppConfig(junk).instances.isEmpty())
    }
}
