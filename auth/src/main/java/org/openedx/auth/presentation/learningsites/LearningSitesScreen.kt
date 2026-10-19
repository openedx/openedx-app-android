package org.openedx.auth.presentation.learningsites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import org.openedx.auth.R
import org.openedx.core.instance.Instance
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.InstanceHeaderImage
import org.openedx.core.ui.LearningSiteRow
import org.openedx.core.ui.SearchBar
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.theme.compose.SignInLogoView
import org.openedx.foundation.presentation.WindowSize
import org.openedx.foundation.presentation.WindowType
import org.openedx.foundation.presentation.windowSizeValue
import org.openedx.core.R as coreR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun LearningSitesScreen(
    windowSize: WindowSize,
    state: LearningSitesUIState,
    isSwitching: Boolean,
    onBackClick: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSiteClick: (Instance) -> Unit,
    onLogOutClick: (Instance) -> Unit,
) {
    var siteToLogOut by remember { mutableStateOf<Instance?>(null) }
    val contentWidth by remember(windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                compact = Modifier.fillMaxWidth(),
            )
        )
    }

    Scaffold(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.appColors.background,
        contentWindowInsets = WindowInsets(),
    ) { paddings ->
        InstanceHeaderImage(
            fallback = painterResource(id = coreR.drawable.core_top_header),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = 0.3f),
        )
        if (isSwitching) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                BackBtn(modifier = Modifier.padding(end = 16.dp), tint = Color.White, onBackClick = onBackClick)
            }
        }
        Column(
            modifier = Modifier.padding(paddings),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SignInLogoView()
            Surface(
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.screenBackgroundShape,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier
                            .then(contentWidth)
                            .displayCutoutForLandscape()
                            .testTag("lc_learning_sites"),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
                    ) {
                        item { Heading(isSwitching) }
                        state.currentSite?.let { current ->
                            siteSection(
                                titleRes = R.string.auth_learning_sites_current_site,
                                sites = listOf(current),
                                onSiteClick = onSiteClick,
                                onLogOutClick = { siteToLogOut = it },
                            )
                        }
                        if (state.otherSignedInSites.isNotEmpty()) {
                            siteSection(
                                titleRes = R.string.auth_learning_sites_more_sites,
                                sites = state.otherSignedInSites,
                                onSiteClick = onSiteClick,
                                onLogOutClick = { siteToLogOut = it },
                            )
                        }
                        addSiteSection(
                            state = state,
                            onSearchQueryChanged = onSearchQueryChanged,
                            onSiteClick = onSiteClick,
                        )
                    }
                }
            }
        }
    }

    siteToLogOut?.let { site ->
        LogOutDialog(
            site = site,
            onDismiss = { siteToLogOut = null },
            onConfirm = {
                siteToLogOut = null
                onLogOutClick(site)
            },
        )
    }
}

@Composable
private fun Heading(isSwitching: Boolean) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            modifier = Modifier.testTag("txt_learning_sites_title"),
            text = stringResource(
                if (isSwitching) R.string.auth_learning_sites_title else R.string.auth_learning_sites_get_started
            ),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.displaySmall,
        )
        Text(
            modifier = Modifier
                .testTag("txt_learning_sites_subtitle")
                .padding(top = 4.dp),
            text = stringResource(
                if (isSwitching) {
                    R.string.auth_learning_sites_subtitle
                } else {
                    R.string.auth_learning_sites_welcome_subtitle
                }
            ),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleSmall,
        )
    }
}

@Composable
private fun SectionTitle(titleRes: Int) {
    Text(
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
        text = stringResource(titleRes),
        color = MaterialTheme.appColors.textPrimary,
        style = MaterialTheme.appTypography.titleMedium,
    )
}

private fun LazyListScope.siteSection(
    titleRes: Int,
    sites: List<Instance>,
    onSiteClick: (Instance) -> Unit,
    onLogOutClick: (Instance) -> Unit,
) {
    item(key = "title_$titleRes") { SectionTitle(titleRes) }
    items(sites, key = { "signed_in_${it.key}" }) { site ->
        SiteItem(site = site, onClick = { onSiteClick(site) }) {
            IconButton(
                modifier = Modifier.testTag("ib_log_out_site"),
                onClick = { onLogOutClick(site) },
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = stringResource(
                        R.string.auth_learning_sites_accessibility_log_out,
                        site.instanceName
                    ),
                    tint = MaterialTheme.appColors.textSecondary,
                )
            }
        }
    }
}

private fun LazyListScope.addSiteSection(
    state: LearningSitesUIState,
    onSearchQueryChanged: (String) -> Unit,
    onSiteClick: (Instance) -> Unit,
) {
    item(key = "title_add") { SectionTitle(R.string.auth_learning_sites_add_site) }
    item(key = "search") {
        SearchBar(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            searchValue = TextFieldValue(state.searchQuery),
            label = stringResource(R.string.auth_learning_sites_search_placeholder),
            keyboardActions = {},
            onValueChanged = { onSearchQueryChanged(it.text) },
            onClearValue = { onSearchQueryChanged("") },
        )
    }
    if (state.addableSites.isEmpty() && state.searchQuery.isNotBlank()) {
        item(key = "empty") {
            Text(
                modifier = Modifier
                    .testTag("txt_learning_sites_empty")
                    .padding(vertical = 12.dp),
                text = stringResource(R.string.auth_learning_sites_empty_search),
                color = MaterialTheme.appColors.textSecondary,
                style = MaterialTheme.appTypography.bodySmall,
            )
        }
    }
    items(state.addableSites, key = { "addable_${it.key}" }) { site ->
        SiteItem(site = site, onClick = { onSiteClick(site) })
    }
}

@Composable
private fun SiteItem(
    site: Instance,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Column {
        LearningSiteRow(
            instance = site,
            modifier = Modifier
                .testTag("btn_select_site")
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 4.dp),
            trailing = { trailing() },
        )
        HorizontalDivider(color = MaterialTheme.appColors.divider)
    }
}

@Composable
private fun LogOutDialog(
    site: Instance,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.appColors.background,
        shape = MaterialTheme.appShapes.cardShape,
        title = {
            Text(
                text = stringResource(R.string.auth_learning_sites_log_out_title),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleLarge,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.auth_learning_sites_log_out_message, site.instanceName),
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(modifier = Modifier.testTag("btn_confirm_log_out"), onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.auth_learning_sites_log_out_action),
                    color = MaterialTheme.appColors.error,
                    style = MaterialTheme.appTypography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(coreR.string.core_cancel),
                    color = MaterialTheme.appColors.primary,
                    style = MaterialTheme.appTypography.labelLarge,
                )
            }
        },
    )
}

private fun previewSite(key: String, name: String) = requireNotNull(
    Instance.fromJson(
        JsonObject().apply {
            addProperty("KEY", key)
            addProperty("NAME", name)
            addProperty("OAUTH_CLIENT_ID", "client")
            addProperty("API_HOST_URL", "https://lms.$key.example.com")
        }
    )
)

@Preview(name = "Switching", widthDp = 375, heightDp = 812)
@Preview(name = "Switching dark", widthDp = 375, heightDp = 812, uiMode = 0x20)
@Composable
private fun LearningSitesScreenPreview() {
    OpenEdXTheme {
        LearningSitesScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            state = LearningSitesUIState(
                currentSite = previewSite("north", "North Valley University"),
                otherSignedInSites = listOf(previewSite("arts", "Institute of Arts")),
                addableSites = listOf(previewSite("coast", "Coastal College"), previewSite("tech", "Tech Academy")),
            ),
            isSwitching = true,
            onBackClick = {},
            onSearchQueryChanged = {},
            onSiteClick = {},
            onLogOutClick = {},
        )
    }
}

@Preview(name = "First launch", widthDp = 375, heightDp = 812)
@Composable
private fun LearningSitesFirstLaunchPreview() {
    var query by rememberSaveable { mutableStateOf("") }
    OpenEdXTheme {
        LearningSitesScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            state = LearningSitesUIState(
                addableSites = listOf(previewSite("coast", "Coastal College"), previewSite("tech", "Tech Academy")),
                searchQuery = query,
            ),
            isSwitching = false,
            onBackClick = {},
            onSearchQueryChanged = { query = it },
            onSiteClick = {},
            onLogOutClick = {},
        )
    }
}
