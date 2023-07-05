package com.raccoongang.auth.presentation

interface AuthAnalytics {
    fun userLoginEvent(method: String)
    fun signUpClickedEvent()
    fun createAccountClickedEvent(provider: String)
    fun registrationSuccessEvent(provider: String)
    fun forgotPasswordClickedEvent()
    fun resetPasswordClickedEvent(success: Boolean)
    fun setUserIdForSession(userId: Long)
}