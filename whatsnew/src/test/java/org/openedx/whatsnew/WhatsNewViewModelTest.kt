package org.openedx.whatsnew

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.openedx.core.presentation.global.AppData
import org.openedx.whatsnew.data.storage.WhatsNewPreferences
import org.openedx.whatsnew.domain.model.WhatsNewItem
import org.openedx.whatsnew.presentation.WhatsNewAnalytics
import org.openedx.whatsnew.presentation.whatsnew.WhatsNewViewModel

class WhatsNewViewModelTest {

    private val whatsNewManager = mockk<WhatsNewManager>()
    private val analytics = mockk<WhatsNewAnalytics>()
    private val router = mockk<WhatsNewRouter>()
    private val preferencesManager = mockk<WhatsNewPreferences>()
    private val appData = mockk<AppData>()

    private val whatsNewItem = WhatsNewItem(
        version = "1.0.0",
        messages = emptyList()
    )

    @Test
    fun `getNewestData success`() = runTest {
        every { whatsNewManager.getNewestData() } returns whatsNewItem

        val viewModel = WhatsNewViewModel(
            "",
            "",
            whatsNewManager,
            analytics,
            router,
            preferencesManager,
            appData
        )

        verify(exactly = 1) { whatsNewManager.getNewestData() }
        assert(viewModel.whatsNewItem.value == whatsNewItem)
    }
}
