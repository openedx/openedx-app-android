package com.raccoongang.core.extension

import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.isInternetError(): Boolean {
    return this is SocketTimeoutException || this is UnknownHostException
}