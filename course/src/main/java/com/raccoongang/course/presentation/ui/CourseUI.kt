package com.raccoongang.course.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.raccoongang.core.BlockType
import com.raccoongang.core.BuildConfig
import com.raccoongang.core.domain.model.*
import com.raccoongang.core.extension.isLinkValid
import com.raccoongang.core.module.db.DownloadedState
import com.raccoongang.core.ui.*
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.course.R
import org.jsoup.Jsoup
import subtitleFile.TimedTextObject
import java.util.*
import com.raccoongang.course.R as courseR

@Composable
fun CourseImageHeader(
    modifier: Modifier,
    courseImage: String?,
    courseCertificate: Certificate?,
) {
    val uriHandler = LocalUriHandler.current
    val imageUrl = if (courseImage?.isLinkValid() == true) {
        courseImage
    } else {
        BuildConfig.BASE_URL.dropLast(1) + courseImage
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .error(com.raccoongang.core.R.drawable.core_no_image_course)
                .placeholder(com.raccoongang.core.R.drawable.core_no_image_course)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.appShapes.cardShape)
        )
        if (courseCertificate?.isCertificateEarned() == true) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.appShapes.cardShape)
                    .background(MaterialTheme.appColors.certificateForeground),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_course_completed_mark),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.course_congratulations),
                    style = MaterialTheme.appTypography.headlineMedium,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.course_passed),
                    style = MaterialTheme.appTypography.bodyMedium,
                    color = Color.White
                )
                Spacer(Modifier.height(20.dp))
                NewEdxOutlinedButton(
                    modifier = Modifier,
                    borderColor = Color.White,
                    textColor = MaterialTheme.appColors.buttonText,
                    text = stringResource(id = R.string.course_view_certificate),
                    onClick = {
                        courseCertificate.certificateURL?.let {
                            uriHandler.openUri(it)
                        }
                    })
            }
        }
    }
}

@Composable
fun CourseSectionCard(
    block: Block,
    downloadedState: DownloadedState?,
    onItemClick: (Block) -> Unit,
    onDownloadClick: (Block) -> Unit
) {
    val iconModifier = Modifier.size(24.dp)

    Column(Modifier.clickable { onItemClick(block) }) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(
                    horizontal = 20.dp,
                    vertical = 24.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_course_chapter_icon),
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = block.displayName,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (downloadedState == DownloadedState.DOWNLOADED || downloadedState == DownloadedState.NOT_DOWNLOADED) {
                    val iconPainter = if (downloadedState == DownloadedState.DOWNLOADED) {
                        painterResource(id = R.drawable.course_ic_remove_download)
                    } else {
                        painterResource(id = R.drawable.course_ic_start_download)
                    }
                    IconButton(modifier = iconModifier,
                        onClick = { onDownloadClick(block) }) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = MaterialTheme.appColors.textPrimary
                        )
                    }
                } else if (downloadedState != null) {
                    Box(contentAlignment = Alignment.Center) {
                        if (downloadedState == DownloadedState.DOWNLOADING || downloadedState == DownloadedState.WAITING) {
                            CircularProgressIndicator(modifier = Modifier.size(34.dp))
                        }
                        IconButton(modifier = iconModifier,
                            onClick = { onDownloadClick(block) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = MaterialTheme.appColors.error
                            )
                        }
                    }
                }
                CardArrow(
                    degrees = 0f
                )
            }
        }
    }
}

@Composable
fun CardArrow(
    degrees: Float
) {
    Icon(
        imageVector = Icons.Filled.ChevronRight,
        tint = MaterialTheme.appColors.primary,
        contentDescription = "Expandable Arrow",
        modifier = Modifier.rotate(degrees),
    )
}

@Composable
fun SequentialItem(
    block: Block,
    onClick: (Block) -> Unit
) {
    val icon = if (block.completion == 1.0) Icons.Filled.TaskAlt else Icons.Filled.Home
    val iconColor =
        if (block.completion == 1.0) MaterialTheme.appColors.primary else MaterialTheme.appColors.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 12.dp
            )
            .clickable { onClick(block) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                block.displayName,
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            tint = MaterialTheme.appColors.onSurface,
            contentDescription = "Expandable Arrow"
        )
    }
}

@Composable
fun VideoRotateView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.buttonShape)
            .background(MaterialTheme.appColors.info)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.course_ic_screen_rotation),
            contentDescription = null,
            tint = Color.White
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.course_video_rotate_for_fullscreen),
            style = MaterialTheme.appTypography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
fun VideoTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.appColors.textPrimary,
        style = MaterialTheme.appTypography.titleLarge,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun NavigationUnitsButtons(
    windowSize: WindowSize,
    prevButtonText: String?,
    nextButtonText: String,
    hasNextBlock: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val nextButtonIcon = if (hasNextBlock) {
        painterResource(id = com.raccoongang.core.R.drawable.core_ic_forward)
    } else {
        painterResource(id = com.raccoongang.core.R.drawable.core_ic_check)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.surface),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            val buttonWidth by remember(key1 = windowSize) {
                mutableStateOf(
                    windowSize.windowSizeValue(
                        expanded = Modifier.widthIn(152.dp, 162.dp),
                        compact = Modifier.weight(1f)
                    )
                )
            }

            if (prevButtonText != null) {
                OutlinedButton(
                    modifier = Modifier
                        .then(buttonWidth)
                        .height(42.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.appColors.primary,
                        backgroundColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.appColors.primary),
                    elevation = null,
                    shape = MaterialTheme.appShapes.buttonShape,
                    onClick = onPrevClick
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            painter = painterResource(id = com.raccoongang.core.R.drawable.core_ic_back),
                            contentDescription = null,
                            tint = MaterialTheme.appColors.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = prevButtonText,
                            color = MaterialTheme.appColors.primary,
                            style = MaterialTheme.appTypography.labelLarge
                        )
                    }
                }
                Spacer(Modifier.width(24.dp))
            }
            Button(
                modifier = Modifier
                    .then(buttonWidth)
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.appColors.buttonBackground
                ),
                elevation = null,
                shape = MaterialTheme.appShapes.buttonShape,
                onClick = onNextClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (prevButtonText != null) Arrangement.SpaceBetween else Arrangement.Center
                ) {
                    Text(
                        text = nextButtonText,
                        color = MaterialTheme.appColors.buttonText,
                        style = MaterialTheme.appTypography.labelLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = nextButtonIcon,
                        contentDescription = null,
                        tint = MaterialTheme.appColors.buttonText
                    )
                }

            }
        }
    }
}

@Composable
fun ConnectionErrorView(
    modifier: Modifier,
    onReloadClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(100.dp),
            imageVector = Icons.Filled.Wifi,
            contentDescription = null,
            tint = MaterialTheme.appColors.onSurface
        )
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.fillMaxWidth(0.6f),
            text = stringResource(id = R.string.course_not_connected_to_internet),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.appTypography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        NewEdxButton(
            width = Modifier
                .widthIn(Dp.Unspecified, 162.dp),
            text = stringResource(id = com.raccoongang.core.R.string.core_reload),
            onClick = onReloadClick
        )
    }
}

@Composable
fun VideoSubtitles(
    listState: LazyListState,
    timedTextObject: TimedTextObject?,
    currentIndex: Int
) {
    timedTextObject?.let {
        LaunchedEffect(key1 = currentIndex) {
            if (currentIndex > 1) {
                listState.animateScrollToItem(currentIndex - 1)
            }
        }
        val scaffoldState = rememberScaffoldState()
        val subtitles = timedTextObject.captions.values.toList()
        Scaffold(scaffoldState = scaffoldState) {
            Column(Modifier.padding(it)) {
                Text(
                    text = stringResource(id = courseR.string.course_subtitles),
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium
                )
                Spacer(Modifier.height(24.dp))
                LazyColumn(
                    state = listState,
                    userScrollEnabled = false
                ) {
                    itemsIndexed(subtitles) { index, item ->
                        val textColor =
                            if (currentIndex == index) {
                                MaterialTheme.appColors.textPrimary
                            } else {
                                MaterialTheme.appColors.textFieldBorder
                            }
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = Jsoup.parse(item.content).text(),
                            color = textColor,
                            style = MaterialTheme.appTypography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationUnitsButtonsOnlyNextButtonPreview() {
    NewEdxTheme {
        NavigationUnitsButtons(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            prevButtonText = null,
            hasNextBlock = true,
            nextButtonText = "Next",
            onPrevClick = {}) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationUnitsButtonsOnlyFinishButtonPreview() {
    NewEdxTheme {
        NavigationUnitsButtons(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            prevButtonText = null,
            hasNextBlock = false,
            nextButtonText = "Finish",
            onPrevClick = {}) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationUnitsButtonsWithFinishPreview() {
    NewEdxTheme {
        NavigationUnitsButtons(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            prevButtonText = "Previous",
            hasNextBlock = false,
            nextButtonText = "Finish",
            onPrevClick = {}) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationUnitsButtonsWithNextPreview() {
    NewEdxTheme {
        NavigationUnitsButtons(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            prevButtonText = "Previous",
            hasNextBlock = true,
            nextButtonText = "Next",
            onPrevClick = {}) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VideoRotateViewPreview() {
    NewEdxTheme {
        VideoRotateView()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SequentialItemPreview() {
    NewEdxTheme() {
        Surface(color = MaterialTheme.appColors.background) {
            SequentialItem(block = mockChapterBlock, onClick = {})
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseChapterItemPreview() {
    NewEdxTheme {
        Surface(color = MaterialTheme.appColors.background) {
            CourseSectionCard(
                mockChapterBlock,
                DownloadedState.DOWNLOADED,
                onItemClick = {},
                onDownloadClick = {}
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseHeaderPreview() {
    NewEdxTheme {
        Surface(color = MaterialTheme.appColors.background) {
            CourseImageHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(6.dp),
                courseCertificate = Certificate(""),
                courseImage = ""
            )
        }
    }
}

private val mockCourse = EnrolledCourse(
    auditAccessExpires = Date(),
    created = "created",
    certificate = Certificate(""),
    mode = "mode",
    isActive = true,
    course = EnrolledCourseData(
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = Date(),
        startDisplay = "",
        startType = "",
        end = Date(),
        dynamicUpgradeDeadline = "",
        subscriptionId = "",
        coursewareAccess = CoursewareAccess(
            true,
            "",
            "",
            "",
            "",
            ""
        ),
        media = null,
        courseImage = "",
        courseAbout = "",
        courseSharingUtmParameters = CourseSharingUtmParameters("", ""),
        courseUpdates = "",
        courseHandouts = "",
        discussionUrl = "",
        videoOutline = "",
        isSelfPaced = false
    )
)
private val mockChapterBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.CHAPTER,
    displayName = "Chapter",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    completion = 0.0
)