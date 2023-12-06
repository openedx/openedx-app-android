package org.openedx.core.domain.model

import org.openedx.core.BlockType
import java.util.Date

data class CourseStructure(
    val root: String,
    val blockData: List<Block>,
    val id: String,
    val name: String,
    val number: String,
    val org: String,
    val start: Date?,
    val startDisplay: String,
    val startType: String,
    val end: Date?,
    val coursewareAccess: CoursewareAccess,
    val media: Media?,
    val certificate: Certificate?,
    val isSelfPaced: Boolean
) {
    val getVerticalBlocks: List<Block>
        get() = blockData.getVerticalBlocks()

    val getSequentialBlocks: List<Block>
        get() = blockData.getSequentialBlocks()
}

fun List<Block>.getVerticalBlocks(): List<Block> {
    return this.filter { it.type == BlockType.VERTICAL }
}

fun List<Block>.getSequentialBlocks(): List<Block> {
    return this.filter { it.type == BlockType.SEQUENTIAL }
}
