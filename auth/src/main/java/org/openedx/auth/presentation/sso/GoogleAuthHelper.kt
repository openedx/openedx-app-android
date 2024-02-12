package org.openedx.auth.presentation.sso

import android.accounts.Account
import android.app.Activity
import android.credentials.GetCredentialException
import android.os.Bundle
import androidx.annotation.WorkerThread
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import org.openedx.auth.data.model.AuthType
import org.openedx.auth.domain.model.SocialAuthResponse
import org.openedx.core.config.Config
import org.openedx.core.utils.Logger

class GoogleAuthHelper(private val config: Config) {

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
                GetSignInWithGoogleOption.Builder(config.getGoogleConfig().clientId).build()
            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext,
            )
            getGoogleIdToken(result.credential)
        }.onFailure {
            if (it is GetCredentialCancellationException &&
                it.type == GetCredentialException.TYPE_USER_CANCELED
            ) {
                return null
            }
            logger.e { "GetCredentials error: ${it.message}" }
        }.getOrNull()
    }

    private fun getGoogleIdToken(credential: Credential): GoogleIdTokenCredential? {
        return when (credential) {
            is GoogleIdTokenCredential -> {
                parseToken(credential.data)
            }

            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    parseToken(credential.data)
                } else {
                    null
                }
            }

            else -> {
                logger.e { "Unknown credential type" }
                null
            }
        }
    }

    private fun parseToken(data: Bundle): GoogleIdTokenCredential? =
        try {
            GoogleIdTokenCredential.createFrom(data)
        } catch (e: GoogleIdTokenParsingException) {
            logger.e { "Token parsing exception: $e" }
            null
        }

    @WorkerThread
    suspend fun socialAuth(activityContext: Activity): SocialAuthResponse? {
        return getCredentials(activityContext)?.let { credentials ->
            if (credentials.id.isNotBlank()) {
                val token = getAuthToken(activityContext, credentials.id).orEmpty()
                logger.d { token }
                SocialAuthResponse(
                    accessToken = token,
                    name = credentials.displayName.orEmpty(),
                    email = credentials.id,
                    authType = AuthType.GOOGLE,
                )
            } else {
                return null
            }
        }
    }

    private companion object {
        const val TAG = "GoogleAuthHelper"
        const val SCOPE = "oauth2: https://www.googleapis.com/auth/userinfo.email"
    }
}
