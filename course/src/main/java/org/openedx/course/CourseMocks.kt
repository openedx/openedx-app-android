package org.openedx.course

import org.openedx.core.BlockType
import org.openedx.core.data.model.DateType
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.DatesSection
import org.openedx.core.domain.model.Progress
import java.util.Date

object CourseMocks {

    val sequentialBlock: Block = Block(
        id = "sequential-id",
        blockId = "sequential-id",
        lmsWebUrl = "lmsWebUrl",
        legacyWebUrl = "legacyWebUrl",
        studentViewUrl = "studentViewUrl",
        type = BlockType.SEQUENTIAL,
        displayName = "Sequential",
        graded = false,
        studentViewData = null,
        studentViewMultiDevice = false,
        blockCounts = BlockCounts(1),
        descendants = emptyList(),
        descendantsType = BlockType.SEQUENTIAL,
        completion = 0.0,
        containsGatedContent = false,
        assignmentProgress = AssignmentProgress(
            assignmentType = "Homework",
            numPointsEarned = 1f,
            numPointsPossible = 3f,
            shortLabel = "HM1"
        ),
        due = Date(),
        offlineDownload = null
    )

    val coursewareAccess = CoursewareAccess(
        hasAccess = true,
        errorCode = "",
        developerMessage = "",
        userMessage = "",
        additionalContextUserMessage = "",
        userFragment = ""
    )

    val progress: Progress = Progress.DEFAULT_PROGRESS

    val assignmentProgress = Progress(1, 3)
    val assignmentProgressTablet = Progress(2, 3)

    val courseDateBlock = CourseDateBlock(
        complete = false,
        date = Date(),
        dateType = DateType.TODAY_DATE,
        description = "Mocked Course Date Description"
    )

    val courseDateBlocks = linkedMapOf(
        Pair(DatesSection.COMPLETED, listOf(courseDateBlock, courseDateBlock)),
        Pair(DatesSection.PAST_DUE, listOf(courseDateBlock, courseDateBlock)),
        Pair(DatesSection.TODAY, listOf(courseDateBlock, courseDateBlock))
    )

    val courseDatesBannerInfoWithData = CourseDatesBannerInfo(
        missedDeadlines = true,
        missedGatedContent = false,
        verifiedUpgradeLink = "",
        contentTypeGatingEnabled = false,
        hasEnded = true,
    )

    val courseDatesResultWithData = CourseDatesResult(
        datesSection = courseDateBlocks,
        courseBanner = courseDatesBannerInfoWithData,
    )
}
