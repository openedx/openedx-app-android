package org.openedx.core.instance

import android.content.Context
import androidx.core.content.edit

/**
 * Where [InstanceStore] persists the key of the selected instance.
 */
interface InstanceSelectionStorage {
    var selectedInstanceKey: String?
}

/** Whether the first multi-site launch has already looked for a session from a single-site build. */
interface LegacySessionAdoptionStorage {
    var hasCheckedLegacySession: Boolean
}

class SharedPrefsInstanceSelectionStorage(context: Context) :
    InstanceSelectionStorage,
    LegacyNamespaceOwnerStorage,
    LegacySessionAdoptionStorage {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Written synchronously: a selection lost to a process kill right after a switch would
    // reopen the app on the previous site.
    override var selectedInstanceKey: String?
        get() = prefs.getString(KEY_SELECTED_INSTANCE, null)
        set(value) = prefs.edit(commit = true) {
            if (value == null) remove(KEY_SELECTED_INSTANCE) else putString(KEY_SELECTED_INSTANCE, value)
        }

    override var legacyOwnerInstanceKey: String?
        get() = prefs.getString(KEY_LEGACY_OWNER, null)
        set(value) = prefs.edit(commit = true) {
            if (value == null) remove(KEY_LEGACY_OWNER) else putString(KEY_LEGACY_OWNER, value)
        }

    override var hasCheckedLegacySession: Boolean
        get() = prefs.getBoolean(KEY_CHECKED_LEGACY_SESSION, false)
        set(value) = prefs.edit(commit = true) { putBoolean(KEY_CHECKED_LEGACY_SESSION, value) }

    private companion object {
        const val PREFS_NAME = "openedx_instance_store"
        const val KEY_SELECTED_INSTANCE = "selected_instance_key"
        const val KEY_LEGACY_OWNER = "legacy_storage_owner_instance_key"
        const val KEY_CHECKED_LEGACY_SESSION = "checked_legacy_session"
    }
}
