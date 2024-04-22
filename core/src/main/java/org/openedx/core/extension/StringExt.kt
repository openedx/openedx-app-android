package org.openedx.core.extension

import android.util.Patterns
import java.util.Locale
import java.util.regex.Pattern


fun String.isEmailValid(): Boolean {
    val regex =
        "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$"
    return Pattern.compile(regex).matcher(this).matches()
}

fun String.isLinkValid() = Patterns.WEB_URL.matcher(this).matches()

fun String.replaceLinkTags(isDarkTheme: Boolean): String {
    val linkColor = if (isDarkTheme) "879FF5" else "0000EE"
    var text = ("<html><head>"
            + "<style type=\"text/css\">body{color:" + "#424242" + ";}" +
            " a{color:#$linkColor; text-decoration:none;}"
            + "</style></head>"
            + "<body>" + this) + "</body></html>"
    var str: String
    while (text.indexOf("\u0082") > 0) {
        if (text.indexOf("\u0082") > 0 && text.indexOf("\u0083") > 0) {
            str = text.substring(text.indexOf("\u0082") + 1, text.indexOf("\u0083"))
            text = text.replace(("\u0082" + str + "\u0083").toRegex(), "<a href=\"$str\">$str</a>")
        }
    }
    return text
}

fun String.replaceSpace(target: String = ""): String = this.replace(" ", target)

fun String.tagId(): String = this.replaceSpace("_").lowercase(Locale.getDefault())

fun String.takeIfNotEmpty(): String? {
    return if (this.isEmpty().not()) this else null
}
