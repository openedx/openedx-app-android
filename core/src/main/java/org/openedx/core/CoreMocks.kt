package org.openedx.core

import org.openedx.core.data.model.User
import org.openedx.core.data.model.room.VideoProgressEntity
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.AssignmentProgress
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseAccessDetails
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesCalendarSync
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseInfoOverview
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.DownloadCoursePreview
import org.openedx.core.domain.model.EncodedVideos
import org.openedx.core.domain.model.EnrollmentDetails
import org.openedx.core.domain.model.Progress
import org.openedx.core.domain.model.ResetCourseDates
import org.openedx.core.domain.model.StudentViewData
import org.openedx.core.domain.model.VideoInfo
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.module.db.FileType
import org.openedx.core.module.download.DownloadModelsSize
import java.util.Date

object CoreMocks {
    val mockAssignmentProgress = AssignmentProgress(
        assignmentType = "Home",
        numPointsEarned = 1f,
        numPointsPossible = 3f,
        shortLabel = "HM1"
    )

    val mockUser = User(
        id = 0,
        username = "",
        email = "",
        name = ""
    )

    val mockAppConfig = AppConfig(
        courseDatesCalendarSync = CourseDatesCalendarSync(
            isEnabled = true,
            isSelfPacedEnabled = true,
            isInstructorPacedEnabled = true,
            isDeepLinkEnabled = false,
        )
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
        descendants = listOf("1"),
        descendantsType = BlockType.CHAPTER,
        completion = 0.0,
        containsGatedContent = false,
        assignmentProgress = mockAssignmentProgress,
        due = Date(),
        offlineDownload = null
    )

    val mockBlockData = listOf(
        mockChapterBlock.copy(
            id = "id",
            type = BlockType.HTML,
            blockCounts = BlockCounts(0),
            descendants = listOf("id2", "id1"),
            descendantsType = BlockType.HTML,
            assignmentProgress = mockAssignmentProgress.copy(
                assignmentType = "Homework",
                shortLabel = "HW1"
            ),
            due = Date()
        ),
        mockChapterBlock.copy(
            id = "id1",
            type = BlockType.VERTICAL,
            blockCounts = BlockCounts(0),
            descendants = listOf("id2", "id"),
            descendantsType = BlockType.HTML,
            assignmentProgress = mockAssignmentProgress.copy(
                assignmentType = "Homework",
                shortLabel = "HW1"
            ),
            due = Date()
        ),
        mockChapterBlock.copy(
            id = "id2",
            type = BlockType.SEQUENTIAL,
            blockCounts = BlockCounts(0),
            descendants = emptyList(),
            descendantsType = BlockType.HTML,
            assignmentProgress = mockAssignmentProgress.copy(
                assignmentType = "Homework",
                shortLabel = "HW1"
            ),
            due = Date()
        ),
        mockChapterBlock.copy(
            id = "id3",
            type = BlockType.HTML,
            blockCounts = BlockCounts(0),
            descendants = emptyList(),
            descendantsType = BlockType.HTML,
            assignmentProgress = mockAssignmentProgress.copy(
                assignmentType = "Homework",
                shortLabel = "HW1"
            ),
            due = Date()
        )
    )

    val mockCourseStructure = CourseStructure(
        root = "",
        blockData = mockBlockData,
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

    val mockCoursewareAccess = CoursewareAccess(
        hasAccess = true,
        errorCode = "",
        developerMessage = "",
        userMessage = "",
        userFragment = "",
        additionalContextUserMessage = ""
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

    val mockCourseAccessDetails = CourseAccessDetails(
        hasUnmetPrerequisites = false,
        isTooEarly = false,
        isStaff = false,
        auditAccessExpires = null,
        coursewareAccess = mockCoursewareAccess
    )

    val mockEnrollmentDetails = EnrollmentDetails(
        created = Date(),
        mode = "audit",
        isActive = true,
        upgradeDeadline = Date()
    )

    val mockCourseInfoOverview = CourseInfoOverview(
        name = "Open edX Demo Course",
        number = "DemoX",
        org = "edX",
        start = Date(),
        startDisplay = "Today",
        startType = "",
        end = null,
        isSelfPaced = false,
        media = null,
        courseSharingUtmParameters = CourseSharingUtmParameters("", ""),
        courseAbout = "About course"
    )

    val mockCourseEnrollmentDetails = CourseEnrollmentDetails(
        id = "course-id",
        courseUpdates = "Course updates",
        courseHandouts = "Course handouts",
        discussionUrl = "https://example.com/discussion",
        courseAccessDetails = mockCourseAccessDetails,
        certificate = null,
        enrollmentDetails = mockEnrollmentDetails,
        courseInfoOverview = mockCourseInfoOverview
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

    val coursePreview = DownloadCoursePreview(
        id = "course-id",
        name = "Preview Course",
        image = "",
        totalSize = 100L
    )

    val mockDownloadModelsSize = DownloadModelsSize(
        isAllBlocksDownloadedOrDownloading = false,
        remainingCount = 0,
        remainingSize = 0,
        allCount = 1,
        allSize = 0
    )
}
