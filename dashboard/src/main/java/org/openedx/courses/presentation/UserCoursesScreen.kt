package org.openedx.courses.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.dashboard.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UsersCourseScreen(
    viewModel: UserCoursesViewModel,
    onItemClick: (EnrolledCourse) -> Unit,
) {
    val updating by viewModel.updating.observeAsState(false)
    val uiMessage by viewModel.uiMessage.collectAsState(null)
    val uiState by viewModel.uiState.observeAsState(UserCoursesUIState.Loading)
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState = rememberPullRefreshState(refreshing = updating, onRefresh = { viewModel.updateCoursed() })
    val scrollState = rememberLazyListState()

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->
        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Surface(
            modifier = Modifier.padding(paddingValues),
            color = MaterialTheme.appColors.background
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState),
            ) {
                when (uiState) {
                    is UserCoursesUIState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.appColors.primary
                        )
                    }

                    is UserCoursesUIState.Courses -> {

                    }

                    is UserCoursesUIState.Empty -> {
                        EmptyState()
                    }
                }

                PullRefreshIndicator(
                    updating,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(185.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.dashboard_ic_empty),
                contentDescription = null,
                tint = MaterialTheme.appColors.textFieldBorder
            )
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_description")
                    .fillMaxWidth(),
                text = stringResource(id = R.string.dashboard_you_are_not_enrolled),
                color = MaterialTheme.appColors.textPrimaryVariant,
                style = MaterialTheme.appTypography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}