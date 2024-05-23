package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class DashboardConfig(
    @SerializedName("TYPE")
    private val viewType: String = DashboardType.GALLERY.name,
) {
    fun getType(): DashboardType {
        return DashboardType.valueOf(viewType.uppercase())
    }

    enum class DashboardType {
        LIST, GALLERY
    }
}
