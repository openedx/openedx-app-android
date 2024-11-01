package org.openedx.auth.presentation.sso

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.suspendCancellableCoroutine
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.domain.model.SocialAuthResponse
import org.openedx.core.ApiConstants
import org.openedx.core.utils.Logger
import org.openedx.foundation.extension.safeResume
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FacebookAuthHelper {

    private val logger = Logger(TAG)
    private val callbackManager = CallbackManager.Factory.create()

    suspend fun socialAuth(fragment: Fragment): SocialAuthResponse? =
        suspendCancellableCoroutine { continuation ->
            LoginManager.getInstance().registerCallback(
                callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onCancel() {
                        logger.d { "Facebook auth canceled" }
                        continuation.resume(SocialAuthResponse())
                    }

                    override fun onError(error: FacebookException) {
                        logger.e { "Facebook auth error: $error" }
                        continuation.resumeWithException(error)
                    }

                    override fun onSuccess(result: LoginResult) {
                        logger.d { "Facebook auth success" }
                        GraphRequest.newMeRequest(result.accessToken) { obj, response ->
                            if (response?.error != null) {
                                continuation.cancel()
                            } else {
                                continuation.safeResume(
                                    SocialAuthResponse(
                                        accessToken = result.accessToken.token,
                                        name = obj?.optString(ApiConstants.NAME).orEmpty(),
                                        email = obj?.optString(ApiConstants.EMAIL).orEmpty(),
                                        authType = AuthType.FACEBOOK,
                                    )
                                ) {
                                    continuation.cancel()
                                }
                            }
                        }.also {
                            it.parameters = Bundle().apply {
                                putString("fields", "${ApiConstants.NAME}, ${ApiConstants.EMAIL}")
                            }
                            it.executeAsync()
                        }
                    }
                }
            )
            LoginManager.getInstance().logOut()
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
