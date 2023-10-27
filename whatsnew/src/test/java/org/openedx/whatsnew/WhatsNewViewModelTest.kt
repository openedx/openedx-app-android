package org.openedx.whatsnew

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.openedx.whatsnew.domain.model.WhatsNewItem
import org.openedx.whatsnew.presentation.whatsnew.WhatsNewViewModel

class WhatsNewViewModelTest {

    private val whatsNewFileManager = mockk<WhatsNewFileManager>()

    private val whatsNewItem = WhatsNewItem(
        version = "1.0.0",
        messages = emptyList()
    )

    @Test
    fun `getNewestData success`() = runTest {
        every { whatsNewFileManager.getNewestData() } returns whatsNewItem

        val viewModel = WhatsNewViewModel(
            whatsNewFileManager
        )

        verify(exactly = 1) { whatsNewFileManager.getNewestData() }
        assert(viewModel.whatsNewItem.value == whatsNewItem)
    }
}