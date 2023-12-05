package org.openedx.auth.presentation.sso

import android.app.Activity
import androidx.annotation.WorkerThread
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.openedx.core.R
import org.openedx.core.utils.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MicrosoftAuthHelper {
    private val logger = Logger(TAG)

    @WorkerThread
    suspend fun signIn(activityContext: Activity): String? =
        suspendCancellableCoroutine { continuation ->
            val clientApplication =
                PublicClientApplication.createSingleAccountPublicClientApplication(
                    activityContext,
                    R.raw.microsoft_auth_config
                )
            val params = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activityContext)
                .withScopes(SCOPES)
                .withCallback(object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                        continuation.resume(authenticationResult?.accessToken)
                    }

                    override fun onError(exception: MsalException) {
                        logger.e { "Microsoft auth error: $exception" }
                        continuation.resumeWithException(exception)
                    }

                    override fun onCancel() {
                        logger.d { "Microsoft auth canceled" }
                        continuation.resume("")
                    }
                }).build()
            clientApplication.acquireToken(params)
        }

    private companion object {
        const val TAG = "MicrosoftAuthHelper"
        val SCOPES = listOf("User.Read")
    }
}
