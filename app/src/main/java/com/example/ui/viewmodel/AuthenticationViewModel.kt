package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.local.TaskFlowDatabase
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUser: UserEntity? = null,
    val isAuthenticated: Boolean = false
)

class AuthenticationViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository
    
    val authState: StateFlow<AuthState>

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        val database = TaskFlowDatabase.getDatabase(application, viewModelScope)
        authRepository = AuthRepository(application.applicationContext, database.taskFlowDao())
        
        authState = authRepository.currentUserState
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Idle)

        viewModelScope.launch {
            authRepository.currentUserState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is AuthState.Authenticated -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = null,
                            currentUser = state.user,
                            isAuthenticated = true,
                            successMessage = "Connecté en tant que ${state.user.displayName}"
                        )
                    }
                    is AuthState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = state.message,
                            successMessage = null
                        )
                    }
                    is AuthState.Idle -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            currentUser = null,
                            isAuthenticated = false
                        )
                    }
                }
            }
        }
    }

    fun signInWithGoogle(webClientId: String? = null, onSuccess: ((UserEntity) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithGoogle(webClientId)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentUser = user,
                    isAuthenticated = true,
                    successMessage = "Connexion Google réussie : ${user.displayName}"
                )
                onSuccess?.invoke(user)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Échec de connexion Google"
                )
            }
        }
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: ((UserEntity) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithEmailPassword(email, pass)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentUser = user,
                    isAuthenticated = true,
                    successMessage = "Bienvenue ${user.displayName}"
                )
                onSuccess?.invoke(user)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Identifiants invalides"
                )
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        pass: String,
        displayName: String,
        organizationName: String,
        onSuccess: ((UserEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signUpWithEmailPassword(email, pass, displayName, organizationName)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentUser = user,
                    isAuthenticated = true,
                    successMessage = "Compte créé pour $organizationName"
                )
                onSuccess?.invoke(user)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Échec de l'inscription"
                )
            }
        }
    }

    fun switchDemoAccount(
        role: UserRole,
        tenantId: String,
        name: String,
        email: String,
        onSuccess: ((UserEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            authRepository.switchDemoAccount(role, tenantId, name, email)
            val user = UserEntity(
                userId = "user_${email.substringBefore("@")}",
                tenantId = tenantId,
                email = email,
                displayName = name,
                role = role
            )
            _uiState.value = _uiState.value.copy(
                currentUser = user,
                isAuthenticated = true,
                successMessage = "Profil basculé : $name ($role)"
            )
            onSuccess?.invoke(user)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState(
                isLoading = false,
                errorMessage = null,
                successMessage = "Vous avez été déconnecté",
                currentUser = null,
                isAuthenticated = false
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
