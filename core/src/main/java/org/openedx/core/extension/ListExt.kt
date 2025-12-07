package org.openedx.core.extension

import org.openedx.core.BlockType
import org.openedx.core.domain.model.Block

fun List<Block>.getVerticalBlocks(): List<Block> {
    return this.filter { it.type == BlockType.VERTICAL }
}

fun List<Block>.getSequentialBlocks(): List<Block> {
    return this.filter { it.type == BlockType.SEQUENTIAL }
}

fun List<Block>.getChapterBlocks(): List<Block> {
    return this.filter { it.type == BlockType.CHAPTER }
}

fun List<Block>.getUnitChapter(blockId: String): Block? {
    val verticalBlock = this.firstOrNull {
        it.type == BlockType.VERTICAL && it.descendants.contains(blockId)
    }

    val sequentialBlock = verticalBlock?.let { vertical ->
        this.firstOrNull {
            it.type == BlockType.SEQUENTIAL && it.descendants.contains(vertical.id)
        }
    }

    return sequentialBlock?.let { sequential ->
        this.firstOrNull {
            it.type == BlockType.CHAPTER && it.descendants.contains(sequential.id)
        }
    }
}
