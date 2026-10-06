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

object GoogleAuthHelper {

    suspend fun signInWithGoogle(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(context)

            // Web client ID or server client ID for Google Sign In
            val serverClientId = "505221327115-auth.apps.googleusercontent.com"

            val googleIdOption = try {
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            } catch (_: Throwable) {
                null
            }

            if (googleIdOption == null) {
                return@withContext Result.failure(Exception("Google Sign-In configuration unavailable."))
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = try {
                credentialManager.getCredential(context = context, request = request)
            } catch (e: GetCredentialCancellationException) {
                return@withContext Result.failure(Exception("Sign-in was cancelled."))
            } catch (e: GetCredentialException) {
                return@withContext Result.failure(Exception(e.localizedMessage ?: "Google Sign-In failed."))
            }

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                return@withContext FirebaseManager.linkOrSignInWithGoogle(idToken)
            } else {
                return@withContext Result.failure(Exception("Unrecognized credential type."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
