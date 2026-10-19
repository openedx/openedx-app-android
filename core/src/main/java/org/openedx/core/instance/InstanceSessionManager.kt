package org.openedx.core.instance

import org.openedx.core.DatabaseManager
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.system.AppCookieManager
import org.openedx.core.ui.theme.InstanceTheme
import org.openedx.core.ui.theme.InstanceThemeManager

/** Stops one site's backend from sending push notifications to this device. */
interface PushTokenUnregistering {
    /** Needs the site's session, so it runs before the session is cleared. */
    suspend fun unregisterPushToken(instanceKey: String?)
}

interface InstanceSessionManager {
    /**
     * Makes [instance] the active site. Nothing is torn down: the outgoing site keeps its
     * session, cached content and downloads for when the learner comes back.
     */
    suspend fun switchActiveInstance(instance: Instance)

    /**
     * Signs out of the selected site: cancels downloads, unregisters push, clears its tokens
     * and user, then clears the selection. Cached courses and downloaded files stay, so signing
     * back in to the same site is fast.
     */
    suspend fun logoutCurrentInstance()

    /**
     * Signs out of [instance]. The selected site goes through [logoutCurrentInstance]; any other
     * site has push unregistered and loses its stored session, and the active one is untouched.
     */
    suspend fun logout(instance: Instance)

    /**
     * [logout] by key, for a sign-out that started on one site and must finish there even if
     * the learner switched in between. `null` is the no-instance (single-site) session.
     */
    suspend fun logout(instanceKey: String?)

    /**
     * Records who signed in to [instanceKey]'s site. When it isn't the learner who used that site
     * last, that learner's cached course data is cleared first.
     */
    fun onSignedIn(instanceKey: String?, username: String)

    /**
     * Applies the selected instance's branding, or the default theme when nothing is selected.
     * Restoring a saved selection at launch doesn't go through [switchActiveInstance], so launch
     * calls this once the catalog is resolved.
     */
    fun applyThemeForCurrentInstance()
}

class DefaultInstanceSessionManager(
    private val instanceStore: InstanceStore,
    private val preferences: CorePreferences,
    private val downloadWorkerController: DownloadWorkerController,
    private val databaseManager: DatabaseManager,
    private val cookieManager: AppCookieManager,
    private val pushTokenUnregistrar: PushTokenUnregistering? = null,
) : InstanceSessionManager {

    override suspend fun switchActiveInstance(instance: Instance) {
        // Web content signs in with session cookies fetched for one site; the next page has to
        // fetch the new site's (refetching replaces the old ones).
        cookieManager.invalidateSessionCookie()
        instanceStore.select(instance)
        applyThemeForCurrentInstance()
    }

    override suspend fun logoutCurrentInstance() {
        downloadWorkerController.cancelUnfinishedDownloads()
        // Before the tokens go: unregistering is an authenticated call.
        pushTokenUnregistrar?.unregisterPushToken(instanceStore.currentInstance?.key)
        preferences.clearCorePreferences()
        cookieManager.clearWebViewCookie()
        // Last, so nothing observes a half signed-out site.
        instanceStore.select(null)
        applyThemeForCurrentInstance()
    }

    override suspend fun logout(instance: Instance) = logout(instance.key)

    override suspend fun logout(instanceKey: String?) {
        if (instanceStore.currentInstance?.key == instanceKey) {
            logoutCurrentInstance()
        } else {
            pushTokenUnregistrar?.unregisterPushToken(instanceKey)
            preferences.clearSession(instanceKey)
        }
    }

    override fun onSignedIn(instanceKey: String?, username: String) {
        val previous = preferences.lastSignedInUsername(instanceKey)
        if (previous.isNotEmpty() && previous != username) {
            databaseManager.clearTables(instanceKey)
        }
        preferences.setLastSignedInUsername(instanceKey, username)
    }

    override fun applyThemeForCurrentInstance() {
        InstanceThemeManager.applyTheme(instanceStore.currentInstance?.toTheme())
    }
}

fun Instance.toTheme() = InstanceTheme(
    key = key,
    accentColorHex = color,
    light = themeColors?.light.orEmpty(),
    dark = themeColors?.dark.orEmpty(),
    logoUrl = logoUrl,
    headerBackgroundUrl = headerBackgroundUrl,
)
