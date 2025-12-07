package org.openedx.course.presentation.section

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.domain.model.Block
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSectionChanged
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.unit.container.CourseViewMode
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class CourseSectionViewModel(
    val courseId: String,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: CourseNotifier,
    private val analytics: CourseAnalytics,
) : BaseViewModel() {

    private val _uiState = MutableLiveData<CourseSectionUIState>(CourseSectionUIState.Loading)
    val uiState: LiveData<CourseSectionUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    var mode = CourseViewMode.FULL

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is CourseSectionChanged) {
                    getBlocks(event.blockId, mode)
                }
            }
        }
    }

    fun getBlocks(blockId: String, mode: CourseViewMode) {
        _uiState.value = CourseSectionUIState.Loading
        viewModelScope.launch {
            try {
                val courseStructure = when (mode) {
                    CourseViewMode.FULL -> interactor.getCourseStructure(courseId)
                    CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos(courseId)
                }
                val blocks = courseStructure.blockData
                val newList = getDescendantBlocks(blocks, blockId)
                val sequentialBlock = getSequentialBlock(blocks, blockId)
                _uiState.value =
                    CourseSectionUIState.Blocks(
                        blocks = ArrayList(newList),
                        courseName = courseStructure.name,
                        sectionName = sequentialBlock.displayName
                    )
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    private fun getDescendantBlocks(blocks: List<Block>, id: String): List<Block> {
        val resultList = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        val selectedBlock = getSequentialBlock(blocks, id)
        for (descendant in selectedBlock.descendants) {
            val blockDescendant = blocks.find {
                it.id == descendant
            }
            if (blockDescendant != null) {
                if (blockDescendant.type == BlockType.VERTICAL) {
                    resultList.add(blockDescendant)
                }
            } else {
                continue
            }
        }
        return resultList
    }

    private fun getSequentialBlock(blocks: List<Block>, id: String): Block {
        return blocks.first {
            it.id == id
        }
    }

    fun verticalClickedEvent(blockId: String) {
        val currentState = uiState.value
        if (currentState is CourseSectionUIState.Blocks) {
            analytics.logEvent(
                event = CourseAnalyticsEvent.UNIT_DETAIL.eventName,
                params = buildMap {
                    put(CourseAnalyticsKey.NAME.key, CourseAnalyticsEvent.UNIT_DETAIL.biValue)
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                    put(CourseAnalyticsKey.CATEGORY.key, CourseAnalyticsKey.NAVIGATION.key)
                }
            )
        }
    }
}
