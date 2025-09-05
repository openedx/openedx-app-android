package org.openedx.core

import org.openedx.core.data.model.room.VideoProgressEntity
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.EncodedVideos
import org.openedx.core.domain.model.OfflineDownload
import org.openedx.core.domain.model.Progress
import org.openedx.core.domain.model.ResetCourseDates
import org.openedx.core.domain.model.StudentViewData
import org.openedx.core.domain.model.VideoInfo
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
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

    val mockCourseComponentStatus = CourseComponentStatus(
        lastVisitedBlockId = "video1"
    )

    val mockCourseDatesBannerInfo = CourseDatesBannerInfo(
        missedDeadlines = false,
        missedGatedContent = false,
        contentTypeGatingEnabled = false,
        verifiedUpgradeLink = "",
        hasEnded = false
    )

    val mockCourseDatesResult = CourseDatesResult(
        datesSection = linkedMapOf(),
        courseBanner = mockCourseDatesBannerInfo
    )

    val mockCourseProgress = CourseProgress(
        verifiedMode = "audit",
        accessExpiration = "",
        certificateData = null,
        completionSummary = null,
        courseGrade = null,
        creditCourseRequirements = "",
        end = "",
        enrollmentMode = "audit",
        gradingPolicy = null,
        hasScheduledContent = false,
        sectionScores = emptyList(),
        studioUrl = "",
        username = "testuser",
        userHasPassingGrade = false,
        verificationData = null,
        disableProgressGraph = false
    )

    val mockVideoProgress = VideoProgressEntity(
        blockId = "video1",
        videoUrl = "test-video-url",
        videoTime = 1000L,
        duration = 5000L
    )

    val mockResetCourseDates = ResetCourseDates(
        message = "Dates reset successfully",
        body = "Your course dates have been reset",
        header = "Success",
        link = "",
        linkText = ""
    )

    val mockDownloadModel = DownloadModel(
        id = "video1",
        title = "Video 1",
        courseId = "test-course-id",
        size = 1000L,
        path = "/test/path/video1",
        url = "test-url",
        type = FileType.VIDEO,
        downloadedState = DownloadedState.NOT_DOWNLOADED,
        lastModified = null
    )

    val mockVideoBlock = Block(
        id = "video1",
        blockId = "video1",
        lmsWebUrl = "lmsWebUrl",
        legacyWebUrl = "legacyWebUrl",
        studentViewUrl = "studentViewUrl",
        type = BlockType.VIDEO,
        displayName = "Video 1",
        graded = false,
        studentViewData = StudentViewData(
            onlyOnWeb = false,
            duration = "",
            transcripts = null,
            encodedVideos = EncodedVideos(
                youtube = null,
                hls = null,
                fallback = null,
                desktopMp4 = null,
                mobileHigh = null,
                mobileLow = VideoInfo(
                    url = "test-url",
                    fileSize = 1000L
                )
            ),
            topicId = ""
        ),
        studentViewMultiDevice = false,
        blockCounts = BlockCounts(0),
        descendants = emptyList(),
        descendantsType = BlockType.VIDEO,
        completion = 0.0,
        containsGatedContent = false,
        assignmentProgress = null,
        due = null,
        offlineDownload = null,
    )

    val mockSequentialBlockForDownload = Block(
        id = "sequential1",
        blockId = "sequential1",
        lmsWebUrl = "lmsWebUrl",
        legacyWebUrl = "legacyWebUrl",
        studentViewUrl = "studentViewUrl",
        type = BlockType.SEQUENTIAL,
        displayName = "Sequential 1",
        graded = false,
        studentViewData = null,
        studentViewMultiDevice = false,
        blockCounts = BlockCounts(0),
        descendants = listOf("vertical1"),
        descendantsType = BlockType.VERTICAL,
        completion = 0.0,
        containsGatedContent = false,
        assignmentProgress = null,
        due = null,
        offlineDownload = null,
    )

    val mockVerticalBlock = Block(
        id = "vertical1",
        blockId = "vertical1",
        lmsWebUrl = "lmsWebUrl",
        legacyWebUrl = "legacyWebUrl",
        studentViewUrl = "studentViewUrl",
        type = BlockType.VERTICAL,
        displayName = "Vertical 1",
        graded = false,
        studentViewData = null,
        studentViewMultiDevice = false,
        blockCounts = BlockCounts(0),
        descendants = listOf("video1"),
        descendantsType = BlockType.VIDEO,
        completion = 0.0,
        containsGatedContent = false,
        assignmentProgress = null,
        due = null,
        offlineDownload = null,
    )

    val mockCourseStructureForDownload = CourseStructure(
        root = "sequential1",
        blockData = listOf(mockSequentialBlockForDownload, mockVerticalBlock, mockVideoBlock),
        id = "test-course-id",
        name = "Test Course",
        number = "CS101",
        org = "TestOrg",
        start = Date(),
        startDisplay = "2024-01-01",
        startType = "timestamped",
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
        progress = null
    )
}
