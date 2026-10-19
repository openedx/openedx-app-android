package org.openedx.core.instance

import org.openedx.core.DatabaseManager
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.ui.theme.InstanceTheme
import org.openedx.core.ui.theme.InstanceThemeManager

/** Unregisters this device's push token from the selected instance's backend. */
interface PushTokenUnregistering {
    suspend fun unregisterCurrentPushToken()
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
     * site only loses its stored session, and the active one is untouched.
     */
    suspend fun logout(instance: Instance)

    /**
     * Records who signed in to the selected site. When it isn't the learner who used it last,
     * that learner's cached course data is cleared first.
     */
    fun onSignedIn(username: String)

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
    private val pushTokenUnregistrar: PushTokenUnregistering? = null,
) : InstanceSessionManager {

    override suspend fun switchActiveInstance(instance: Instance) {
        instanceStore.select(instance)
        applyThemeForCurrentInstance()
    }

    override suspend fun logoutCurrentInstance() {
        downloadWorkerController.cancelUnfinishedDownloads()
        // Before the tokens go: unregistering is an authenticated call.
        pushTokenUnregistrar?.unregisterCurrentPushToken()
        preferences.clearCorePreferences()
        // Last, so nothing observes a half signed-out site.
        instanceStore.select(null)
        applyThemeForCurrentInstance()
    }

    override suspend fun logout(instance: Instance) {
        if (instanceStore.currentInstance?.key == instance.key) {
            logoutCurrentInstance()
        } else {
            preferences.clearSession(instance.key)
        }
    }

    override fun onSignedIn(username: String) {
        val previous = preferences.lastSignedInUsername
        if (previous.isNotEmpty() && previous != username) {
            databaseManager.clearTables()
        }
        preferences.lastSignedInUsername = username
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
