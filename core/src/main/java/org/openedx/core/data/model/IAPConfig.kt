package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.IAPConfig as DomainIAPConfig

/**
 * Model class that contains the Config related to In App Purchases.
 */
data class IAPConfig(

    @SerializedName("enabled")
    val isEnabled: Boolean = false,

    @SerializedName("android_product_prefix")
    val productPrefix: String = "",

    @SerializedName("android_disabled_versions")
    val disableVersions: List<String> = listOf()

) {
    fun mapToDomain(): DomainIAPConfig {
        return DomainIAPConfig(
            isEnabled = isEnabled,
            productPrefix = productPrefix,
            disableVersions = disableVersions
        )
    }
}
