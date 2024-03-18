package org.openedx.whatsnew.presentation.whatsnew

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.openedx.core.BaseViewModel
import org.openedx.whatsnew.WhatsNewManager
import org.openedx.whatsnew.domain.model.WhatsNewItem
import org.openedx.whatsnew.presentation.WhatsNewAnalyticKey
import org.openedx.whatsnew.presentation.WhatsNewAnalytics
import org.openedx.whatsnew.presentation.WhatsNewAnalyticsEvent

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
        logEvent(WhatsNewAnalyticsEvent.WHATS_NEW_VIEW)
    }

    fun logWhatsNewCompleted() {
        logEvent(WhatsNewAnalyticsEvent.WHATS_NEW_DONE)
    }

    private fun logEvent(event: WhatsNewAnalyticsEvent) {
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(WhatsNewAnalyticKey.NAME.key, event.biValue)
                put(WhatsNewAnalyticKey.CATEGORY.key, WhatsNewAnalyticKey.WHATS_NEW.key)
                put(WhatsNewAnalyticKey.TOTAL_SCREENS.key, whatsNewItem.value?.messages?.size)
            })
    }
}
