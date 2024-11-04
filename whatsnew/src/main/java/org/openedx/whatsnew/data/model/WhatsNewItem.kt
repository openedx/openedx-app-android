package org.openedx.whatsnew.data.model

import android.content.Context
import com.google.gson.annotations.SerializedName

data class WhatsNewItem(
    @SerializedName("version")
    val version: String,
    @SerializedName("messages")
    val messages: List<WhatsNewMessage>
) {
    fun mapToDomain(context: Context) = org.openedx.whatsnew.domain.model.WhatsNewItem(
        version = version,
        messages = messages.map { it.mapToDomain(context) }
    )
}
