package org.openedx.whatsnew.presentation.whatsnew

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentManager
import org.openedx.core.BaseViewModel
import org.openedx.core.presentation.global.AppData
import org.openedx.whatsnew.WhatsNewManager
import org.openedx.whatsnew.WhatsNewRouter
import org.openedx.whatsnew.data.storage.WhatsNewPreferences
import org.openedx.whatsnew.domain.model.WhatsNewItem
import org.openedx.whatsnew.presentation.WhatsNewAnalyticKey
import org.openedx.whatsnew.presentation.WhatsNewAnalytics
import org.openedx.whatsnew.presentation.WhatsNewAnalyticsEvent

class WhatsNewViewModel(
    val courseId: String?,
    val infoType: String?,
    private val whatsNewManager: WhatsNewManager,
    private val analytics: WhatsNewAnalytics,
    private val router: WhatsNewRouter,
    private val preferencesManager: WhatsNewPreferences,
    private val appData: AppData,
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

    fun navigateToMain(fm: FragmentManager) {
        val versionName = appData.versionName
        preferencesManager.lastWhatsNewVersion = versionName
        router.navigateToMain(
            fm,
            courseId,
            infoType
        )
    }

    fun logWhatsNewViewed() {
        logEvent(WhatsNewAnalyticsEvent.WHATS_NEW_VIEW)
    }

    fun logWhatsNewDismissed(currentlyViewed: Int) {
        logEvent(WhatsNewAnalyticsEvent.WHATS_NEW_CLOSE, currentlyViewed)
    }

    fun logWhatsNewCompleted() {
        logEvent(WhatsNewAnalyticsEvent.WHATS_NEW_DONE)
    }

    private fun logEvent(event: WhatsNewAnalyticsEvent, currentlyViewed: Int? = null) {
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(WhatsNewAnalyticKey.NAME.key, event.biValue)
                put(WhatsNewAnalyticKey.CATEGORY.key, WhatsNewAnalyticKey.WHATS_NEW.key)
                put(WhatsNewAnalyticKey.TOTAL_SCREENS.key, whatsNewItem.value?.messages?.size)
                currentlyViewed?.let {
                    put(WhatsNewAnalyticKey.CURRENTLY_VIEWED.key, it)
                }
            }
        )
    }
}
