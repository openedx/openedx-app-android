package org.openedx.core

enum class BlockType {
    CHAPTER {
        override fun isContainer() = true
    },
    COURSE {
        override fun isContainer() = true
    },
    DISCUSSION {
        override fun isContainer() = false
    },
    DRAG_AND_DROP_V2 {
        override fun isContainer() = false
    },
    HTML {
        override fun isContainer() = false
    },
    LTI_CONSUMER {
        override fun isContainer() = false
    },
    OPENASSESSMENT {
        override fun isContainer() = false
    },
    OTHERS {
        override fun isContainer() = false
    },
    PROBLEM {
        override fun isContainer() = false
    },
    SECTION {
        override fun isContainer() = true
    },
    SEQUENTIAL {
        override fun isContainer() = true
    },
    VERTICAL {
        override fun isContainer() = true
    },
    VIDEO {
        override fun isContainer() = false
    },
    WORD_CLOUD {
        override fun isContainer() = false
    },
    SURVEY {
        override fun isContainer() = false
    };

    abstract fun isContainer(): Boolean

    companion object {
        private const val PROBLEM_PRIORITY = 1
        private const val VIDEO_PRIORITY = 2
        private const val DISCUSSION_PRIORITY = 3
        private const val HTML_PRIORITY = 4

        fun getBlockType(type: String): BlockType {
            val actualType = if (type.contains("-")) {
                type.replace("-", "_")
            } else {
                type
            }
            return try {
                BlockType.valueOf(actualType.uppercase())
            } catch (e: Exception) {
                e.printStackTrace()
                OTHERS
            }
        }

        fun sortByPriority(blockTypes: List<BlockType>): List<BlockType> {
            val priorityMap = mapOf(
                PROBLEM to PROBLEM_PRIORITY,
                VIDEO to VIDEO_PRIORITY,
                DISCUSSION to DISCUSSION_PRIORITY,
                HTML to HTML_PRIORITY
            )
            val comparator = Comparator<BlockType> { blockType1, blockType2 ->
                val priority1 = priorityMap[blockType1] ?: Int.MAX_VALUE
                val priority2 = priorityMap[blockType2] ?: Int.MAX_VALUE
                priority1 - priority2
            }
            return blockTypes.sortedWith(comparator)
        }
    }
}
