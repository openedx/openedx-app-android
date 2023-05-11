package com.raccoongang.course.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.domain.model.BlockCounts
import com.raccoongang.core.domain.model.Certificate
import com.raccoongang.core.domain.model.CourseSharingUtmParameters
import com.raccoongang.core.domain.model.CoursewareAccess
import com.raccoongang.core.domain.model.EnrolledCourse
import com.raccoongang.core.domain.model.EnrolledCourseData
import com.raccoongang.core.extension.isLinkValid
import com.raccoongang.core.module.db.DownloadedState
import com.raccoongang.core.ui.IconText
import com.raccoongang.core.ui.NewEdxButton
import com.raccoongang.core.ui.NewEdxOutlinedButton
import com.raccoongang.core.ui.WindowSize
import com.raccoongang.core.ui.WindowType
import com.raccoongang.core.ui.noRippleClickable
import com.raccoongang.core.ui.theme.NewEdxTheme
import com.raccoongang.core.ui.theme.appColors
import com.raccoongang.core.ui.theme.appShapes
import com.raccoongang.core.ui.theme.appTypography
import com.raccoongang.course.R
import org.jsoup.Jsoup
import subtitleFile.TimedTextObject
import java.util.Date
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                color = MaterialTheme.appColors.primary
                            )
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
    nextButtonText: String,
    hasPrevBlock: Boolean,
    hasNextBlock: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val nextButtonIcon = if (hasNextBlock) {
        painterResource(id = com.raccoongang.core.R.drawable.core_ic_down)
    } else {
        painterResource(id = com.raccoongang.core.R.drawable.core_ic_check)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Button(
            modifier = Modifier
                .width(124.dp)
                .height(42.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.appColors.buttonSecondaryBackground
            ),
            elevation = null,
            shape = MaterialTheme.appShapes.navigationButtonShape,
            onClick = onPrevClick,
            enabled = hasPrevBlock
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.course_navigation_prev),
                    color = MaterialTheme.appColors.buttonText,
                    style = MaterialTheme.appTypography.labelLarge
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = com.raccoongang.core.R.drawable.core_ic_up),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.buttonText
                )
            }
        }
        Spacer(Modifier.width(7.dp))
        Button(
            modifier = Modifier
                .width(124.dp)
                .height(42.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.appColors.buttonBackground
            ),
            elevation = null,
            shape = MaterialTheme.appShapes.navigationButtonShape,
            onClick = onNextClick
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
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

@Composable
fun CountUnits(
    units: Int,
    currentIndex: Int
) {
    val index by remember(currentIndex) {
        mutableStateOf(currentIndex)
    }
    Column(
        Modifier
            .width(24.dp)
            .padding(end = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(units) { iteration ->
            val (color, size) = if (iteration == currentIndex) {
                Pair(MaterialTheme.appColors.primary, 7)
            } else {
                Pair(MaterialTheme.appColors.bottomSheetToggle, 5)
            }

            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(size.dp)
            )
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
    subtitleLanguage: String,
    showSubtitleLanguage: Boolean,
    currentIndex: Int,
    onSettingsClick: () -> Unit
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = courseR.string.course_subtitles),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium
                    )
                    if (showSubtitleLanguage) {
                        IconText(
                            modifier = Modifier.noRippleClickable {
                                onSettingsClick()
                            },
                            text = subtitleLanguage,
                            painter = painterResource(id = courseR.drawable.course_ic_cc),
                            color = MaterialTheme.appColors.textAccent,
                            textStyle = MaterialTheme.appTypography.labelLarge
                        )
                    }
                }
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
            hasPrevBlock = true,
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
            hasPrevBlock = true,
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
            hasPrevBlock = true,
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
            hasPrevBlock = true,
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