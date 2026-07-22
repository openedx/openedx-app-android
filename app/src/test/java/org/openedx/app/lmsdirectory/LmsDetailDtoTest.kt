package org.openedx.app.lmsdirectory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.openedx.core.lmsdirectory.LmsDetailDto

/**
 * The catalog summary can't log you in — only the detail carries the per-LMS OAuth
 * client id and feedback email. This verifies the mapping the selection flow relies on.
 */
class LmsDetailDtoTest {

    @Test
    fun `maps api fields to domain`() {
        val detail = LmsDetailDto(
            id = "4",
            title = "Sandbox Env",
            baseUrl = "https://sandbox.openedx.org",
            logoUrl = "https://cdn.example.com/logo.png",
            accentColor = "#6a2e7b",
            api = LmsDetailDto.ApiDto(
                hostUrl = "https://sandbox.openedx.org",
                oauthClientId = "android",
                feedbackEmail = "team@example.com",
            ),
        ).toDomain()

        assertEquals("android", detail.oauthClientId)
        assertEquals("team@example.com", detail.feedbackEmail)
        assertEquals("https://sandbox.openedx.org", detail.baseUrl)
        assertEquals("#6a2e7b", detail.accentColor)
        assertEquals("https://cdn.example.com/logo.png", detail.logoUrl)
    }

    @Test
    fun `blank api values fall back to null and base_url`() {
        val detail = LmsDetailDto(
            id = "1",
            title = "Fallback",
            baseUrl = "https://fallback.example.com",
            api = LmsDetailDto.ApiDto(hostUrl = "", oauthClientId = "", feedbackEmail = null),
        ).toDomain()

        // Blank host_url → the top-level base_url is used.
        assertEquals("https://fallback.example.com", detail.baseUrl)
        assertNull(detail.oauthClientId)
        assertNull(detail.feedbackEmail)
    }

    @Test
    fun `null api yields base_url and null credentials`() {
        val detail = LmsDetailDto(
            id = "2",
            title = "No API block",
            baseUrl = "https://noapi.example.com",
            api = null,
        ).toDomain()

        assertEquals("https://noapi.example.com", detail.baseUrl)
        assertNull(detail.oauthClientId)
    }
}
