package org.openedx.core.config

import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openedx.core.instance.Instance
import org.openedx.core.instance.InstanceProvider

class InstanceAwareConfigTest {

    private class Selection(var instance: Instance?) : InstanceProvider {
        override val currentInstance: Instance? get() = instance
        override val currentInstanceFlow: StateFlow<Instance?> = MutableStateFlow(null)
        override val hasMultipleInstances = true
    }

    private val appConfig = JsonParser.parseString(
        """
        {
          "API_HOST_URL": "https://app.example.com",
          "OAUTH_CLIENT_ID": "app-client",
          "SSO_URL": "https://app.example.com/sso",
          "SSO_FINISHED_URL": "https://app.example.com/sso/done",
          "TOKEN_TYPE": "JWT",
          "FEEDBACK_EMAIL_ADDRESS": "app@example.com",
          "FAQ_URL": "https://app.example.com/faq",
          "PLATFORM_NAME": "App Platform",
          "URI_SCHEME": "org.openedx.app",
          "WHATS_NEW_ENABLED": true,
          "SOCIAL_AUTH_ENABLED": true,
          "LOGIN_REGISTRATION_ENABLED": true,
          "SAML_SSO_LOGIN_ENABLED": false,
          "DASHBOARD": { "TYPE": "gallery" },
          "DISCOVERY": { "TYPE": "native" },
          "UI_COMPONENTS": {
            "COURSE_DROPDOWN_NAVIGATION_ENABLED": true,
            "COURSE_UNIT_PROGRESS_ENABLED": false
          },
          "FIREBASE": { "ENABLED": true, "PROJECT_ID": "app-firebase" },
          "EXPERIMENTAL_FEATURES": { "APP_LEVEL_DOWNLOADS": { "ENABLED": false } }
        }
        """
    ).asJsonObject

    private fun instance(json: String) = requireNotNull(Instance.fromJson(JsonParser.parseString(json).asJsonObject))

    private val minimal = instance(
        """{"KEY": "min", "NAME": "Minimal", "OAUTH_CLIENT_ID": "min-client",
            "API_HOST_URL": "https://min.example.com"}"""
    )

    private val full = instance(
        """
        {
          "KEY": "full", "NAME": "Full", "OAUTH_CLIENT_ID": "full-client",
          "API_HOST_URL": "https://full.example.com",
          "SSO_URL": "https://full.example.com/sso",
          "SSO_URL_SUCCESSFUL_LOGIN": "https://full.example.com/sso/done",
          "TOKEN_TYPE": "BEARER",
          "FEEDBACK_EMAIL_ADDRESS": "help@full.example.com",
          "FAQ_URL": "",
          "PLATFORM_NAME": "Full Platform",
          "WHATS_NEW_ENABLED": false,
          "URI_SCHEME": "evil.scheme",
          "FIREBASE": { "ENABLED": false },
          "DASHBOARD": { "TYPE": "list" },
          "UI_COMPONENTS": {
            "COURSE_UNIT_PROGRESS_ENABLED": true,
            "LOGIN_REGISTRATION_ENABLED": false,
            "SAML_SSO_LOGIN_ENABLED": true
          },
          "EXPERIMENTAL_FEATURES": { "APP_LEVEL_DOWNLOADS": { "ENABLED": true } }
        }
        """
    )

    private fun config(instance: Instance?) = InstanceAwareConfig(appConfig, Selection(instance))

    @Test
    fun `no selection reads the app level config unchanged`() {
        val config = config(null)

        assertEquals("https://app.example.com", config.getApiHostURL())
        assertEquals("app-client", config.getOAuthClientId())
        assertEquals("App Platform", config.getPlatformName())
        assertTrue(config.getCourseUIConfig().isCourseDropdownNavigationEnabled)
    }

    @Test
    fun `instance fields win when set`() {
        val config = config(full)

        assertEquals("https://full.example.com", config.getApiHostURL())
        assertEquals("full-client", config.getOAuthClientId())
        assertEquals("https://full.example.com/sso", config.getSSOURL())
        assertEquals("https://full.example.com/sso/done", config.getSSOFinishedURL())
        assertEquals("BEARER", config.getAccessTokenType())
        assertEquals("help@full.example.com", config.getFeedbackEmailAddress())
        assertEquals("Full Platform", config.getPlatformName())
        assertEquals(DashboardConfig.DashboardType.LIST, config.getDashboardConfig().getType())
        assertTrue(config.getDownloadsConfig().isEnabled)
    }

    @Test
    fun `fields an instance omits fall back to the app level`() {
        val config = config(minimal)

        assertEquals("https://min.example.com", config.getApiHostURL())
        assertEquals("app@example.com", config.getFeedbackEmailAddress())
        assertEquals("JWT", config.getAccessTokenType())
        assertEquals("https://app.example.com/sso", config.getSSOURL())
        assertEquals(DashboardConfig.DashboardType.GALLERY, config.getDashboardConfig().getType())
        assertFalse(config.getDownloadsConfig().isEnabled)
    }

    @Test
    fun `blank instance values count as unset`() {
        assertEquals("https://app.example.com/faq", config(full).getFaqUrl())
    }

    @Test
    fun `platform name defaults to the instance display name`() {
        assertEquals("Minimal", config(minimal).getPlatformName())
    }

    @Test
    fun `nested login flags override the matching top level keys`() {
        val config = config(full)

        assertFalse(config.isLoginRegistrationEnabled())
        assertTrue(config.isSSOLoginEnabled())
        assertTrue(config(minimal).isLoginRegistrationEnabled())
        assertFalse(config(minimal).isSSOLoginEnabled())
    }

    @Test
    fun `course UI flags merge per field`() {
        val ui = config(full).getCourseUIConfig()

        assertTrue(ui.isCourseUnitProgressEnabled)
        assertTrue(ui.isCourseDropdownNavigationEnabled)
    }

    @Test
    fun `app level only keys ignore the instance`() {
        val config = config(full)

        assertEquals("org.openedx.app", config.getUriScheme())
        assertTrue(config.isWhatsNewEnabled())
        assertTrue(config.getFirebaseConfig().enabled)
        assertEquals("app-firebase", config.getFirebaseConfig().projectId)
    }

    @Test
    fun `values follow the selection as it changes`() {
        val selection = Selection(minimal)
        val config = InstanceAwareConfig(appConfig, selection)

        assertEquals("https://min.example.com", config.getApiHostURL())
        selection.instance = full
        assertEquals("https://full.example.com", config.getApiHostURL())
        selection.instance = null
        assertEquals("https://app.example.com", config.getApiHostURL())
    }

    @Test
    fun `app level accessors never move with the selection`() {
        val config = config(full)

        assertEquals("https://app.example.com", config.getAppLevelApiHostURL())
        assertEquals("app-client", config.getAppLevelOAuthClientId())
    }
}
