package org.openedx.auth.presentation.lmsselection

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.openedx.auth.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.lmsdirectory.LmsDirectoryRepository
import org.openedx.core.lmsdirectory.LmsSummary
import org.openedx.core.lmsdirectory.LmsThemeController
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.system.ResourceManager
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Drives the LMS Directory selection screen: search or browse the registry catalog,
 * or enter an LMS URL by hand. Picking a platform persists it as the app's host,
 * re-themes the app to its brand color, then signals the fragment to continue to
 * sign-in.
 */
class SiteSelectionViewModel(
    private val corePreferences: CorePreferences,
    private val resourceManager: ResourceManager,
    private val config: Config,
    private val directoryRepository: LmsDirectoryRepository,
) : BaseViewModel(resourceManager) {

    private val validationClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(
        SiteSelectionUIState(inputUrl = corePreferences.selectedBaseUrl?.trimEnd('/') ?: "")
    )
    val uiState: StateFlow<SiteSelectionUIState> = _uiState

    private val _actions = MutableSharedFlow<SiteSelectionAction>()
    val actions: SharedFlow<SiteSelectionAction> = _actions.asSharedFlow()

    private var searchJob: Job? = null

    init {
        loadCatalogConfig()
    }

    // region Catalog

    private fun loadCatalogConfig() {
        viewModelScope.launch {
            val remote = directoryRepository.fetchConfig()
            val configuredMode = config.getLMSDirectoryConfig().directoryMode
            val curated = remote.isCurated || configuredMode.equals("curated", ignoreCase = true)
            _uiState.update {
                it.copy(isCurated = curated, providerName = remote.providerName)
            }
            if (curated) {
                loadFeatured()
            }
        }
    }

    private fun loadFeatured() {
        _uiState.update { it.copy(catalog = CatalogState.Loading) }
        viewModelScope.launch {
            directoryRepository.fetchFeatured()
                .onSuccess { items -> applyResults(items) }
                .onFailure { showCatalogError() }
        }
    }

    private fun showCatalogError() {
        val message = resourceManager.getString(R.string.auth_lms_error_catalog_failed)
        _uiState.update { it.copy(catalog = CatalogState.Error(message)) }
    }

    fun onQueryChanged(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(catalog = CatalogState.Idle, results = emptyList()) }
            return
        }
        _uiState.update { it.copy(catalog = CatalogState.Loading) }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            directoryRepository.search(trimmed)
                .onSuccess { items -> applyResults(items) }
                .onFailure { showCatalogError() }
        }
    }

    private fun applyResults(items: List<LmsSummary>) {
        _uiState.update {
            it.copy(
                results = items,
                catalog = if (items.isEmpty()) CatalogState.Empty else CatalogState.Loaded,
            )
        }
    }

    fun onCatalogItemSelected(item: LmsSummary) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            // Fetch the full record: the catalog summary has no OAuth client id, and
            // sign-in needs the platform's own registered mobile client to work.
            val detail = directoryRepository.fetchDetail(item.id).getOrNull()
            val normalized = normalizeUrl(detail?.baseUrl ?: item.baseUrl)
            if (normalized == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = resourceManager.getString(R.string.auth_lms_error_invalid_url),
                    )
                }
                return@launch
            }
            selectLms(
                baseUrl = normalized.newBuilder().encodedPath("/").build().toString(),
                accentColor = detail?.accentColor ?: item.accentColor,
                oauthClientId = detail?.oauthClientId,
                feedbackEmail = detail?.feedbackEmail,
                logoUrl = detail?.logoUrl ?: item.logoUrl,
                title = detail?.title ?: item.title,
            )
            _actions.emit(SiteSelectionAction.Success)
        }
    }

    /** Select an LMS by URL scanned from a QR code. Validates before committing. */
    fun onUrlScanned(rawUrl: String) {
        val normalized = normalizeUrl(rawUrl) ?: run {
            _uiState.update {
                it.copy(errorMessage = resourceManager.getString(R.string.auth_lms_error_invalid_url))
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val confirmed = withContext(Dispatchers.IO) { confirmBaseUrl(normalized) }
            if (confirmed) {
                selectLms(normalized.newBuilder().encodedPath("/").build().toString(), accentColor = null)
                _actions.emit(SiteSelectionAction.Success)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = resourceManager.getString(R.string.auth_lms_error_unable_confirm_url),
                    )
                }
            }
        }
    }

    // endregion

    // region Manual URL entry (fallback when the registry is unreachable)

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(inputUrl = value, errorMessage = null) }
    }

    fun onSubmitManual() {
        val normalized = normalizeUrl(_uiState.value.inputUrl)
        if (normalized == null) {
            _uiState.update {
                it.copy(errorMessage = resourceManager.getString(R.string.auth_lms_error_invalid_url))
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val confirmed = withContext(Dispatchers.IO) { confirmBaseUrl(normalized) }
            if (confirmed) {
                selectLms(normalized.newBuilder().encodedPath("/").build().toString(), accentColor = null)
                _actions.emit(SiteSelectionAction.Success)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = resourceManager.getString(R.string.auth_lms_error_unable_confirm_url),
                    )
                }
            }
        }
    }

    /**
     * Commit the chosen platform: persist host, OAuth client, feedback, branding, then
     * re-theme immediately. Manual/QR entry passes only the URL, clearing the per-LMS
     * OAuth override so sign-in falls back to the config client for unknown hosts.
     */
    private fun selectLms(
        baseUrl: String,
        accentColor: String?,
        oauthClientId: String? = null,
        feedbackEmail: String? = null,
        logoUrl: String? = null,
        title: String? = null,
    ) {
        corePreferences.selectedBaseUrl = baseUrl
        corePreferences.selectedLmsAccentColor = accentColor
        corePreferences.selectedOAuthClientId = oauthClientId
        corePreferences.selectedFeedbackEmail = feedbackEmail
        corePreferences.selectedLmsLogoUrl = logoUrl
        corePreferences.selectedLmsTitle = title
        LmsThemeController.apply(accentColor)
    }

    private fun normalizeUrl(text: String): HttpUrl? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val candidate = withScheme.toHttpUrlOrNull()
        return when {
            candidate == null || candidate.host.isEmpty() -> null
            else -> candidate.newBuilder()
                .encodedPath("/")
                .query(null)
                .fragment(null)
                .build()
        }
    }

    @Suppress("MagicNumber")
    private fun confirmBaseUrl(base: HttpUrl): Boolean {
        return try {
            val registrationRequest = Request.Builder()
                .get()
                .url(base.newBuilder().addPathSegments(REGISTRATION_PATH).build())
                .build()
            val registrationSuccessful = validationClient.newCall(registrationRequest).execute()
                .use { response -> response.isSuccessful }

            if (registrationSuccessful) {
                true
            } else {
                val oauthRequest = Request.Builder()
                    .get()
                    .url(base.newBuilder().addPathSegments(OAUTH_PATH).build())
                    .build()
                validationClient.newCall(oauthRequest).execute()
                    .use { response -> response.code in 200..399 }
            }
        } catch (_: IOException) {
            false
        }
    }

    // endregion

    sealed interface SiteSelectionAction {
        data object Success : SiteSelectionAction
    }

    private companion object {
        const val REGISTRATION_PATH = "user_api/v1/account/registration/"
        const val OAUTH_PATH = "oauth2/login/"
        const val SEARCH_DEBOUNCE_MS = 400L
        const val VALIDATION_TIMEOUT_SECONDS = 20L
    }
}
