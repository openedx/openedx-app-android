package org.openedx.auth.presentation.sso

import android.accounts.Account
import android.app.Activity
import androidx.annotation.WorkerThread
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import org.openedx.core.BuildConfig
import org.openedx.core.utils.Logger

class GoogleAuthHelper {

    private val logger = Logger(TAG)

    private fun getAuthToken(activityContext: Activity, name: String): String? {
        return runCatching {
            GoogleAuthUtil.getToken(
                activityContext,
                Account(name, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE),
                SCOPE
            )
        }.getOrNull()
    }

    private suspend fun getCredentials(activityContext: Activity): GoogleIdTokenCredential? {
        return runCatching {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption =
                GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID).build()
            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext,
            )
            getGoogleIdToken(result.credential)
        }.onFailure {
            logger.e { "GetCredentials error: ${it.message}" }
        }.getOrNull()
    }

    private fun getGoogleIdToken(credential: Credential): GoogleIdTokenCredential? {
        return when (credential) {
            is GoogleIdTokenCredential -> {
                try {
                    GoogleIdTokenCredential.createFrom(credential.data)
                } catch (e: GoogleIdTokenParsingException) {
                    logger.e { "Token parsing exception: $e" }
                    null
                }
            }

            else -> {
                logger.e { "Unknown credential type" }
                null
            }
        }
    }

    @WorkerThread
    suspend fun signIn(activityContext: Activity): String? {
        val credentials = getCredentials(activityContext)
        return credentials?.id?.let {
            getAuthToken(activityContext, it)
        }
    }

    private companion object {
        const val TAG = "GoogleAuthHelper"
        const val SCOPE = "oauth2: https://www.googleapis.com/auth/userinfo.email"
    }
}
