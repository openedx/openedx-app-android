package org.openedx.core

import java.util.regex.Pattern

class Validator {

    fun isEmailOrUserNameValid(email: String): Boolean {
        return if (email.contains("@")) {
            val validEmailAddressRegex = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE
            )
            validEmailAddressRegex.matcher(email).find()
        } else {
            email.isBlank().not()
        }
    }

    fun isPasswordValid(password: String): Boolean {
        return password.length >= 2
    }

}
