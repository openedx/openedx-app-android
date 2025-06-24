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
