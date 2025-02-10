@file:OptIn(ExperimentalComposeUiApi::class)

package org.openedx.discussion.presentation.ui

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.openedx.core.domain.model.ProfileImage
import org.openedx.core.extension.TextConverter
import org.openedx.core.ui.AutoSizeText
import org.openedx.core.ui.HyperlinkImageText
import org.openedx.core.ui.IconText
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.discussion.R
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.domain.model.Topic
import org.openedx.discussion.presentation.comments.DiscussionCommentsFragment
import org.openedx.core.R as CoreR

@Composable
fun ThreadMainItem(
    modifier: Modifier,
    thread: org.openedx.discussion.domain.model.Thread,
    onClick: (String, Boolean) -> Unit,
    onUserPhotoClick: (String) -> Unit
) {
    val profileImageUrl = if (thread.users?.get(thread.author)?.image?.hasImage == true) {
        thread.users[thread.author]?.image?.imageUrlFull
    } else {
        CoreR.drawable.core_ic_default_profile_picture
    }

    val votePainter = if (thread.voted) {
        painterResource(id = R.drawable.discussion_ic_like_success)
    } else {
        painterResource(id = R.drawable.discussion_ic_like)
    }
    val voteColor = if (thread.voted) {
        MaterialTheme.appColors.primary
    } else {
        MaterialTheme.appColors.textPrimaryVariant
    }
    val reportText = if (thread.abuseFlagged) {
        stringResource(id = R.string.discussion_unreport)
    } else {
        stringResource(id = R.string.discussion_report)
    }
    val reportColor = if (thread.abuseFlagged) {
        MaterialTheme.appColors.error
    } else {
        MaterialTheme.appColors.textPrimaryVariant
    }

    val context = LocalContext.current

    Column(
        modifier = modifier
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImageUrl)
                    .error(CoreR.drawable.core_ic_default_profile_picture)
                    .placeholder(CoreR.drawable.core_ic_default_profile_picture)
                    .build(),
                contentDescription = stringResource(
                    id = CoreR.string.core_accessibility_user_profile_image,
                    thread.author
                ),
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.appShapes.material.medium)
                    .clickable {
                        if (thread.author.isNotEmpty()) {
                            onUserPhotoClick(thread.author)
                        }
                    }
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (thread.author.isNotEmpty()) {
                            onUserPhotoClick(thread.author)
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = thread.author.ifEmpty { stringResource(id = R.string.discussion_anonymous) },
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium
                )
                Text(
                    text = TimeUtils.iso8601ToDateWithTime(context, thread.createdAt),
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textPrimaryVariant
                )
            }
            IconText(
                text = stringResource(id = R.string.discussion_follow),
                painter = painterResource(
                    if (thread.following) {
                        R.drawable.discussion_star_filled
                    } else {
                        R.drawable.discussion_star
                    }
                ),
                textStyle = MaterialTheme.appTypography.labelLarge,
                color = MaterialTheme.appColors.textPrimaryVariant,
                onClick = {
                    onClick(DiscussionCommentsFragment.ACTION_FOLLOW_THREAD, !thread.following)
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        HyperlinkImageText(
            title = thread.title,
            imageText = thread.parsedRenderedBody,
            linkTextColor = MaterialTheme.appColors.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconText(
                text = pluralStringResource(
                    id = R.plurals.discussion_votes,
                    thread.voteCount,
                    thread.voteCount
                ),
                painter = votePainter,
                color = voteColor,
                textStyle = MaterialTheme.appTypography.labelLarge,
                onClick = {
                    onClick(DiscussionCommentsFragment.ACTION_UPVOTE_THREAD, !thread.voted)
                }
            )
            IconText(
                text = reportText,
                painter = painterResource(id = R.drawable.discussion_ic_report),
                textStyle = MaterialTheme.appTypography.labelLarge,
                color = reportColor,
                onClick = {
                    onClick(DiscussionCommentsFragment.ACTION_REPORT_THREAD, !thread.abuseFlagged)
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.appColors.cardViewBorder)
    }
}

@Composable
fun CommentItem(
    modifier: Modifier,
    comment: DiscussionComment,
    shape: Shape = MaterialTheme.appShapes.cardShape,
    onClick: (String, String, Boolean) -> Unit,
    onAddCommentClick: () -> Unit = {},
    onUserPhotoClick: (String) -> Unit
) {
    val profileImageUrl = if (comment.profileImage?.hasImage == true) {
        comment.profileImage.imageUrlFull
    } else if (comment.users?.get(comment.author)?.image?.hasImage == true) {
        comment.users[comment.author]?.image?.imageUrlFull
    } else {
        CoreR.drawable.core_ic_default_profile_picture
    }

    val reportText = if (comment.abuseFlagged) {
        stringResource(id = R.string.discussion_unreport)
    } else {
        stringResource(id = R.string.discussion_report)
    }

    val reportColor = if (comment.abuseFlagged) {
        MaterialTheme.appColors.error
    } else {
        MaterialTheme.appColors.textPrimaryVariant
    }
    val votePainter = if (comment.voted) {
        painterResource(id = R.drawable.discussion_ic_like_success)
    } else {
        painterResource(id = R.drawable.discussion_ic_like)
    }
    val voteColor = if (comment.voted) {
        MaterialTheme.appColors.textAccent
    } else {
        MaterialTheme.appColors.textPrimaryVariant
    }

    val context = LocalContext.current

    Card(
        shape = shape,
        modifier = modifier.then(
            Modifier.border(
                1.dp,
                MaterialTheme.appColors.cardViewBorder,
                shape
            )
        ),
        backgroundColor = MaterialTheme.appColors.surface,
        elevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileImageUrl)
                        .error(CoreR.drawable.core_ic_default_profile_picture)
                        .placeholder(CoreR.drawable.core_ic_default_profile_picture)
                        .build(),
                    contentDescription = stringResource(
                        id = CoreR.string.core_accessibility_user_profile_image,
                        comment.author
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            onUserPhotoClick(comment.author)
                        }
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onUserPhotoClick(comment.author)
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = comment.author,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleSmall
                    )
                    Text(
                        text = TimeUtils.iso8601ToDateWithTime(context, comment.createdAt),
                        style = MaterialTheme.appTypography.labelSmall,
                        color = MaterialTheme.appColors.textPrimaryVariant
                    )
                }
                IconText(
                    text = reportText,
                    painter = painterResource(id = R.drawable.discussion_ic_report),
                    textStyle = MaterialTheme.appTypography.labelMedium,
                    color = reportColor,
                    onClick = {
                        onClick(
                            DiscussionCommentsFragment.ACTION_REPORT_COMMENT,
                            comment.id,
                            !comment.abuseFlagged
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            HyperlinkImageText(
                imageText = comment.parsedRenderedBody,
                linkTextColor = MaterialTheme.appColors.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconText(
                    text = pluralStringResource(
                        id = R.plurals.discussion_votes,
                        comment.voteCount,
                        comment.voteCount
                    ),
                    painter = votePainter,
                    color = voteColor,
                    textStyle = MaterialTheme.appTypography.labelLarge,
                    onClick = {
                        onClick(
                            DiscussionCommentsFragment.ACTION_UPVOTE_COMMENT,
                            comment.id,
                            !comment.voted
                        )
                    }
                )
                IconText(
                    text = pluralStringResource(
                        id = R.plurals.discussion_comments,
                        comment.childCount,
                        comment.childCount
                    ),
                    painter = painterResource(id = R.drawable.discussion_ic_comment),
                    color = MaterialTheme.appColors.textPrimaryVariant,
                    textStyle = MaterialTheme.appTypography.labelLarge,
                    onClick = {
                        onAddCommentClick()
                    }
                )
            }
        }
    }
}

@Composable
fun CommentMainItem(
    modifier: Modifier,
    internalPadding: Dp = 16.dp,
    comment: DiscussionComment,
    onClick: (String, String, Boolean) -> Unit,
    onUserPhotoClick: (String) -> Unit
) {
    val profileImageUrl = if (comment.profileImage?.hasImage == true) {
        comment.profileImage.imageUrlFull
    } else if (comment.users?.get(comment.author)?.image?.hasImage == true) {
        comment.users[comment.author]?.image?.imageUrlFull
    } else {
        CoreR.drawable.core_ic_default_profile_picture
    }

    val reportText = if (comment.abuseFlagged) {
        stringResource(id = R.string.discussion_unreport)
    } else {
        stringResource(id = R.string.discussion_report)
    }
    val reportColor = if (comment.abuseFlagged) {
        MaterialTheme.appColors.error
    } else {
        MaterialTheme.appColors.textPrimaryVariant
    }

    val votePainter = if (comment.voted) {
        painterResource(id = R.drawable.discussion_ic_like_success)
    } else {
        painterResource(id = R.drawable.discussion_ic_like)
    }
    val voteColor = if (comment.voted) {
        MaterialTheme.appColors.textAccent
    } else {
        MaterialTheme.appColors.textPrimaryVariant
    }

    val context = LocalContext.current

    Surface(
        modifier = modifier,
        color = MaterialTheme.appColors.background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(internalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileImageUrl)
                        .error(CoreR.drawable.core_ic_default_profile_picture)
                        .placeholder(CoreR.drawable.core_ic_default_profile_picture)
                        .build(),
                    contentDescription = stringResource(
                        id = CoreR.string.core_accessibility_user_profile_image,
                        comment.author
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            onUserPhotoClick(comment.author)
                        }
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onUserPhotoClick(comment.author)
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = comment.author,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium
                    )
                    Text(
                        text = TimeUtils.iso8601ToDateWithTime(context, comment.createdAt),
                        style = MaterialTheme.appTypography.labelSmall,
                        color = MaterialTheme.appColors.textPrimaryVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HyperlinkImageText(
                imageText = comment.parsedRenderedBody,
                linkTextColor = MaterialTheme.appColors.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconText(
                    text = pluralStringResource(
                        id = R.plurals.discussion_votes,
                        comment.voteCount,
                        comment.voteCount
                    ),
                    painter = votePainter,
                    color = voteColor,
                    textStyle = MaterialTheme.appTypography.labelLarge,
                    onClick = {
                        onClick(
                            DiscussionCommentsFragment.ACTION_UPVOTE_COMMENT,
                            comment.id,
                            !comment.voted
                        )
                    }
                )
                IconText(
                    text = reportText,
                    painter = painterResource(id = R.drawable.discussion_ic_report),
                    textStyle = MaterialTheme.appTypography.labelLarge,
                    color = reportColor,
                    onClick = {
                        onClick(
                            DiscussionCommentsFragment.ACTION_REPORT_COMMENT,
                            comment.id,
                            !comment.abuseFlagged
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ThreadItem(
    thread: org.openedx.discussion.domain.model.Thread,
    onClick: (org.openedx.discussion.domain.model.Thread) -> Unit,
) {
    val icon = when (thread.type) {
        DiscussionType.DISCUSSION -> painterResource(id = R.drawable.discussion_ic_discussion)
        DiscussionType.QUESTION -> rememberVectorPainter(image = Icons.AutoMirrored.Outlined.HelpOutline)
    }
    val textType = when (thread.type) {
        DiscussionType.DISCUSSION -> stringResource(id = R.string.discussion_discussion)
        DiscussionType.QUESTION -> stringResource(id = R.string.discussion_question)
    }

    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.appColors.background)
            .clickable { onClick(thread) }
            .padding(vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconText(
                text = textType,
                painter = icon,
                color = MaterialTheme.appColors.textPrimaryVariant,
                textStyle = MaterialTheme.appTypography.labelSmall
            )
            if (thread.unreadCommentCount > 0 && !thread.read) {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        Icon(
                            modifier = Modifier.size((MaterialTheme.appTypography.labelLarge.fontSize.value).dp),
                            painter = painterResource(id = R.drawable.discussion_ic_unread_replies),
                            tint = MaterialTheme.appColors.textPrimaryVariant,
                            contentDescription = null
                        )
                        Image(
                            modifier = Modifier.size((MaterialTheme.appTypography.labelLarge.fontSize.value).dp),
                            painter = painterResource(id = R.drawable.discussion_ic_unread_replies_dot),
                            contentDescription = null
                        )
                    }
                    Text(
                        text = pluralStringResource(
                            id = R.plurals.discussion_missed_posts,
                            thread.unreadCommentCount,
                            thread.unreadCommentCount
                        ),
                        color = MaterialTheme.appColors.textPrimaryVariant,
                        style = MaterialTheme.appTypography.labelSmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = thread.title,
            style = MaterialTheme.appTypography.labelLarge,
            color = MaterialTheme.appColors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                id = R.string.discussion_last_post,
                TimeUtils.iso8601ToDateWithTime(context, thread.updatedAt)
            ),
            style = MaterialTheme.appTypography.labelSmall,
            color = MaterialTheme.appColors.textPrimaryVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        IconText(
            text = pluralStringResource(
                id = R.plurals.discussion_responses,
                thread.commentCount - 1,
                thread.commentCount - 1
            ),
            painter = painterResource(id = R.drawable.discussion_ic_responses),
            color = MaterialTheme.appColors.textPrimary,
            textStyle = MaterialTheme.appTypography.labelLarge
        )
    }
}

@Composable
fun ThreadItemCategory(
    name: String,
    painterResource: Painter,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.then(
            Modifier
                .border(
                    1.dp,
                    MaterialTheme.appColors.cardViewBorder,
                    MaterialTheme.appShapes.cardShape
                )
                .clip(MaterialTheme.appShapes.cardShape)
                .clickable { onClick() }
        ),
        shape = MaterialTheme.appShapes.cardShape,
        backgroundColor = MaterialTheme.appColors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource,
                contentDescription = null,
                tint = MaterialTheme.appColors.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            AutoSizeText(
                text = name,
                style = MaterialTheme.appTypography.bodyMedium,
                color = MaterialTheme.appColors.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TopicItem(
    topic: Topic,
    onClick: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(topic.id, topic.name) }
            .padding(horizontal = 8.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = topic.name,
            style = MaterialTheme.appTypography.titleMedium,
            color = MaterialTheme.appColors.textPrimary
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            tint = MaterialTheme.appColors.primary,
            contentDescription = "Expandable Arrow"
        )
    }
}

@Preview
@Composable
private fun TopicItemPreview() {
    OpenEdXTheme {
        TopicItem(
            topic = mockTopic,
            onClick = { _, _ -> }
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ThreadItemPreview() {
    OpenEdXTheme {
        ThreadItem(
            thread = mockThread,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun CommentItemPreview() {
    OpenEdXTheme {
        CommentItem(
            modifier = Modifier.fillMaxWidth(),
            comment = mockComment,
            onClick = { _, _, _ -> },
            onUserPhotoClick = {}
        )
    }
}

@Preview
@Composable
private fun ThreadMainItemPreview() {
    ThreadMainItem(
        modifier = Modifier.fillMaxWidth(),
        thread = mockThread,
        onClick = { _, _ -> },
        onUserPhotoClick = {}
    )
}

private val mockComment = DiscussionComment(
    "",
    "",
    "",
    "",
    "",
    "",
    "",
    TextConverter.textToLinkedImageText(""),
    false,
    true,
    20,
    emptyList(),
    false,
    "",
    "",
    false,
    "",
    "",
    "",
    21,
    emptyList(),
    ProfileImage("", "", "", "", false),
    mapOf()
)

private val mockThread = org.openedx.discussion.domain.model.Thread(
    "",
    "",
    "",
    "",
    "",
    "",
    "",
    TextConverter.textToLinkedImageText(""),
    false,
    true,
    20,
    emptyList(),
    false,
    "",
    "",
    "",
    "",
    DiscussionType.DISCUSSION,
    "",
    "",
    "Discussion title long Discussion title long good item",
    true,
    false,
    true,
    21,
    4,
    false,
    false,
    mapOf(),
    10,
    false,
    false
)

private val mockTopic = Topic(
    id = "",
    name = "All Topics",
    threadListUrl = "",
    children = emptyList()
)
