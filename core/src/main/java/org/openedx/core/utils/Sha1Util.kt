package org.openedx.core.utils

import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Sha1Util {

    fun SHA1(text: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            md.update(text.toByteArray(charset("iso-8859-1")), 0, text.length)
            val sha1hash = md.digest()
            convertToHex(sha1hash)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            text
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            text
        }
    }

    @Suppress("MagicNumber")
    fun convertToHex(data: ByteArray): String {
        val buf = StringBuilder()
        for (b in data) {
            var halfbyte = b.toInt() ushr 4 and 0x0F
            var twoHalfs = 0
            do {
                buf.append(
                    if (halfbyte in 0..9) {
                        ('0'.code + halfbyte).toChar()
                    } else {
                        ('a'.code + (halfbyte - 10)).toChar()
                    }
                )
                halfbyte = b.toInt() and 0x0F
            } while (twoHalfs++ < 1)
        }
        return buf.toString()
    }
}
