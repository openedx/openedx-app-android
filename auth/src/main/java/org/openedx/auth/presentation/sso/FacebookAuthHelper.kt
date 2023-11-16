package org.openedx.auth.presentation.sso

import androidx.fragment.app.Fragment
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.suspendCancellableCoroutine
import org.openedx.core.utils.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FacebookAuthHelper {

    private val logger = Logger(TAG)
    private val callbackManager = CallbackManager.Factory.create()

    suspend fun signIn(fragment: Fragment): String? = suspendCancellableCoroutine { continuation ->
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onCancel() {
                    logger.d { "Facebook login canceled" }
                    continuation.cancel()
                }

                override fun onError(error: FacebookException) {
                    logger.e { "Facebook login error: $error" }
                    continuation.resumeWithException(error)
                }

                override fun onSuccess(result: LoginResult) {
                    logger.d { "Facebook login success" }
                    continuation.resume(result.accessToken.token)
                }
            }
        )
        LoginManager.getInstance().logInWithReadPermissions(
            fragment,
            callbackManager,
            PERMISSIONS_LIST
        )
    }

    fun clear() {
        runCatching {
            LoginManager.getInstance().unregisterCallback(callbackManager)
        }
    }

    private companion object {
        const val TAG = "FacebookAuthHelper"
        val PERMISSIONS_LIST = listOf("email", "public_profile")
    }
}
