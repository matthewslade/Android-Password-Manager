package com.sladematthew.apm

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.util.Log
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


class AuthRepository(context: Context) {
    private val scopes = listOf(Scope(DriveScopes.DRIVE_FILE))
    private val authorizationRequest = AuthorizationRequest.builder().setRequestedScopes(scopes).build()

    private val oneTap = Identity.getSignInClient(context)
    private val signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
        BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true)
            .setServerClientId(
                "953212827275-pe30n6gpq1ku4e73cadktq2sup4qetv1.apps.googleusercontent.com"
            ).setFilterByAuthorizedAccounts(false).build()
    ).setAutoSelectEnabled(true).build()

    fun observeUserStatus(): Flow<UserInfoResult?> {
        return callbackFlow {
            val authListener = FirebaseAuth.AuthStateListener {
                val currentUser = it.currentUser
                if (currentUser != null ){
                    trySend(UserInfoResult(currentUser.email!!))
                }else{
                    trySend(null)
                }
            }
            firebaseAuth.addAuthStateListener(authListener)
            awaitClose {
                firebaseAuth.removeAuthStateListener(authListener)
            }
        }
    }


    private val authorize = Identity.getAuthorizationClient(context)

    private val firebaseAuth = Firebase.auth

    private val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))

    suspend fun signInGoogle(): IntentSender {
        return oneTap.beginSignIn(signInRequest).await().pendingIntent.intentSender
    }

    suspend fun signOut() {
        oneTap.signOut().await()
        firebaseAuth.signOut()
    }

    suspend fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun getUserInfo(): UserInfoResult? {
        val currentUser = firebaseAuth.currentUser
        return if (currentUser!=null){
            UserInfoResult(currentUser.email!!)
        }else{
            null
        }
    }

    suspend fun getGoogleDrive(): Drive? {
        val currentUser = firebaseAuth.currentUser
        return if (currentUser!=null){
            credential.selectedAccount = Account(currentUser.email,"google.com")
            Drive.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(),
                credential
            ).build()
        }else{
            Log.e("APM","current user is null")
            null
        }
    }

    suspend fun authorizeGoogleDrive(): AuthorizationResult {
        return authorize.authorize(authorizationRequest).await()
    }

    suspend fun authorizeGoogleDriveResult(intent: Intent): AuthorizationResult {
        return authorize.getAuthorizationResultFromIntent(intent)
    }

    suspend fun getSignInResult(intent: Intent): UserInfoResult {
        val credential = oneTap.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredential = GoogleAuthProvider.getCredential(googleIdToken,null)
        val authResult = firebaseAuth.signInWithCredential(googleCredential).await()
        return UserInfoResult(authResult.user!!.email!!)
    }
}


data class UserInfoResult(
    val email:String
)