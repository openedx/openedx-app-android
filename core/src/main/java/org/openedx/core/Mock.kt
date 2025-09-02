package org.openedx.core

import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.OfflineDownload
import org.openedx.core.domain.model.Progress
import java.util.Date

object Mock {
    private val mockAssignmentProgress = AssignmentProgress(
        assignmentType = "Home",
        numPointsEarned = 1f,
        numPointsPossible = 3f,
        shortLabel = "HM1"
    )
    val mockChapterBlock = Block(
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
        assignmentProgress = mockAssignmentProgress,
        due = Date(),
        offlineDownload = null
    )
    private val mockSequentialBlock = Block(
        id = "id",
        blockId = "blockId",
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
        descendantsType = BlockType.CHAPTER,
        completion = 0.0,
        containsGatedContent = false,
        assignmentProgress = mockAssignmentProgress,
        due = Date(),
        offlineDownload = OfflineDownload("fileUrl", "", 1),
    )

    val mockCourseStructure = CourseStructure(
        root = "",
        blockData = listOf(mockSequentialBlock, mockSequentialBlock),
        id = "id",
        name = "Course name",
        number = "",
        org = "Org",
        start = Date(),
        startDisplay = "",
        startType = "",
        end = Date(),
        coursewareAccess = CoursewareAccess(
            true,
            "",
            "",
            "",
            "",
            ""
        ),
        media = null,
        certificate = null,
        isSelfPaced = false,
        progress = Progress(1, 3),
    )
}
