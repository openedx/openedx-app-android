package org.openedx.core

enum class BlockType {
    CHAPTER{ override fun isContainer() = true },
    COURSE{ override fun isContainer() = true },
    DISCUSSION{ override fun isContainer() = false },
    DRAG_AND_DROP_V2{ override fun isContainer() = false },
    HTML{ override fun isContainer() = false },
    LTI_CONSUMER{ override fun isContainer() = false },
    OPENASSESSMENT{ override fun isContainer() = false },
    OTHERS{ override fun isContainer() = false },
    PROBLEM{ override fun isContainer() = false },
    SECTION{ override fun isContainer() = true },
    SEQUENTIAL{ override fun isContainer() = true },
    VERTICAL{ override fun isContainer() = true },
    VIDEO{ override fun isContainer() = false },
    WORD_CLOUD{ override fun isContainer() = false };

    abstract fun isContainer() : Boolean

    companion object{
        fun getBlockType(type: String): BlockType {
            val actualType = if (type.contains("-")){
                type.replace("-", "_")
            } else type
            return try {
                BlockType.valueOf(actualType.uppercase())
            } catch (e : Exception){
                BlockType.OTHERS
            }
        }
    }
}

