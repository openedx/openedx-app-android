package org.openedx.auth.presentation.lmsselection

import org.openedx.core.lmsdirectory.LmsSummary

/** UI callbacks for [SiteSelectionScreen]. */
class SiteSelectionCallbacks(
    val onBack: () -> Unit,
    val onInputChanged: (String) -> Unit,
    val onSubmitManual: () -> Unit,
    val onQueryChanged: (String) -> Unit,
    val onCatalogItemSelected: (LmsSummary) -> Unit,
)
