package org.openedx.core.instance

import android.content.Context
import androidx.core.content.edit

/**
 * Where [InstanceStore] persists the key of the selected instance.
 */
interface InstanceSelectionStorage {
    var selectedInstanceKey: String?
}

class SharedPrefsInstanceSelectionStorage(context: Context) : InstanceSelectionStorage {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Written synchronously: a selection lost to a process kill right after a switch would
    // reopen the app on the previous site.
    override var selectedInstanceKey: String?
        get() = prefs.getString(KEY_SELECTED_INSTANCE, null)
        set(value) = prefs.edit(commit = true) {
            if (value == null) remove(KEY_SELECTED_INSTANCE) else putString(KEY_SELECTED_INSTANCE, value)
        }

    private companion object {
        const val PREFS_NAME = "openedx_instance_store"
        const val KEY_SELECTED_INSTANCE = "selected_instance_key"
    }
}
