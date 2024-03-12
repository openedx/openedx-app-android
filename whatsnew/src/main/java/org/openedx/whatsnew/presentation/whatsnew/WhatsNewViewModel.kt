package org.openedx.whatsnew.presentation.whatsnew

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.openedx.core.BaseViewModel
import org.openedx.whatsnew.WhatsNewManager
import org.openedx.whatsnew.domain.model.WhatsNewItem
import org.openedx.whatsnew.presentation.WhatsNewAnalyticEvent
import org.openedx.whatsnew.presentation.WhatsNewAnalyticKey
import org.openedx.whatsnew.presentation.WhatsNewAnalyticValue
import org.openedx.whatsnew.presentation.WhatsNewAnalytics

class WhatsNewViewModel(
    val courseId: String?,
    val infoType: String?,
    private val whatsNewManager: WhatsNewManager,
    private val analytics: WhatsNewAnalytics,
) : BaseViewModel() {

    private val _whatsNewItem = mutableStateOf<WhatsNewItem?>(null)
    val whatsNewItem: State<WhatsNewItem?>
        get() = _whatsNewItem

    init {
        getNewestData()
    }

    private fun getNewestData() {
        _whatsNewItem.value = whatsNewManager.getNewestData()
    }

    fun logWhatsNewViewed() {
        logEvent(WhatsNewAnalyticEvent.WHATS_NEW_VIEW, emptyMap())
    }

    fun logWhatsNewCompleted() {
        logEvent(WhatsNewAnalyticEvent.WHATS_NEW_COMPLETED, buildMap {
            put(WhatsNewAnalyticKey.TOTAL_SCREENS.key, whatsNewItem.value?.messages?.size)
        })
    }


    private fun logEvent(event: WhatsNewAnalyticEvent, params: Map<String, Any?>) {
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(WhatsNewAnalyticKey.NAME.key, WhatsNewAnalyticValue.SCREEN_NAVIGATION.value)
                put(WhatsNewAnalyticKey.CATEGORY.key, WhatsNewAnalyticValue.WHATS_NEW.value)
                put(WhatsNewAnalyticKey.APP_VERSION.key, whatsNewItem.value?.version)
                putAll(params)
            })
    }
}
