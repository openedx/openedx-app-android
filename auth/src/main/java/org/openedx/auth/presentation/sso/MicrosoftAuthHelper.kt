package org.openedx.auth.presentation.sso

import android.app.Activity
import androidx.annotation.WorkerThread
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.domain.model.SocialAuthResponse
import org.openedx.core.ApiConstants
import org.openedx.core.R
import org.openedx.core.utils.Logger
import org.openedx.foundation.extension.safeResume
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MicrosoftAuthHelper {
    private val logger = Logger(TAG)

    @WorkerThread
    suspend fun socialAuth(activityContext: Activity): SocialAuthResponse? =
        suspendCancellableCoroutine { continuation ->
            val clientApplication =
                PublicClientApplication.createMultipleAccountPublicClientApplication(
                    activityContext,
                    R.raw.microsoft_auth_config
                )
            val params = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activityContext)
                .withScopes(SCOPES)
                .withCallback(object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                        val claims = authenticationResult?.account?.claims
                        val name =
                            (claims?.getOrDefault(ApiConstants.NAME, "") as? String)
                                .orEmpty()
                        val email =
                            (claims?.getOrDefault(ApiConstants.EMAIL, "") as? String)
                                .orEmpty()
                        continuation.safeResume(
                            SocialAuthResponse(
                                accessToken = authenticationResult?.accessToken.orEmpty(),
                                name = name,
                                email = email,
                                authType = AuthType.MICROSOFT,
                            )
                        ) {
                            continuation.cancel()
                        }
                    }

                    override fun onError(exception: MsalException) {
                        logger.e { "Microsoft auth error: $exception" }
                        continuation.resumeWithException(exception)
                    }

                    override fun onCancel() {
                        logger.d { "Microsoft auth canceled" }
                        continuation.resume(SocialAuthResponse())
                    }
                }).build()
            clientApplication.accounts.forEach {
                clientApplication.removeAccount(it)
            }
            clientApplication.acquireToken(params)
        }

    private companion object {
        const val TAG = "MicrosoftAuthHelper"
        val SCOPES = listOf("User.Read", ApiConstants.EMAIL)
    }
}
