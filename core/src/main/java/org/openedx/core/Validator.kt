package org.openedx.core

import java.util.regex.Pattern

class Validator {

    fun isEmailOrUserNameValid(input: String): Boolean {
        return if (input.contains("@")) {
            val validEmailAddressRegex = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
                Pattern.CASE_INSENSITIVE
            )
            validEmailAddressRegex.matcher(input).find()
        } else {
            input.isNotBlank() && input.contains(" ").not()
        }
    }

    fun isPasswordValid(password: String): Boolean {
        return password.length >= 2
    }
}
