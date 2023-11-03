package org.openedx.whatsnew.presentation.whatsnew

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.openedx.core.BaseViewModel
import org.openedx.whatsnew.WhatsNewManagerManager
import org.openedx.whatsnew.domain.model.WhatsNewItem

class WhatsNewViewModel(
    private val whatsNewManager: WhatsNewManagerManager
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
}