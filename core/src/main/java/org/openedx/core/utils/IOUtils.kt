package org.openedx.core.utils

import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

object IOUtils {

    fun toString(file: File, charset: Charset): String {
        return file.source().buffer().readString(charset)
    }

    fun copy(input: InputStream, out: OutputStream) {
        out.sink().buffer().writeAll(input.source())
    }
}
