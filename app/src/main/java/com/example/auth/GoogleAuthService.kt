package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

object GoogleAuthService {

  private const val TAG = "GoogleAuthService"

  fun isUserSignedIn(): Boolean = Firebase.auth.currentUser != null

  fun currentUserId(): String? = Firebase.auth.currentUser?.uid

  fun currentUserDisplayName(): String? = Firebase.auth.currentUser?.displayName

  fun currentUserEmail(): String? = Firebase.auth.currentUser?.email

  /**
   * Interactive Sign-In with Google using Jetpack CredentialManager.
   */
  suspend fun signInWithGoogle(
    activity: Activity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
  ) {
    val credentialManager = CredentialManager.create(activity)
    val clientId = try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

    // MANDATORY: Exactly one credential option per request for Android 16+ responsiveness
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        Log.d(TAG, "Google Sign-In succeeded for user: ${Firebase.auth.currentUser?.uid}")
        onSuccess()
      } else {
        onError("Unexpected credential type returned from Google")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onCancelled()
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      onError(e.localizedMessage ?: "Sign in failed")
    }
  }

  /**
   * Silent auto sign-in attempt on startup.
   */
  suspend fun attemptAutoSignIn(context: Context, onSuccess: () -> Unit, onUnauthenticated: () -> Unit) {
    if (Firebase.auth.currentUser != null) {
      onSuccess()
      return
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onUnauthenticated()
      return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    val credentialManager = CredentialManager.create(context)
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }

  /**
   * Signs out of Firebase Auth and clears CredentialManager local state.
   */
  suspend fun signOut(context: Context, onComplete: () -> Unit) {
    Firebase.auth.signOut()
    try {
      val credentialManager = CredentialManager.create(context)
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e(TAG, "Failed to clear credential state", e)
    } finally {
      onComplete()
    }
  }
}
