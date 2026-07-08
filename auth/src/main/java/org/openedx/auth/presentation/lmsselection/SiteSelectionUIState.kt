package org.openedx.auth.presentation.lmsselection

import org.openedx.core.lmsdirectory.LmsHistoryEntry
import org.openedx.core.lmsdirectory.LmsSummary

data class SiteSelectionUIState(
    val inputUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Registry catalog (search / curated browse)
    val isCurated: Boolean = false,
    val providerName: String = "",
    val query: String = "",
    val catalog: CatalogState = CatalogState.Idle,
    val results: List<LmsSummary> = emptyList(),

    // Recently selected platforms, shown when the search field is empty.
    val history: List<LmsHistoryEntry> = emptyList(),
)

sealed interface CatalogState {
    data object Idle : CatalogState
    data object Loading : CatalogState
    data object Loaded : CatalogState
    data object Empty : CatalogState
    data class Error(val message: String) : CatalogState
}
