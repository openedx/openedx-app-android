package org.openedx.course.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.jsoup.Jsoup
import org.openedx.core.BlockType
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.Progress
import org.openedx.core.extension.safeDivBy
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.ui.BackBtn
import org.openedx.core.ui.IconText
import org.openedx.core.ui.OpenEdXButton
import org.openedx.core.ui.OpenEdXOutlinedButton
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.core.utils.VideoPreview
import org.openedx.course.R
import org.openedx.course.presentation.dates.mockedCourseBannerInfo
import org.openedx.course.presentation.outline.getUnitBlockIcon
import org.openedx.foundation.extension.nonZero
import org.openedx.foundation.extension.toFileSize
import subtitleFile.Caption
import subtitleFile.TimedTextObject
import java.util.Date
import org.openedx.core.R as coreR

const val AUTO_SCROLL_DELAY = 3000L

@Composable
fun CourseSectionCard(
    block: Block,
    downloadedState: DownloadedState?,
    onItemClick: (Block) -> Unit,
    onDownloadClick: (Block) -> Unit,
) {
    val iconModifier = Modifier.size(24.dp)

    Column(
        modifier = Modifier.clickable { onItemClick(block) }
    ) {
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
            val completedIconPainter = if (block.isCompleted()) {
                painterResource(R.drawable.course_ic_task_alt)
            } else {
                painterResource(coreR.drawable.core_ic_chapter_icon)
            }
            val completedIconColor = if (block.isCompleted()) {
                MaterialTheme.appColors.primary
            } else {
                MaterialTheme.appColors.onSurface
            }
            val completedIconDescription = if (block.isCompleted()) {
                stringResource(id = R.string.course_accessibility_section_completed)
            } else {
                stringResource(id = R.string.course_accessibility_section_uncompleted)
            }
            Icon(
                painter = completedIconPainter,
                contentDescription = completedIconDescription,
                tint = completedIconColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (downloadedState == DownloadedState.DOWNLOADED ||
                    downloadedState == DownloadedState.NOT_DOWNLOADED
                ) {
                    val downloadIcon = if (downloadedState == DownloadedState.DOWNLOADED) {
                        Icons.Default.CloudDone
                    } else {
                        Icons.Outlined.CloudDownload
                    }
                    val downloadIconDescription =
                        if (downloadedState == DownloadedState.DOWNLOADED) {
                            stringResource(id = R.string.course_accessibility_remove_course_section)
                        } else {
                            stringResource(id = R.string.course_accessibility_download_course_section)
                        }
                    IconButton(
                        modifier = iconModifier,
                        onClick = { onDownloadClick(block) }
                    ) {
                        Icon(
                            imageVector = downloadIcon,
                            contentDescription = downloadIconDescription,
                            tint = MaterialTheme.appColors.textPrimary
                        )
                    }
                } else if (downloadedState != null) {
                    Box(contentAlignment = Alignment.Center) {
                        if (downloadedState == DownloadedState.DOWNLOADING ||
                            downloadedState == DownloadedState.WAITING
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                backgroundColor = Color.LightGray,
                                strokeWidth = 2.dp,
                                color = MaterialTheme.appColors.primary
                            )
                        }
                        IconButton(
                            modifier = iconModifier.padding(top = 2.dp),
                            onClick = { onDownloadClick(block) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription =
                                    stringResource(id = R.string.course_accessibility_stop_downloading_course_section),
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
fun OfflineQueueCard(
    downloadModel: DownloadModel,
    progressValue: Long,
    progressSize: Long,
    onDownloadClick: (DownloadModel) -> Unit,
) {
    val iconModifier = Modifier.size(24.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .padding(start = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = downloadModel.title.ifEmpty { stringResource(id = coreR.string.core_download_untitled) },
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Text(
                text = downloadModel.size.toFileSize(),
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textSecondary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            val progress = progressValue.toFloat().safeDivBy(progressSize.toFloat())
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                progress = progress
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .padding(end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                backgroundColor = Color.LightGray,
                strokeWidth = 2.dp,
                color = MaterialTheme.appColors.primary
            )
            IconButton(
                modifier = iconModifier.padding(2.dp),
                onClick = { onDownloadClick(downloadModel) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(
                        id = R.string.course_accessibility_stop_downloading_course_section
                    ),
                    tint = MaterialTheme.appColors.error
                )
            }
        }
    }
}

@Composable
fun CardArrow(
    degrees: Float,
    tint: Color = MaterialTheme.appColors.textDark,
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        tint = tint,
        contentDescription = null,
        modifier = Modifier.rotate(degrees),
    )
}

@Composable
fun VideoTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.appColors.textPrimary,
        style = MaterialTheme.appTypography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun NavigationUnitsButtons(
    nextButtonText: String,
    hasPrevBlock: Boolean,
    hasNextBlock: Boolean,
    isVerticalNavigation: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    val nextButtonIcon = if (hasNextBlock) {
        painterResource(id = coreR.drawable.core_ic_down)
    } else {
        painterResource(id = coreR.drawable.core_ic_check_in_box)
    }

    val subModifier =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Modifier
                .height(72.dp)
                .fillMaxWidth()
        } else {
            Modifier
                .statusBarsPadding()
                .padding(end = 32.dp)
                .padding(top = 2.dp)
        }

    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .then(subModifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (hasPrevBlock) {
            OutlinedButton(
                modifier = Modifier
                    .height(42.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MaterialTheme.appColors.background
                ),
                border = BorderStroke(1.dp, MaterialTheme.appColors.primaryButtonBorder),
                elevation = null,
                shape = MaterialTheme.appShapes.navigationButtonShape,
                onClick = onPrevClick,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.course_navigation_prev),
                        color = MaterialTheme.appColors.primary,
                        style = MaterialTheme.appTypography.labelLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        modifier = Modifier.rotate(if (isVerticalNavigation) 0f else -90f),
                        painter = painterResource(id = coreR.drawable.core_ic_up),
                        contentDescription = null,
                        tint = MaterialTheme.appColors.primary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        }
        Button(
            modifier = Modifier
                .height(42.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.appColors.primaryButtonBackground
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
                    color = MaterialTheme.appColors.primaryButtonText,
                    style = MaterialTheme.appTypography.labelLarge
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    modifier = Modifier.rotate(if (isVerticalNavigation || !hasNextBlock) 0f else -90f),
                    painter = nextButtonIcon,
                    contentDescription = null,
                    tint = MaterialTheme.appColors.primaryButtonText
                )
            }
        }
    }
}

@Composable
fun HorizontalPageIndicator(
    modifier: Modifier = Modifier,
    blocks: List<Block>,
    selectedPage: Int = 0,
    completedAndSelectedColor: Color = Color.Green,
    completedColor: Color = Color.Green,
    selectedColor: Color = Color.White,
    defaultColor: Color = Color.Gray,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier
    ) {
        blocks.forEachIndexed { index, block ->
            val backgroundColor = when {
                block.isCompleted() && index == selectedPage -> completedAndSelectedColor
                block.isCompleted() -> completedColor
                index == selectedPage -> selectedColor
                else -> defaultColor
            }

            Surface(
                modifier = Modifier
                    .padding(vertical = if (index == selectedPage) 0.dp else 1.dp)
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .background(backgroundColor)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun VerticalPageIndicator(
    modifier: Modifier = Modifier,
    numberOfPages: Int,
    selectedPage: Int = 0,
    selectedColor: Color = Color.White,
    defaultColor: Color = Color.Gray,
    defaultRadius: Dp = 8.dp,
    selectedLength: Dp = 25.dp,
    space: Dp = 4.dp,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        repeat(numberOfPages) {
            Indicator(
                isSelected = it == selectedPage,
                selectedColor = selectedColor,
                defaultColor = defaultColor,
                defaultRadius = defaultRadius,
                selectedSize = selectedLength,
                modifier = Modifier.padding(vertical = space)
            )
        }
    }
}

@Composable
fun Indicator(
    isSelected: Boolean,
    selectedColor: Color,
    defaultColor: Color,
    defaultRadius: Dp,
    selectedSize: Dp,
    modifier: Modifier = Modifier,
) {
    val size by animateDpAsState(
        targetValue = if (isSelected) selectedSize else defaultRadius,
        animationSpec = tween(durationMillis = 300),
        label = ""
    )
    val color by animateColorAsState(
        targetValue = if (isSelected) selectedColor else defaultColor,
        animationSpec = tween(durationMillis = 300),
        label = ""
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .size(size)
    )
}

@Composable
fun VideoSubtitles(
    listState: LazyListState,
    timedTextObject: TimedTextObject?,
    subtitleLanguage: String,
    showSubtitleLanguage: Boolean,
    currentIndex: Int,
    onTranscriptClick: (Caption) -> Unit,
    onSettingsClick: () -> Unit,
) {
    timedTextObject?.let {
        var lastScrollTime by remember {
            mutableLongStateOf(0L)
        }
        if (listState.isScrollInProgress) {
            lastScrollTime = Date().time
        }

        LaunchedEffect(key1 = currentIndex) {
            if (currentIndex > 1 && lastScrollTime + AUTO_SCROLL_DELAY < Date().time) {
                listState.animateScrollToItem(currentIndex - 1)
            }
        }
        val scaffoldState = rememberScaffoldState()
        val subtitles = timedTextObject.captions.values.toList()
        Scaffold(scaffoldState = scaffoldState) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .background(color = MaterialTheme.appColors.background)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.course_subtitles),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium
                    )
                    if (showSubtitleLanguage) {
                        IconText(
                            modifier = Modifier.noRippleClickable {
                                onSettingsClick()
                            },
                            text = subtitleLanguage,
                            painter = painterResource(id = R.drawable.course_ic_cc),
                            color = MaterialTheme.appColors.textAccent,
                            textStyle = MaterialTheme.appTypography.labelLarge
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                LazyColumn(
                    state = listState
                ) {
                    itemsIndexed(subtitles) { index, item ->
                        val textColor =
                            if (currentIndex == index) {
                                MaterialTheme.appColors.textPrimary
                            } else {
                                MaterialTheme.appColors.textFieldBorder
                            }
                        val fontWeight = if (currentIndex == index) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .noRippleClickable {
                                    onTranscriptClick(item)
                                },
                            text = Jsoup.parse(item.content).text(),
                            color = textColor,
                            style = MaterialTheme.appTypography.bodyMedium,
                            fontWeight = fontWeight,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CourseVideoSection(
    block: Block,
    videoBlocks: List<Block>,
    preview: Map<String, VideoPreview?>,
    progress: Map<String, Float>,
    downloadedStateMap: Map<String, DownloadedState>,
    onVideoClick: (Block) -> Unit,
    onDownloadClick: (blocksIds: List<String>) -> Unit,
) {
    val state = rememberLazyListState()
    val subSectionIds = videoBlocks.map { it.id }
    val filteredStatuses = downloadedStateMap.filterKeys { it in subSectionIds }.values
    val downloadedState = when {
        filteredStatuses.isEmpty() -> null
        filteredStatuses.all { it.isDownloaded } -> DownloadedState.DOWNLOADED
        filteredStatuses.any { it.isWaitingOrDownloading } -> DownloadedState.DOWNLOADING
        else -> DownloadedState.NOT_DOWNLOADED
    }

    LaunchedEffect(Unit) {
        try {
            val uncompletedBlockIndex = videoBlocks.indexOf(videoBlocks.find { !it.isCompleted() })
            state.scrollToItem(uncompletedBlockIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        CourseVideoSectionHeader(
            block = block,
            downloadedState = downloadedState,
            videoBlocks = videoBlocks,
            onDownloadClick = {
                onDownloadClick(block.descendants)
            }
        )
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp,
            )
        ) {
            items(videoBlocks) { block ->
                CourseVideoItem(
                    videoBlock = block,
                    preview = preview[block.id],
                    progress = progress[block.id] ?: 0f,
                    onClick = {
                        onVideoClick(block)
                    }
                )
            }
        }
        Divider(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun CourseVideoItem(
    videoBlock: Block,
    preview: VideoPreview?,
    progress: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(192.dp)
            .height(108.dp)
            .clip(MaterialTheme.appShapes.videoPreviewShape)
            .let {
                if (videoBlock.isCompleted()) {
                    it.border(
                        width = 3.dp,
                        color = MaterialTheme.appColors.successGreen,
                        shape = MaterialTheme.appShapes.videoPreviewShape
                    )
                } else {
                    it
                }
            }
            .clickable { onClick() }
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize(),
            model = ImageRequest.Builder(LocalContext.current)
                .data(preview?.link ?: preview?.bitmap)
                .error(coreR.drawable.core_no_image_course)
                .placeholder(coreR.drawable.core_no_image_course)
                .build(),
            contentDescription = stringResource(R.string.course_accessibility_video_player),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Image(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center),
            painter = painterResource(id = R.drawable.course_video_play_button),
            contentDescription = null,
        )

        // Title (top-left)
        Text(
            text = videoBlock.displayName,
            color = Color.White,
            style = MaterialTheme.appTypography.bodySmall,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Progress bar (bottom)
        if (progress > 0.0f) {
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .height(16.dp)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = 8.dp)
                        .clip(CircleShape),
                    progress = progress,
                    color = if (videoBlock.isCompleted() && progress > 0.95f) {
                        MaterialTheme.appColors.progressBarColor
                    } else {
                        MaterialTheme.appColors.primary
                    },
                    backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor
                )
                if (videoBlock.isCompleted()) {
                    Image(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .offset(x = (-4).dp),
                        painter = painterResource(id = coreR.drawable.ic_core_check),
                        contentDescription = stringResource(R.string.course_accessibility_video_watched),
                    )
                }
            }
        }
    }
}

@Composable
fun CourseVideoSectionHeader(
    modifier: Modifier = Modifier,
    block: Block,
    videoBlocks: List<Block>?,
    downloadedState: DownloadedState?,
    onDownloadClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = block.displayName,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.course_video_watched,
                    videoBlocks?.filter { it.isCompleted() }?.size ?: 0,
                    videoBlocks?.size ?: 0
                ),
                style = MaterialTheme.appTypography.bodySmall,
                color = MaterialTheme.appColors.textPrimary,
            )
        }
        DownloadIcon(
            downloadedState = downloadedState,
            onDownloadClick = onDownloadClick
        )
    }
}

@Composable
fun DownloadIcon(
    downloadedState: DownloadedState?,
    onDownloadClick: () -> Unit,
) {
    val iconModifier = Modifier.size(24.dp)
    Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (downloadedState == DownloadedState.DOWNLOADED || downloadedState == DownloadedState.NOT_DOWNLOADED) {
            val downloadIcon = if (downloadedState == DownloadedState.DOWNLOADED) {
                Icons.Default.CloudDone
            } else {
                Icons.Outlined.CloudDownload
            }
            val downloadIconDescription = if (downloadedState == DownloadedState.DOWNLOADED) {
                stringResource(id = R.string.course_accessibility_remove_course_section)
            } else {
                stringResource(id = R.string.course_accessibility_download_course_section)
            }
            val downloadIconTint = if (downloadedState == DownloadedState.DOWNLOADED) {
                MaterialTheme.appColors.successGreen
            } else {
                MaterialTheme.appColors.textAccent
            }
            IconButton(
                modifier = iconModifier,
                onClick = { onDownloadClick() }
            ) {
                Icon(
                    imageVector = downloadIcon,
                    contentDescription = downloadIconDescription,
                    tint = downloadIconTint
                )
            }
        } else if (downloadedState != null) {
            Box(contentAlignment = Alignment.Center) {
                if (downloadedState == DownloadedState.DOWNLOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        backgroundColor = Color.LightGray,
                        strokeWidth = 2.dp,
                        color = MaterialTheme.appColors.primary
                    )
                } else if (downloadedState == DownloadedState.WAITING) {
                    Icon(
                        painter = painterResource(id = coreR.drawable.core_download_waiting),
                        contentDescription = stringResource(
                            id = R.string.course_accessibility_stop_downloading_course_section
                        ),
                        tint = MaterialTheme.appColors.error
                    )
                }
                IconButton(
                    modifier = iconModifier.padding(2.dp),
                    onClick = { onDownloadClick() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(
                            id = R.string.course_accessibility_stop_downloading_course_section
                        ),
                        tint = MaterialTheme.appColors.error
                    )
                }
            }
        }
    }
}

@Composable
fun CourseSection(
    modifier: Modifier = Modifier,
    block: Block,
    useRelativeDates: Boolean,
    onItemClick: (Block) -> Unit,
    isSectionVisible: Boolean?,
    courseSubSections: List<Block>?,
    downloadedStateMap: Map<String, DownloadedState>,
    onSubSectionClick: (Block) -> Unit,
    onDownloadClick: (blocksIds: List<String>) -> Unit,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isSectionVisible == true) {
            -90f
        } else {
            90f
        },
        label = ""
    )
    val subSectionIds = courseSubSections?.map { it.id }.orEmpty()
    val filteredStatuses = downloadedStateMap.filterKeys { it in subSectionIds }.values
    val downloadedState = when {
        filteredStatuses.isEmpty() -> null
        filteredStatuses.all { it.isDownloaded } -> DownloadedState.DOWNLOADED
        filteredStatuses.any { it.isWaitingOrDownloading } -> DownloadedState.DOWNLOADING
        else -> DownloadedState.NOT_DOWNLOADED
    }

    // Section progress
    val completedCount = courseSubSections?.count { it.isCompleted() } ?: 0
    val totalCount = courseSubSections?.size ?: 0
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Column(
        modifier = modifier
            .clip(MaterialTheme.appShapes.sectionCardShape)
            .noRippleClickable { onItemClick(block) }
            .background(MaterialTheme.appColors.cardViewBackground)
            .border(
                1.dp,
                MaterialTheme.appColors.cardViewBorder,
                MaterialTheme.appShapes.sectionCardShape
            )
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            progress = progress,
            color = MaterialTheme.appColors.progressBarColor,
            backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor
        )
        CourseExpandableChapterCard(
            block = block,
            arrowDegrees = arrowRotation,
            downloadedState = downloadedState,
            onDownloadClick = {
                onDownloadClick(block.descendants)
            }
        )
        courseSubSections?.forEach { subSectionBlock ->
            AnimatedVisibility(
                visible = isSectionVisible == true
            ) {
                CourseSubSectionItem(
                    block = subSectionBlock,
                    onClick = onSubSectionClick,
                    useRelativeDates = useRelativeDates
                )
            }
        }
    }
}

@Composable
fun CourseExpandableChapterCard(
    modifier: Modifier = Modifier,
    block: Block,
    arrowDegrees: Float = 0f,
    downloadedState: DownloadedState?,
    onDownloadClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(vertical = 8.dp)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CardArrow(degrees = arrowDegrees)
        if (block.isCompleted()) {
            val completedIconPainter = painterResource(R.drawable.course_ic_task_alt)
            val completedIconColor = MaterialTheme.appColors.successGreen
            val completedIconDescription =
                stringResource(id = R.string.course_accessibility_section_completed)

            Icon(
                painter = completedIconPainter,
                contentDescription = completedIconDescription,
                tint = completedIconColor
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = block.displayName,
            style = MaterialTheme.appTypography.titleSmall,
            color = MaterialTheme.appColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DownloadIcon(
            downloadedState = downloadedState,
            onDownloadClick = onDownloadClick
        )
    }
}

@Composable
fun CourseSubSectionItem(
    modifier: Modifier = Modifier,
    block: Block,
    useRelativeDates: Boolean,
    onClick: (Block) -> Unit,
) {
    val context = LocalContext.current
    val icon = if (block.isCompleted()) {
        painterResource(R.drawable.course_ic_task_alt)
    } else {
        painterResource(coreR.drawable.core_ic_chapter_icon)
    }
    val iconColor = if (block.isCompleted()) {
        MaterialTheme.appColors.successGreen
    } else {
        MaterialTheme.appColors.onSurface
    }
    val due by rememberSaveable {
        mutableStateOf(
            block.due?.let { TimeUtils.formatToString(context, it, useRelativeDates) }
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(block) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = block.displayName,
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(16.dp))
            if (due != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    tint = MaterialTheme.appColors.onSurface,
                    contentDescription = null
                )
            }
        }
        val strings = listOf(
            block.assignmentProgress?.assignmentType,
            due?.let {
                stringResource(
                    id = coreR.string.core_date_format_assignment_due,
                    it
                )
            },
            block.assignmentProgress?.numPointsPossible?.let {
                if (it > 0) {
                    block.assignmentProgress?.toPointString(" ")
                } else {
                    null
                }
            }
        )
        val assignmentString = strings
            .filter { !it.isNullOrEmpty() }
            .joinToString(" - ")

        if (assignmentString.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = assignmentString,
                style = MaterialTheme.appTypography.bodySmall,
                color = MaterialTheme.appColors.textPrimary
            )
        }
    }
}

@Composable
fun CourseUnitToolbar(
    title: String,
    onBackClick: () -> Unit,
) {
    OpenEdXTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .displayCutoutForLandscape()
                .zIndex(1f)
                .statusBarsPadding()
        ) {
            BackBtn { onBackClick() }
            Text(
                modifier = Modifier
                    .padding(horizontal = 56.dp)
                    .align(Alignment.Center),
                text = title,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SubSectionUnitsTitle(
    unitName: String,
    unitsCount: Int,
    unitsListShowed: Boolean,
    onUnitsClick: () -> Unit,
) {
    val textStyle = MaterialTheme.appTypography.titleMedium
    val hasMultipleUnits = unitsCount > 1
    var rowModifier = Modifier
        .fillMaxWidth()
        .padding(
            horizontal = 16.dp,
            vertical = 8.dp
        )
        .displayCutoutForLandscape()
    if (hasMultipleUnits) {
        rowModifier = rowModifier.noRippleClickable { onUnitsClick() }
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = unitName,
            color = MaterialTheme.appColors.textPrimary,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )

        if (hasMultipleUnits) {
            Icon(
                modifier = Modifier.rotate(if (unitsListShowed) 180f else 0f),
                painter = painterResource(id = R.drawable.course_ic_arrow_down),
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary
            )
        }
    }
}

@Composable
fun SubSectionUnitsList(
    unitBlocks: List<Block>,
    selectedUnitIndex: Int = 0,
    onUnitClick: (index: Int, unit: Block) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top)
            .displayCutoutForLandscape(),
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
    ) {
        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(unitBlocks) { index, unit ->
                Column(
                    modifier = Modifier
                        .background(
                            if (index == selectedUnitIndex) {
                                MaterialTheme.appColors.surface
                            } else {
                                MaterialTheme.appColors.background
                            }
                        )
                        .clickable { onUnitClick(index, unit) }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier
                                .size(16.dp)
                                .alpha(if (unit.isCompleted()) 1f else 0f),
                            painter = painterResource(id = coreR.drawable.core_ic_check),
                            contentDescription = "done"
                        )
                        Text(
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp)
                                .weight(1f),
                            text = unit.displayName,
                            color = MaterialTheme.appColors.textPrimary,
                            style = MaterialTheme.appTypography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                        )
                        Icon(
                            modifier = Modifier
                                .size(18.dp),
                            painter = painterResource(
                                id = getUnitBlockIcon(unit)
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.appColors.textPrimary
                        )
                    }
                    if (unit.isGated()) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier
                                    .size(16.dp),
                                painter = painterResource(id = R.drawable.course_ic_gated),
                                contentDescription = "gated"
                            )
                            Text(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 8.dp)
                                    .weight(1f),
                                text = stringResource(
                                    id = R.string.course_gated_content_label
                                ),
                                color = MaterialTheme.appColors.textPrimaryVariant,
                                style = MaterialTheme.appTypography.labelSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun CourseDatesBanner(
    modifier: Modifier = Modifier,
    banner: CourseDatesBannerInfo,
    resetDates: () -> Unit,
) {
    val cardModifier = modifier
        .background(
            MaterialTheme.appColors.cardViewBackground,
            MaterialTheme.appShapes.material.medium
        )
        .border(
            1.dp,
            MaterialTheme.appColors.cardViewBorder,
            MaterialTheme.appShapes.material.medium
        )
        .padding(16.dp)

    Column(modifier = cardModifier) {
        banner.bannerType.headerResId.nonZero()?.let {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResource(id = it),
                style = MaterialTheme.appTypography.titleMedium,
                color = MaterialTheme.appColors.textDark
            )
        }

        banner.bannerType.bodyResId.nonZero()?.let {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResource(id = it),
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textDark
            )
        }

        banner.bannerType.buttonResId.nonZero()?.let {
            OpenEdXButton(
                text = stringResource(id = it),
                onClick = resetDates,
            )
        }
    }
}

@Composable
fun CourseDatesBannerTablet(
    modifier: Modifier = Modifier,
    banner: CourseDatesBannerInfo,
    resetDates: () -> Unit,
) {
    val cardModifier = modifier
        .background(
            MaterialTheme.appColors.cardViewBackground,
            MaterialTheme.appShapes.material.medium
        )
        .border(
            1.dp,
            MaterialTheme.appColors.cardViewBorder,
            MaterialTheme.appShapes.material.medium
        )
        .padding(16.dp)

    Row(
        modifier = cardModifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            banner.bannerType.headerResId.nonZero()?.let {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = stringResource(id = it),
                    style = MaterialTheme.appTypography.titleMedium,
                    color = MaterialTheme.appColors.textDark
                )
            }

            banner.bannerType.bodyResId.nonZero()?.let {
                Text(
                    text = stringResource(id = it),
                    style = MaterialTheme.appTypography.bodyMedium,
                    color = MaterialTheme.appColors.textDark
                )
            }
        }
        banner.bannerType.buttonResId.nonZero()?.let {
            OpenEdXButton(
                modifier = Modifier.width(210.dp),
                text = stringResource(id = it),
                onClick = resetDates,
            )
        }
    }
}

@Composable
fun DatesShiftedSnackBar(
    showAction: Boolean = false,
    onViewDates: () -> Unit? = {},
    onClose: () -> Unit? = {},
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        backgroundColor = MaterialTheme.appColors.background
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Box {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart),
                    text = stringResource(id = coreR.string.core_dates_shift_dates_successfully_title),
                    color = MaterialTheme.appColors.textFieldText,
                    style = MaterialTheme.appTypography.titleMedium
                )
                IconButton(modifier = Modifier.align(Alignment.TopEnd), onClick = { onClose() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "close",
                        tint = MaterialTheme.appColors.onBackground,
                    )
                }
            }
            Text(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth(),
                text = stringResource(id = coreR.string.core_dates_shift_dates_successfully_msg),
                color = MaterialTheme.appColors.textFieldText,
                style = MaterialTheme.appTypography.titleSmall,
            )
            if (showAction) {
                OpenEdXOutlinedButton(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    text = stringResource(id = coreR.string.core_dates_view_all_dates),
                    backgroundColor = MaterialTheme.appColors.background,
                    textColor = MaterialTheme.appColors.primary,
                    borderColor = MaterialTheme.appColors.primary,
                    onClick = {
                        onViewDates()
                    }
                )
            }
        }
    }
}

@Composable
fun CourseMessage(
    modifier: Modifier = Modifier,
    icon: Painter,
    message: String,
    action: String? = null,
    onActionClick: () -> Unit = {},
) {
    Column {
        Row(
            modifier
                .semantics(mergeDescendants = true) {}
                .noRippleClickable(onActionClick)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterVertically),
                tint = MaterialTheme.appColors.textPrimary
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    text = message,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.labelLarge
                )
                if (action != null) {
                    Text(
                        text = action,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.labelLarge.copy(textDecoration = TextDecoration.Underline)
                    )
                }
            }
        }
        Divider(
            color = MaterialTheme.appColors.divider
        )
    }
}

@Composable
fun CourseProgress(
    modifier: Modifier = Modifier,
    progress: Progress,
    description: String,
    isCompletedShown: Boolean = false,
    onVisibilityChanged: (() -> Unit)? = null
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isCompletedShown) {
            -90f
        } else {
            90f
        },
        label = ""
    )
    val buttonText = if (isCompletedShown) {
        stringResource(R.string.course_hide_completed)
    } else {
        stringResource(R.string.course_view_completed)
    }
    Column(
        modifier = modifier,
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            progress = progress.value,
            color = MaterialTheme.appColors.progressBarColor,
            backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = description,
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.labelSmall
            )
            if (onVisibilityChanged != null) {
                Row(
                    modifier = Modifier.clickable {
                        onVisibilityChanged()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = buttonText,
                        color = MaterialTheme.appColors.textAccent,
                        style = MaterialTheme.appTypography.labelMedium
                    )
                    CardArrow(
                        degrees = arrowRotation,
                        tint = MaterialTheme.appColors.textAccent,
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationUnitsButtonsOnlyNextButtonPreview() {
    OpenEdXTheme {
        NavigationUnitsButtons(
            hasPrevBlock = true,
            hasNextBlock = true,
            isVerticalNavigation = true,
            nextButtonText = "Next",
            onPrevClick = {}
        ) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationUnitsButtonsOnlyFinishButtonPreview() {
    OpenEdXTheme {
        NavigationUnitsButtons(
            hasPrevBlock = true,
            hasNextBlock = false,
            isVerticalNavigation = true,
            nextButtonText = "Finish",
            onPrevClick = {}
        ) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationUnitsButtonsWithFinishPreview() {
    OpenEdXTheme {
        NavigationUnitsButtons(
            hasPrevBlock = true,
            hasNextBlock = false,
            isVerticalNavigation = true,
            nextButtonText = "Finish",
            onPrevClick = {}
        ) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationUnitsButtonsWithNextPreview() {
    OpenEdXTheme {
        NavigationUnitsButtons(
            hasPrevBlock = true,
            hasNextBlock = true,
            isVerticalNavigation = true,
            nextButtonText = "Next",
            onPrevClick = {}
        ) {}
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseSectionCardPreview() {
    OpenEdXTheme {
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
private fun CourseDatesBannerPreview() {
    OpenEdXTheme {
        CourseDatesBanner(
            modifier = Modifier,
            banner = mockedCourseBannerInfo,
            resetDates = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseDatesBannerTabletPreview() {
    OpenEdXTheme {
        CourseDatesBannerTablet(
            modifier = Modifier,
            banner = mockedCourseBannerInfo,
            resetDates = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OfflineQueueCardPreview() {
    OpenEdXTheme {
        Surface(color = MaterialTheme.appColors.background) {
            OfflineQueueCard(
                downloadModel = DownloadModel(
                    courseId = "",
                    id = "",
                    title = "Problems of society",
                    size = 4000,
                    path = "",
                    url = "",
                    type = FileType.VIDEO,
                    downloadedState = DownloadedState.DOWNLOADING,
                ),
                progressValue = 10,
                progressSize = 30,
                onDownloadClick = {}
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseMessagePreview() {
    OpenEdXTheme {
        Surface(color = MaterialTheme.appColors.background) {
            CourseMessage(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                icon = painterResource(R.drawable.course_ic_certificate),
                message = stringResource(
                    R.string.course_you_earned_certificate,
                    "Demo Course"
                ),
                action = stringResource(R.string.course_view_certificate),
            )
        }
    }
}

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
    descendantsType = BlockType.CHAPTER,
    completion = 0.0,
    containsGatedContent = false,
    assignmentProgress = AssignmentProgress("", 1f, 2f, "HM1"),
    due = Date(),
    offlineDownload = null
)
