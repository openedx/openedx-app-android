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
        logEvent(WhatsNewAnalyticEvent.WHATS_NEW_VIEW, WhatsNewAnalyticValue.WHATS_NEW_VIEW)
    }

    fun logWhatsNewCompleted() {
        logEvent(WhatsNewAnalyticEvent.WHATS_NEW_DONE, WhatsNewAnalyticValue.WHATS_NEW_DONE)
    }


    private fun logEvent(event: WhatsNewAnalyticEvent, biValue: WhatsNewAnalyticValue) {
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(WhatsNewAnalyticKey.NAME.key, biValue.value)
                put(WhatsNewAnalyticKey.CATEGORY.key, WhatsNewAnalyticKey.WHATS_NEW.key)
                put(WhatsNewAnalyticKey.TOTAL_SCREENS.key, whatsNewItem.value?.messages?.size)
            })
    }
}
