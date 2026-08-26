package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAIService
import com.example.data.ai.TaskConfigSuggestion
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.local.TaskFlowDatabase
import com.example.data.model.AdminPlatformMetrics
import com.example.data.model.CreditTransaction
import com.example.data.model.DigitalDocument
import com.example.data.model.ModuleType
import com.example.data.model.SubscriptionTier
import com.example.data.model.TaskRecord
import com.example.data.model.UserRole
import com.example.data.model.UserWallet
import com.example.data.repository.TaskFlowRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Home : ScreenDestination()
    data class ModuleView(val module: ModuleType) : ScreenDestination()
    object TaskDashboard : ScreenDestination()
    object WalletAndPricing : ScreenDestination()
    object AdminDashboard : ScreenDestination()
    object ArchitectureBlueprint : ScreenDestination()
    object Auth : ScreenDestination()
}

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class TaskFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskFlowRepository
    private val authRepository: AuthRepository
    val geminiAiService: GeminiAIService = GeminiAIService()
    
    val authState: StateFlow<AuthState>
    val userWallet: StateFlow<UserWallet>
    val allTasks: StateFlow<List<TaskRecord>>
    val allDocuments: StateFlow<List<DigitalDocument>>
    val allTransactions: StateFlow<List<CreditTransaction>>

    private val _aiSuggestion = MutableStateFlow<TaskConfigSuggestion?>(null)
    val aiSuggestion: StateFlow<TaskConfigSuggestion?> = _aiSuggestion.asStateFlow()

    private val _isGeneratingAiSuggestion = MutableStateFlow(false)
    val isGeneratingAiSuggestion: StateFlow<Boolean> = _isGeneratingAiSuggestion.asStateFlow()

    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Home)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _isExecutingTask = MutableStateFlow(false)
    val isExecutingTask: StateFlow<Boolean> = _isExecutingTask.asStateFlow()

    private val _lastGeneratedOutput = MutableStateFlow<String?>(null)
    val lastGeneratedOutput: StateFlow<String?> = _lastGeneratedOutput.asStateFlow()

    // Owner dynamic settings
    private val _adminMetrics = MutableStateFlow(AdminPlatformMetrics())
    val adminMetrics: StateFlow<AdminPlatformMetrics> = _adminMetrics.asStateFlow()

    init {
        val database = TaskFlowDatabase.getDatabase(application, viewModelScope)
        val dao = database.taskFlowDao()
        repository = TaskFlowRepository(dao)
        authRepository = AuthRepository(application.applicationContext, dao)

        authState = authRepository.currentUserState
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Idle)

        userWallet = repository.userWallet
            .map { it ?: UserWallet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserWallet())

        allTasks = repository.allTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allDocuments = repository.allDocuments
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTransactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun clearNotification() {
        _notification.value = null
    }

    fun showNotification(message: String, isError: Boolean = false) {
        _notification.value = UiNotification(message, isError)
    }

    fun executeModuleTask(
        moduleId: String,
        taskType: String,
        title: String,
        description: String,
        baseCreditCost: Int,
        timeSavedMinutes: Int,
        generatedPayload: String,
        createSafeDoc: Boolean = false,
        docCategory: String = "Général"
    ) {
        viewModelScope.launch {
            _isExecutingTask.value = true
            // Simulate realistic LLM pipeline & workflow step processing
            delay(1200)

            val result = repository.executeTask(
                moduleId = moduleId,
                taskType = taskType,
                title = title,
                description = description,
                baseCreditCost = baseCreditCost,
                timeSavedMinutes = timeSavedMinutes,
                generatedResult = generatedPayload,
                createSafeDocument = createSafeDoc,
                documentCategory = docCategory
            )

            _isExecutingTask.value = false

            result.onSuccess { task ->
                _lastGeneratedOutput.value = task.generatedResult
                showNotification("Tâche \"${task.title}\" complétée avec succès ! (-${task.creditsCost} crédits, +${timeSavedMinutes}m gagnées)")
                
                // Update simulated platform metrics
                val current = _adminMetrics.value
                _adminMetrics.value = current.copy(
                    tasksCompletedThisMonth = current.tasksCompletedThisMonth + 1,
                    totalTimeSavedHoursAllUsers = current.totalTimeSavedHoursAllUsers + (timeSavedMinutes / 60)
                )
            }.onFailure { err ->
                showNotification(err.message ?: "Erreur lors de l'exécution", isError = true)
            }
        }
    }

    fun purchaseCredits(packName: String, credits: Int, priceEuros: Double) {
        viewModelScope.launch {
            repository.purchaseCreditPack(packName, credits, priceEuros)
            showNotification("Pack $packName activé ! +$credits crédits ajoutés au portefeuille.")
            
            val current = _adminMetrics.value
            _adminMetrics.value = current.copy(
                totalGrossRevenue = current.totalGrossRevenue + priceEuros
            )
        }
    }

    fun upgradeSubscriptionToPro() {
        viewModelScope.launch {
            repository.upgradeToPro()
            showNotification("Félicitations ! Vous êtes maintenant membre TaskFlow Pro (30% de réduction sur toutes les tâches).")
            
            val current = _adminMetrics.value
            _adminMetrics.value = current.copy(
                mrrEuros = current.mrrEuros + 49.0,
                totalGrossRevenue = current.totalGrossRevenue + 49.0,
                activeSubscribersCount = current.activeSubscribersCount + 1
            )
        }
    }

    fun bookEscalation(serviceTitle: String, expertType: String, quoteEuros: Double) {
        viewModelScope.launch {
            val commRate = _adminMetrics.value.humanServiceCommissionRate
            val result = repository.bookHumanEscalation(serviceTitle, expertType, quoteEuros, commRate)
            result.onSuccess { commEuros ->
                showNotification("Demande d'expert transmise à $expertType. Rémunération plateforme : ${String.format("%.2f", commEuros)}€ (${(commRate * 100).toInt()}%).")
                
                val current = _adminMetrics.value
                _adminMetrics.value = current.copy(
                    totalGrossRevenue = current.totalGrossRevenue + quoteEuros,
                    totalCommissionsEarned = current.totalCommissionsEarned + commEuros
                )
            }
        }
    }

    fun updatePlatformCommissionRate(rate: Double) {
        val current = _adminMetrics.value
        _adminMetrics.value = current.copy(humanServiceCommissionRate = rate)
        showNotification("Taux de commission plateforme ajusté à ${(rate * 100).toInt()}%.")
    }

    fun deleteSafeDocument(id: Long) {
        viewModelScope.launch {
            repository.deleteDocument(id)
            showNotification("Document supprimé du coffre-fort numérique.")
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            val result = authRepository.signInWithEmailPassword(email, pass)
            result.onSuccess { user ->
                repository.setActiveTenantAndUser(user.tenantId, user.userId)
                showNotification("Bienvenue ${user.displayName} !")
                _currentScreen.value = ScreenDestination.Home
            }.onFailure { err ->
                showNotification("Échec de connexion : ${err.localizedMessage}", isError = true)
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String, orgName: String) {
        viewModelScope.launch {
            val result = authRepository.signUpWithEmailPassword(email, pass, displayName, orgName)
            result.onSuccess { user ->
                repository.setActiveTenantAndUser(user.tenantId, user.userId)
                showNotification("Compte et espace $orgName créés avec succès !")
                _currentScreen.value = ScreenDestination.Home
            }.onFailure { err ->
                showNotification("Échec d'inscription : ${err.localizedMessage}", isError = true)
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle()
            result.onSuccess { user ->
                repository.setActiveTenantAndUser(user.tenantId, user.userId)
                showNotification("Connexion Google réussie : ${user.displayName}")
                _currentScreen.value = ScreenDestination.Home
            }.onFailure { err ->
                showNotification("Google Sign-In : ${err.localizedMessage}", isError = true)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            showNotification("Déconnecté avec succès.")
        }
    }

    fun switchDemoAccount(role: UserRole, tenantId: String, name: String, email: String) {
        viewModelScope.launch {
            authRepository.switchDemoAccount(role, tenantId, name, email)
            val userId = "user_${email.substringBefore("@")}"
            repository.setActiveTenantAndUser(tenantId, userId)
            showNotification("Profil activé : $name ($role)")
        }
    }

    /**
     * Calls Gemini AI client service to get smart configuration suggestions for any module task.
     */
    fun requestTaskConfigurationSuggestion(
        moduleType: ModuleType,
        userIntent: String,
        contextInfo: String = ""
    ) {
        viewModelScope.launch {
            _isGeneratingAiSuggestion.value = true
            try {
                val suggestion = geminiAiService.getTaskConfigurationSuggestion(
                    moduleType = moduleType,
                    userIntent = userIntent,
                    contextInfo = contextInfo
                )
                _aiSuggestion.value = suggestion
                showNotification("Suggestion Gemini IA prête (${suggestion.source})")
            } catch (e: Exception) {
                val fallback = geminiAiService.getContextualFallbackSuggestion(moduleType, userIntent)
                _aiSuggestion.value = fallback
                showNotification("Suggestion prête (moteur intelligent)")
            } finally {
                _isGeneratingAiSuggestion.value = false
            }
        }
    }

    fun clearAiSuggestion() {
        _aiSuggestion.value = null
    }

    /**
     * Generates a rich, domain-specific AI deliverable draft using Gemini AI client.
     */
    fun generateAiDraft(
        moduleType: ModuleType,
        taskTitle: String,
        parameters: Map<String, String>,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isExecutingTask.value = true
            try {
                val draft = geminiAiService.generateTaskDraft(moduleType, taskTitle, parameters)
                onResult(draft)
                showNotification("Livrable rédigé par Gemini AI avec succès !")
            } catch (e: Exception) {
                showNotification("Erreur lors de la rédaction IA : ${e.message}", isError = true)
            } finally {
                _isExecutingTask.value = false
            }
        }
    }
}

