package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.online.repository.FirebaseManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoogleSignInResult(
    val uid: String,
    val displayName: String,
    val email: String?
)

object GoogleAuthHelper {

    fun getWebClientId(context: Context): String? {
        // 1. Try reading generated resource from google-services.json
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            try {
                val value = context.getString(resId).trim()
                if (value.isNotBlank() && !value.contains("dummy", ignoreCase = true)) {
                    return value
                }
            } catch (_: Throwable) { }
        }

        // 2. Custom string if user added it
        val customResId = context.resources.getIdentifier("google_web_client_id", "string", context.packageName)
        if (customResId != 0) {
            try {
                val value = context.getString(customResId).trim()
                if (value.isNotBlank()) return value
            } catch (_: Throwable) { }
        }

        return null
    }

    suspend fun signInWithGoogle(context: Context): Result<GoogleSignInResult> = withContext(Dispatchers.IO) {
        try {
            val serverClientId = getWebClientId(context)
            if (serverClientId.isNullOrBlank()) {
                return@withContext Result.failure(
                    Exception("Google Sign-In is not configured with a Web Client ID in Firebase. Please tap 'CONTINUE AS GUEST' to play now!")
                )
            }

            val credentialManager = CredentialManager.create(context)

            val googleIdOption = try {
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            } catch (t: Throwable) {
                return@withContext Result.failure(
                    Exception("Google Sign-In configuration error: ${t.localizedMessage ?: "Unknown error"}")
                )
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = try {
                credentialManager.getCredential(context = context, request = request)
            } catch (e: GetCredentialCancellationException) {
                return@withContext Result.failure(Exception("Sign-in was cancelled."))
            } catch (e: GetCredentialException) {
                val rawMsg = e.localizedMessage ?: "Google Sign-In failed"
                val friendlyMessage = if (rawMsg.contains("28444") || rawMsg.contains("Developer console", ignoreCase = true)) {
                    "Google Sign-In is not linked in Firebase Console. Tap 'CONTINUE AS GUEST' to play now, or configure your SHA-1 and Web Client ID in Firebase."
                } else if (rawMsg.contains("cancel", ignoreCase = true)) {
                    "Sign-in was cancelled."
                } else {
                    rawMsg
                }
                return@withContext Result.failure(Exception(friendlyMessage))
            }

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@")
                val email = googleIdTokenCredential.id

                // Attempt to link or sign in with Firebase
                val firebaseResult = FirebaseManager.linkOrSignInWithGoogle(idToken)
                val finalUid = firebaseResult.getOrNull() ?: ("google_" + email.hashCode().toString())

                return@withContext Result.success(
                    GoogleSignInResult(
                        uid = finalUid,
                        displayName = displayName,
                        email = email
                    )
                )
            } else {
                return@withContext Result.failure(Exception("Unrecognized credential type."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
