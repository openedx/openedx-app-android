package org.openedx.auth.presentation.lmsselection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.openedx.auth.R
import org.openedx.core.lmsdirectory.LmsSummary
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography

@Composable
internal fun SiteSelectionScreen(
    state: SiteSelectionUIState,
    callbacks: SiteSelectionCallbacks,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.appColors.background,
        topBar = {
            Surface(color = MaterialTheme.appColors.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { callbacks.onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                id = org.openedx.core.R.string.core_accessibility_btn_back
                            ),
                            tint = MaterialTheme.appColors.textPrimary,
                        )
                    }
                    Text(
                        text = if (state.isCurated && state.providerName.isNotBlank()) {
                            state.providerName
                        } else {
                            stringResource(id = R.string.auth_lms_choose_title)
                        },
                        style = MaterialTheme.appTypography.titleLarge,
                        color = MaterialTheme.appColors.textPrimary,
                        modifier = Modifier.padding(start = 8.dp)
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (state.isCurated) {
                Text(
                    text = stringResource(id = R.string.auth_lms_catalog_available),
                    style = MaterialTheme.appTypography.titleMedium,
                    color = MaterialTheme.appColors.textPrimary,
                )
                CatalogContent(state, callbacks)
            } else {
                SearchSection(state, callbacks)
            }
            ManualEntrySection(state, callbacks)
        }
    }
}

@Composable
private fun SearchSection(state: SiteSelectionUIState, callbacks: SiteSelectionCallbacks) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    tint = MaterialTheme.appColors.textSecondary,
                )
            },
            textStyle = MaterialTheme.appTypography.bodyLarge,
            shape = MaterialTheme.appShapes.textFieldShape,
            colors = directoryTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, keyboardType = KeyboardType.Uri),
        )
        CatalogContent(state, callbacks)
    }
}

@Composable
private fun CatalogContent(state: SiteSelectionUIState, callbacks: SiteSelectionCallbacks) {
    when (val catalog = state.catalog) {
        is CatalogState.Idle -> PlaceholderText(stringResource(id = R.string.auth_lms_start_typing))
        is CatalogState.Loading -> Row(
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
        is CatalogState.Empty -> PlaceholderText(stringResource(id = R.string.auth_lms_no_results))
        is CatalogState.Error -> Text(
            text = catalog.message,
            style = MaterialTheme.appTypography.bodyMedium,
            color = MaterialTheme.appColors.error,
        )
        is CatalogState.Loaded -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.results.forEach { item ->
                CatalogRow(item = item, onSelect = { callbacks.onCatalogItemSelected(item) })
            }
        }
    }
}

@Composable
private fun CatalogRow(item: LmsSummary, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = MaterialTheme.appShapes.textFieldShape,
        color = MaterialTheme.appColors.background,
        border = BorderStroke(1.dp, MaterialTheme.appColors.textFieldBorder.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    maxLines = 1,
                    style = MaterialTheme.appTypography.titleSmall,
                    color = MaterialTheme.appColors.textPrimary,
                )
                if (item.shortDescription.isNotBlank()) {
                    Text(
                        text = item.shortDescription,
                        maxLines = 1,
                        style = MaterialTheme.appTypography.bodyMedium,
                        color = MaterialTheme.appColors.textSecondary,
                    )
                }
                Text(
                    text = hostOf(item.baseUrl),
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

@Composable
private fun ManualEntrySection(state: SiteSelectionUIState, callbacks: SiteSelectionCallbacks) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(id = R.string.auth_lms_manual_entry_title),
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textPrimary,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.inputUrl,
            onValueChange = { callbacks.onInputChanged(it) },
            enabled = !state.isLoading,
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.auth_lms_url_placeholder),
                    style = MaterialTheme.appTypography.bodyLarge,
                    color = MaterialTheme.appColors.textFieldHint,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textSecondary,
                )
            },
            textStyle = MaterialTheme.appTypography.bodyLarge,
            shape = MaterialTheme.appShapes.textFieldShape,
            colors = directoryTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Uri),
            keyboardActions = KeyboardActions {
                keyboardController?.hide()
                focusManager.clearFocus()
                callbacks.onSubmitManual()
            }
        )
        if (!state.errorMessage.isNullOrEmpty()) {
            Text(
                text = state.errorMessage,
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.error,
            )
        }
        OpenEdXButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                callbacks.onSubmitManual()
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = org.openedx.core.R.string.core_continue),
                    color = MaterialTheme.appColors.primaryButtonText,
                    style = MaterialTheme.appTypography.labelLarge
                )
                if (state.isLoading) {
                    Spacer(modifier = Modifier.size(12.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.appColors.primaryButtonText,
                    )
                }
            }
        }
    }
}

@Composable
private fun directoryTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.appColors.textFieldText,
    unfocusedTextColor = MaterialTheme.appColors.textFieldText,
    focusedContainerColor = MaterialTheme.appColors.textFieldBackground,
    unfocusedContainerColor = MaterialTheme.appColors.textFieldBackground,
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
