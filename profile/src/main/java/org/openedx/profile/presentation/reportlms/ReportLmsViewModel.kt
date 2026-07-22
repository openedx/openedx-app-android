package org.openedx.profile.presentation.reportlms

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.config.Config
import org.openedx.core.lmsdirectory.LmsDirectoryRepository
import org.openedx.core.lmsdirectory.ReportCategory
import org.openedx.core.lmsdirectory.ReportDraft
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.system.ResourceManager
import org.openedx.profile.R

/**
 * Reports the currently signed-in LMS from the Profile tab. Reuses the registry
 * submission pipeline ([LmsDirectoryRepository.submitReport]); the target is always
 * the current platform ([Config.getApiHostURL]), so there is no lmsId.
 */
class ReportLmsViewModel(
    private val config: Config,
    private val directoryRepository: LmsDirectoryRepository,
    private val resourceManager: ResourceManager,
) : BaseViewModel(resourceManager) {

    private val _uiState = MutableStateFlow(ReportLmsUiState())
    val uiState: StateFlow<ReportLmsUiState> = _uiState

    /**
     * Host of the LMS being reported (the current platform), for the sheet header and
     * success message — e.g. "sandbox.openedx.org" rather than the full "https://…/",
     * matching iOS's `lmsTitle`.
     */
    val displayHost: String
        get() {
            val url = config.getApiHostURL()
            return android.net.Uri.parse(url).host ?: url
        }

    fun onCategoryChanged(category: ReportCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onMessageChanged(value: String) {
        _uiState.update { it.copy(message = value) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onScreenshotPicked(base64: String, preview: ImageBitmap) {
        _uiState.update { it.copy(screenshotBase64 = base64, screenshotPreview = preview) }
    }

    fun onScreenshotRemoved() {
        _uiState.update { it.copy(screenshotBase64 = null, screenshotPreview = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            directoryRepository.submitReport(
                ReportDraft(
                    lmsId = null,
                    baseUrl = config.getApiHostURL(),
                    category = state.category,
                    message = state.message.trim(),
                    reporterEmail = state.email.ifBlank { null },
                    screenshotBase64 = state.screenshotBase64,
                )
            ).onSuccess {
                _uiState.update { it.copy(submitting = false, submitted = true) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        submitting = false,
                        error = resourceManager.getString(R.string.profile_report_lms_error),
                    )
                }
            }
        }
    }

    /** Reset back to a fresh form (called when the sheet is dismissed). */
    fun reset() {
        _uiState.value = ReportLmsUiState()
    }
}

data class ReportLmsUiState(
    val category: ReportCategory = ReportCategory.INAPPROPRIATE,
    val message: String = "",
    val email: String = "",
    val screenshotBase64: String? = null,
    val screenshotPreview: ImageBitmap? = null,
    val submitting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = !submitting && message.isNotBlank()
}
