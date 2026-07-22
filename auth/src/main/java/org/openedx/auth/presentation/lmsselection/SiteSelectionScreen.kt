package org.openedx.auth.presentation.lmsselection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.auth.R
import org.openedx.core.lmsdirectory.LmsHistoryEntry
import org.openedx.core.lmsdirectory.LmsSummary
import org.openedx.core.lmsdirectory.LmsThemeController
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

@Composable
internal fun SiteSelectionScreen(
    state: SiteSelectionUIState,
    callbacks: SiteSelectionCallbacks,
    // Hidden when this screen IS the entry point (curated mode landing) — there is
    // nothing to go back to. Shown when pushed from the "Find my LMS" landing button.
    showBack: Boolean = true,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.appColors.background,
        topBar = {
            Surface(color = MaterialTheme.appColors.background) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showBack) {
                        BackBtn(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp),
                            tint = MaterialTheme.appColors.textPrimary,
                        ) { callbacks.onBack() }
                    }
                    Text(
                        text = stringResource(
                            id = if (state.isCurated) {
                                R.string.auth_lms_curated_title
                            } else {
                                R.string.auth_lms_choose_title
                            }
                        ),
                        style = MaterialTheme.appTypography.titleMedium,
                        color = MaterialTheme.appColors.textPrimary,
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.isCurated) {
                SearchField(state, callbacks)
            }
            CatalogContent(state, callbacks)
        }
    }
}

@Composable
private fun SearchField(state: SiteSelectionUIState, callbacks: SiteSelectionCallbacks) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = state.query,
        onValueChange = { callbacks.onQueryChanged(it) },
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(id = R.string.auth_lms_search_hint),
                style = MaterialTheme.appTypography.bodyLarge,
                color = MaterialTheme.appColors.textFieldHint,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.appColors.textFieldText,
            )
        },
        trailingIcon = {
            IconButton(onClick = { callbacks.onQrClick() }) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = stringResource(id = R.string.auth_lms_qr_button),
                    tint = MaterialTheme.appColors.primary,
                )
            }
        },
        textStyle = MaterialTheme.appTypography.bodyLarge,
        shape = MaterialTheme.appShapes.textFieldShape,
        colors = directoryTextFieldColors(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, keyboardType = KeyboardType.Uri),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                focusManager.clearFocus()
                callbacks.onSubmitManual()
            }
        ),
    )
}

@Composable
private fun CatalogContent(state: SiteSelectionUIState, callbacks: SiteSelectionCallbacks) {
    when (val catalog = state.catalog) {
        // Curated mode has no search/history — the org's fixed list loads straight away,
        // so an idle state just means "still loading the list".
        is CatalogState.Idle -> when {
            state.isCurated -> LoadingRow()
            state.history.isNotEmpty() -> HistorySection(history = state.history, callbacks = callbacks)
            else -> PlaceholderText(stringResource(id = R.string.auth_lms_start_typing))
        }
        is CatalogState.Loading -> LoadingRow()
        is CatalogState.Empty -> PlaceholderText(
            stringResource(
                id = if (state.isCurated) R.string.auth_lms_curated_empty else R.string.auth_lms_no_results
            )
        )
        is CatalogState.Error -> Text(
            text = catalog.message,
            style = MaterialTheme.appTypography.bodyMedium,
            color = MaterialTheme.appColors.error,
        )
        is CatalogState.Loaded -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // "Results" is a search concept — in curated mode the list is just the platforms.
            if (!state.isCurated) {
                SectionHeader(text = stringResource(id = R.string.auth_lms_results))
            }
            state.results.forEach { item ->
                CatalogRow(item = item, onSelect = { callbacks.onCatalogItemSelected(item) })
            }
        }
    }
}

/**
 * "History" section shown when the search field is empty: a header with a
 * "Clean history" action, then the recently selected platforms rendered with the
 * same row visual as search results. Mirrors iOS `historySection`.
 */
@Composable
private fun HistorySection(history: List<LmsHistoryEntry>, callbacks: SiteSelectionCallbacks) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(text = stringResource(id = R.string.auth_lms_history))
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = R.string.auth_lms_clean_history),
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.primary,
                modifier = Modifier.clickable { callbacks.onCleanHistory() },
            )
        }
        history.forEach { entry ->
            CatalogRow(entry = entry, onSelect = { callbacks.onHistoryItemSelected(entry) })
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.appColors.primary,
        )
        PlaceholderText(stringResource(id = R.string.auth_lms_searching))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.appTypography.labelLarge,
        color = MaterialTheme.appColors.textSecondary,
    )
}

@Composable
private fun CatalogRow(item: LmsSummary, onSelect: () -> Unit) {
    CatalogRow(
        title = item.title,
        shortDescription = item.shortDescription,
        baseUrl = item.baseUrl,
        logoUrl = item.logoUrl,
        accentColor = item.accentColor,
        onSelect = onSelect,
    )
}

@Composable
private fun CatalogRow(entry: LmsHistoryEntry, onSelect: () -> Unit) {
    CatalogRow(
        title = entry.title,
        shortDescription = entry.shortDescription,
        baseUrl = entry.baseUrl,
        logoUrl = entry.logoUrl,
        accentColor = entry.accentColor,
        onSelect = onSelect,
    )
}

@Composable
private fun CatalogRow(
    title: String,
    shortDescription: String,
    baseUrl: String,
    logoUrl: String?,
    accentColor: String?,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = MaterialTheme.appShapes.textFieldShape,
        color = MaterialTheme.appColors.background,
        border = BorderStroke(1.dp, MaterialTheme.appColors.textFieldBorder.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LmsRowLogo(logoUrl = logoUrl, title = title, accentColor = accentColor)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.appTypography.titleSmall,
                    color = MaterialTheme.appColors.textPrimary,
                )
                if (shortDescription.isNotBlank()) {
                    Text(
                        text = shortDescription,
                        maxLines = 1,
                        style = MaterialTheme.appTypography.bodyMedium,
                        color = MaterialTheme.appColors.textSecondary,
                    )
                }
                Text(
                    text = hostOf(baseUrl),
                    maxLines = 1,
                    style = MaterialTheme.appTypography.labelMedium,
                    color = MaterialTheme.appColors.textSecondary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.appColors.primary,
            )
        }
    }
}

/**
 * Platform logo for a catalog row. Loads the LMS's logo when available; otherwise
 * falls back to a colored initial badge tinted with the platform's accent color —
 * mirroring the iOS directory rows.
 */
@Composable
private fun LmsRowLogo(logoUrl: String?, title: String, accentColor: String?) {
    val logoModifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(10.dp))
    if (!logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = logoModifier,
        )
    } else {
        val accent = LmsThemeController.parseHexColor(accentColor) ?: MaterialTheme.appColors.primary
        Box(
            modifier = logoModifier.background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.trim().take(1).uppercase(),
                style = MaterialTheme.appTypography.titleMedium,
                color = accent,
            )
        }
    }
}

@Composable
private fun directoryTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.appColors.textFieldText,
    unfocusedTextColor = MaterialTheme.appColors.textFieldText,
    focusedContainerColor = MaterialTheme.appColors.background,
    unfocusedContainerColor = MaterialTheme.appColors.background,
    focusedBorderColor = MaterialTheme.appColors.textFieldBorder,
    unfocusedBorderColor = MaterialTheme.appColors.textFieldBorder,
    cursorColor = MaterialTheme.appColors.primary,
)

@Composable
private fun PlaceholderText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.appTypography.bodyLarge,
        color = MaterialTheme.appColors.textSecondary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

private fun hostOf(url: String): String {
    return url.removePrefix("https://").removePrefix("http://").trimEnd('/')
}
