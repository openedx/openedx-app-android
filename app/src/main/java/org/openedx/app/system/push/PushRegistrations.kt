package org.openedx.app.system.push

import org.openedx.app.data.api.NotificationsApi
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.instance.InstanceStore
import org.openedx.core.instance.PushTokenUnregistering
import org.openedx.core.instance.RequestInstance

/**
 * The device has one push token; each site the learner is signed in to registers it with its
 * own backend. Registering and unregistering are pinned to one site, so they reach that site's
 * host with that site's session even if the learner switches sites while they run.
 */
class PushRegistrations(
    private val api: NotificationsApi,
    private val preferences: CorePreferences,
    private val config: Config,
    private val instanceStore: InstanceStore,
) : PushTokenUnregistering {

    private val isEnabled: Boolean
        get() = config.getFirebaseConfig().isCloudMessagingEnabled

    /** The sites with a session: catalog sites, or the single-site session (`null`) with no catalog. */
    fun signedInSites(): List<String?> {
        val catalog = instanceStore.instancesConfig.instances
        return if (catalog.isEmpty()) {
            if (preferences.hasSession(null)) listOf(null) else emptyList()
        } else {
            catalog.map { it.key }.filter { preferences.hasSession(it) }
        }
    }

    /** Registers this device's token with one site; does nothing for a site without a session. */
    suspend fun register(instanceKey: String?): Boolean {
        val token = preferences.pushToken
        if (!isEnabled || token.isEmpty() || !preferences.hasSession(instanceKey)) return false
        api.syncFirebaseToken(token, active = true, instance = RequestInstance(instanceKey))
        return true
    }

    override suspend fun unregisterPushToken(instanceKey: String?) {
        val token = preferences.pushToken
        if (!isEnabled || token.isEmpty() || !preferences.hasSession(instanceKey)) return
        // Best effort: signing out must not fail because the site is unreachable.
        runCatching {
            api.syncFirebaseToken(token, active = false, instance = RequestInstance(instanceKey))
        }
    }
}
