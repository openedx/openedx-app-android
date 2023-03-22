package com.raccoongang.core

import java.util.regex.Pattern

class Validator {

    fun isEmailValid(email: String): Boolean {
        val validEmailAddressRegex =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE)
        val matcher = validEmailAddressRegex.matcher(email)
        return matcher.find()
    }

    fun isPasswordValid(password: String): Boolean {
        return password.length >= 2
    }

}