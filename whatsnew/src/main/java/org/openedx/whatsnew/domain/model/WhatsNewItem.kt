package org.openedx.whatsnew.domain.model

data class WhatsNewItem(
    val version: String,
    val messages: List<WhatsNewMessage>
)
