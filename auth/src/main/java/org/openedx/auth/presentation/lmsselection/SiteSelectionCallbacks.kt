package org.openedx.auth.presentation.lmsselection

import org.openedx.core.lmsdirectory.LmsHistoryEntry
import org.openedx.core.lmsdirectory.LmsSummary

/** UI callbacks for [SiteSelectionScreen]. */
class SiteSelectionCallbacks(
    val onBack: () -> Unit,
    val onQrClick: () -> Unit,
    val onSubmitManual: () -> Unit,
    val onQueryChanged: (String) -> Unit,
    val onCatalogItemSelected: (LmsSummary) -> Unit,
    val onCleanHistory: () -> Unit,
    val onHistoryItemSelected: (LmsHistoryEntry) -> Unit,
)
