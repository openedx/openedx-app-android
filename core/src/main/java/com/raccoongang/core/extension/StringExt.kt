package com.raccoongang.core.extension

import android.util.Patterns
import java.util.regex.Pattern


fun String.isEmailValid(): Boolean {
    val regex =
        "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$"
    return Pattern.compile(regex).matcher(this).matches()
}

fun String.isLinkValid() = Patterns.WEB_URL.matcher(this).matches()
