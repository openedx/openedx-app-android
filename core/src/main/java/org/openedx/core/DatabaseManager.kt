package org.openedx.core

interface DatabaseManager {
    /** Clears the selected site's cached course data. */
    fun clearTables()

    /** Clears one site's cached course data, whichever site is selected. */
    fun clearTables(instanceKey: String?)
}
