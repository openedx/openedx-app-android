package org.openedx.core.instance

import java.net.URI
import java.security.MessageDigest
import java.util.Locale

/**
 * Where one instance's local data lives: preference keys, the database file and the
 * downloads folder.
 *
 * The legacy namespace ([instanceKey] `null`) is the storage a single-site build has always
 * used, unprefixed. It belongs to "no instance selected" and to the one instance that is the
 * same site as the app-level configuration, so upgrading keeps a signed-in learner's session,
 * cached courses and downloads in place.
 */
data class StorageNamespace(val instanceKey: String?) {

    val isLegacy: Boolean get() = instanceKey == null

    /** e.g. `access_token` for the legacy namespace, `instance.acme.access_token` otherwise. */
    fun key(rawKey: String): String = if (instanceKey == null) rawKey else "$PREFIX$instanceKey.$rawKey"

    /**
     * A file-system-safe, collision-resistant name segment, or `null` for the legacy namespace.
     * The readable part is lossy; the hash keeps two keys that sanitize alike apart.
     */
    val fileSegment: String?
        get() = instanceKey?.let { key ->
            val readable = key.replace(UNSAFE_FILE_CHARS, "_").take(MAX_READABLE_LENGTH)
            "${readable}_${sha256(key).take(HASH_LENGTH)}"
        }

    companion object {
        val LEGACY = StorageNamespace(null)

        private const val PREFIX = "instance."
        private const val MAX_READABLE_LENGTH = 32
        private const val HASH_LENGTH = 12
        private val UNSAFE_FILE_CHARS = "[^A-Za-z0-9_-]".toRegex()

        private fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}

/** Resolves instances to the namespace their local data lives under. */
interface StorageNamespaceProvider {
    /** The selected instance's namespace; legacy when nothing is selected. */
    val current: StorageNamespace

    fun namespaceFor(instance: Instance?): StorageNamespace

    /** Resolves by key through the catalog; a key no longer in the catalog keeps its own namespace. */
    fun namespaceFor(instanceKey: String?): StorageNamespace
}

/** Persists which instance owns the legacy namespace once that has been decided. */
interface LegacyNamespaceOwnerStorage {
    var legacyOwnerInstanceKey: String?
}

/**
 * The instance whose API host and OAuth client are the app-level ones is the site a
 * single-site build of this app was signing in to, so it inherits the legacy namespace. That
 * decision is recorded the first time it is made and never revisited: a later catalog or
 * configuration change can't hand existing data to a different site.
 */
class InstanceStorageNamespaces(
    private val instanceStore: InstanceStore,
    private val ownerStorage: LegacyNamespaceOwnerStorage,
    private val appApiHostUrl: String,
    private val appOAuthClientId: String,
) : StorageNamespaceProvider {

    private val lock = Any()

    override val current: StorageNamespace
        get() = namespaceFor(instanceStore.currentInstance)

    override fun namespaceFor(instance: Instance?): StorageNamespace {
        if (instance == null) return StorageNamespace.LEGACY
        val owner = synchronized(lock) {
            ownerStorage.legacyOwnerInstanceKey ?: instance.key
                .takeIf { isAppLevelSite(instance) }
                ?.also { ownerStorage.legacyOwnerInstanceKey = it }
        }
        return if (owner == instance.key) StorageNamespace.LEGACY else StorageNamespace(instance.key)
    }

    override fun namespaceFor(instanceKey: String?): StorageNamespace {
        val instance = instanceKey?.let { instanceStore.instance(it) }
        val owner = synchronized(lock) { ownerStorage.legacyOwnerInstanceKey }
        return when {
            instanceKey == null -> StorageNamespace.LEGACY
            instance != null -> namespaceFor(instance)
            owner == instanceKey -> StorageNamespace.LEGACY
            else -> StorageNamespace(instanceKey)
        }
    }

    private fun isAppLevelSite(instance: Instance): Boolean {
        return instance.oAuthClientId == appOAuthClientId &&
            normalizedOrigin(instance.apiHostUrl) != null &&
            normalizedOrigin(instance.apiHostUrl) == normalizedOrigin(appApiHostUrl)
    }

    private fun normalizedOrigin(url: String): String? = runCatching {
        val uri = URI(url.trim())
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        val port = when {
            uri.port != -1 -> uri.port
            scheme == "https" -> HTTPS_PORT
            else -> HTTP_PORT
        }
        "$scheme://$host:$port${uri.path.orEmpty().trimEnd('/')}"
    }.getOrNull()

    private companion object {
        const val HTTPS_PORT = 443
        const val HTTP_PORT = 80
    }
}
