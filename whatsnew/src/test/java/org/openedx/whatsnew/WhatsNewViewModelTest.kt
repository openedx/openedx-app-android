package org.openedx.whatsnew

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.openedx.whatsnew.domain.model.WhatsNewItem
import org.openedx.whatsnew.presentation.whatsnew.WhatsNewViewModel

class WhatsNewViewModelTest {

    private val whatsNewManager = mockk<WhatsNewManagerManager>()

    private val whatsNewItem = WhatsNewItem(
        version = "1.0.0",
        messages = emptyList()
    )

    @Test
    fun `getNewestData success`() = runTest {
        every { whatsNewManager.getNewestData() } returns whatsNewItem

        val viewModel = WhatsNewViewModel(
            whatsNewManager
        )

        verify(exactly = 1) { whatsNewManager.getNewestData() }
        assert(viewModel.whatsNewItem.value == whatsNewItem)
    }
}