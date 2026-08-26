package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.firestore.FirebaseFirestoreManager
import com.example.data.local.TaskFlowDao
import com.example.data.model.SubscriptionTier
import com.example.data.model.TenantEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.UserWallet
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(
        val user: UserEntity,
        val firebaseUser: FirebaseUser? = null
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository(
    private val context: Context,
    private val dao: TaskFlowDao
) {
    private val tag = "AuthRepository"
    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.w(tag, "FirebaseAuth not configured: ${e.message}")
        null
    }

    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUserState = MutableStateFlow<AuthState>(AuthState.Idle)
    val currentUserState: Flow<AuthState> = _currentUserState.asStateFlow()

    init {
        checkCurrentSession()
    }

    private fun checkCurrentSession() {
        val fbUser = firebaseAuth?.currentUser
        if (fbUser != null) {
            val userEntity = UserEntity(
                userId = fbUser.uid,
                tenantId = "tenant_alpha",
                email = fbUser.email ?: "user@taskflow.pro",
                displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Utilisateur",
                role = UserRole.OWNER,
                avatarUrl = fbUser.photoUrl?.toString(),
                creditsBalance = 50,
                lastActiveAt = System.currentTimeMillis()
            )
            _currentUserState.value = AuthState.Authenticated(userEntity, fbUser)
        } else {
            // Default demo authenticated session
            val defaultUser = UserEntity(
                userId = "user_alex",
                tenantId = "tenant_alpha",
                email = "alexandre.founder@agencealpha.fr",
                displayName = "Alexandre Dupont",
                role = UserRole.OWNER,
                creditsBalance = 58,
                totalTasksCompleted = 18,
                totalTimeSavedMinutes = 720,
                totalSpentEuros = 145.0
            )
            _currentUserState.value = AuthState.Authenticated(defaultUser, null)
        }
    }

    suspend fun signInWithEmailPassword(email: String, pass: String): Result<UserEntity> {
        _currentUserState.value = AuthState.Loading
        return try {
            val auth = firebaseAuth
            if (auth != null) {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val fbUser = result.user ?: throw Exception("Utilisateur introuvable")
                val entity = syncUserWithLocalAndCloud(fbUser)
                _currentUserState.value = AuthState.Authenticated(entity, fbUser)
                Result.success(entity)
            } else {
                // Fallback mock authentication
                val entity = UserEntity(
                    userId = "user_${email.replace("@", "_").replace(".", "_")}",
                    tenantId = "tenant_alpha",
                    email = email,
                    displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    role = UserRole.OWNER,
                    creditsBalance = 40
                )
                dao.insertUser(entity)
                _currentUserState.value = AuthState.Authenticated(entity, null)
                Result.success(entity)
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Erreur de connexion"
            _currentUserState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmailPassword(
        email: String,
        pass: String,
        displayName: String,
        organizationName: String
    ): Result<UserEntity> {
        _currentUserState.value = AuthState.Loading
        return try {
            val auth = firebaseAuth
            val tenantSlug = organizationName.lowercase().trim().replace("\\s+".toRegex(), "-")
            val tenantId = "tenant_${tenantSlug.replace("-", "_")}"

            val newTenant = TenantEntity(
                tenantId = tenantId,
                name = organizationName,
                slug = tenantSlug,
                plan = SubscriptionTier.FREE,
                creditsBalance = 40
            )
            dao.insertTenant(newTenant)
            FirebaseFirestoreManager.syncTenant(newTenant)

            if (auth != null) {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val fbUser = result.user ?: throw Exception("Création de compte échouée")

                // Update profile display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                fbUser.updateProfile(profileUpdates).await()

                val entity = UserEntity(
                    userId = fbUser.uid,
                    tenantId = tenantId,
                    email = email,
                    displayName = displayName,
                    role = UserRole.OWNER,
                    creditsBalance = 40
                )
                dao.insertUser(entity)
                FirebaseFirestoreManager.syncUser(entity)

                _currentUserState.value = AuthState.Authenticated(entity, fbUser)
                Result.success(entity)
            } else {
                val entity = UserEntity(
                    userId = "user_${UUID.randomUUID().toString().take(8)}",
                    tenantId = tenantId,
                    email = email,
                    displayName = displayName,
                    role = UserRole.OWNER,
                    creditsBalance = 40
                )
                dao.insertUser(entity)
                _currentUserState.value = AuthState.Authenticated(entity, null)
                Result.success(entity)
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Erreur d'inscription"
            _currentUserState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(webClientId: String? = null): Result<UserEntity> {
        _currentUserState.value = AuthState.Loading
        return try {
            // Build GoogleIdOption
            val clientId = webClientId ?: "default-client-id.apps.googleusercontent.com"
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val auth = firebaseAuth
                if (auth != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val fbUser = authResult.user ?: throw Exception("Google Sign-In user introuvable")

                    val entity = syncUserWithLocalAndCloud(fbUser)
                    _currentUserState.value = AuthState.Authenticated(entity, fbUser)
                    Result.success(entity)
                } else {
                    val entity = UserEntity(
                        userId = "google_${googleIdTokenCredential.id.take(8)}",
                        tenantId = "tenant_alpha",
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: "Google User",
                        role = UserRole.OWNER,
                        avatarUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                        creditsBalance = 50
                    )
                    dao.insertUser(entity)
                    _currentUserState.value = AuthState.Authenticated(entity, null)
                    Result.success(entity)
                }
            } else {
                throw Exception("Type de credential Google non supporté")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(tag, "Google Sign-In cancelled by user")
            checkCurrentSession()
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Google Sign-In failed", e)
            val msg = e.localizedMessage ?: "Échec de connexion Google"
            _currentUserState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun switchDemoAccount(role: UserRole, tenantId: String, name: String, email: String) {
        val user = UserEntity(
            userId = "user_${email.substringBefore("@")}",
            tenantId = tenantId,
            email = email,
            displayName = name,
            role = role,
            creditsBalance = if (role == UserRole.OWNER) 58 else 20
        )
        dao.insertUser(user)
        _currentUserState.value = AuthState.Authenticated(user, null)
    }

    suspend fun signOut() {
        try {
            firebaseAuth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(tag, "Sign out error", e)
        }
        _currentUserState.value = AuthState.Idle
    }

    private suspend fun syncUserWithLocalAndCloud(fbUser: FirebaseUser): UserEntity {
        val entity = UserEntity(
            userId = fbUser.uid,
            tenantId = "tenant_alpha",
            email = fbUser.email ?: "user@taskflow.pro",
            displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Utilisateur",
            role = UserRole.OWNER,
            avatarUrl = fbUser.photoUrl?.toString(),
            creditsBalance = 50,
            lastActiveAt = System.currentTimeMillis()
        )
        dao.insertUser(entity)
        FirebaseFirestoreManager.syncUser(entity)
        return entity
    }
}
