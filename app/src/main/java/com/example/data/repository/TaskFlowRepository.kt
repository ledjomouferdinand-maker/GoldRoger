package com.example.data.repository

import com.example.data.firestore.FirebaseFirestoreManager
import com.example.data.local.TaskFlowDao
import com.example.data.model.AdminPlatformMetrics
import com.example.data.model.CreditTransaction
import com.example.data.model.DigitalDocument
import com.example.data.model.ModuleEntity
import com.example.data.model.SubscriptionTier
import com.example.data.model.TaskRecord
import com.example.data.model.TaskStatus
import com.example.data.model.TenantEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.UserWallet
import com.example.data.service.PaymentCreditService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFlowRepository(
    private val dao: TaskFlowDao,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val paymentCreditService: PaymentCreditService = PaymentCreditService(dao, ioScope)
) {

    // Current active tenant & user context
    private val _activeTenantId = MutableStateFlow("tenant_alpha")
    val activeTenantId = _activeTenantId.asStateFlow()

    private val _activeUserId = MutableStateFlow("user_alex")
    val activeUserId = _activeUserId.asStateFlow()

    // Multi-tenant reactive streams
    val allTenants: Flow<List<TenantEntity>> = dao.getAllTenants()
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    val allModules: Flow<List<ModuleEntity>> = dao.getAllModules()

    val currentTenant: Flow<TenantEntity?> = _activeTenantId.flatMapLatest { tenantId ->
        dao.getTenantById(tenantId)
    }

    val currentTenantUsers: Flow<List<UserEntity>> = _activeTenantId.flatMapLatest { tenantId ->
        dao.getUsersByTenant(tenantId)
    }

    val currentTenantTasks: Flow<List<TaskRecord>> = _activeTenantId.flatMapLatest { tenantId ->
        dao.getTasksByTenant(tenantId)
    }

    val currentTenantDocuments: Flow<List<DigitalDocument>> = _activeTenantId.flatMapLatest { tenantId ->
        dao.getDocumentsByTenant(tenantId)
    }

    val currentTenantTransactions: Flow<List<CreditTransaction>> = _activeTenantId.flatMapLatest { tenantId ->
        dao.getTransactionsByTenant(tenantId)
    }

    // Legacy/global flows for backward compatibility
    val allTasks: Flow<List<TaskRecord>> = dao.getAllTasks()
    val allDocuments: Flow<List<DigitalDocument>> = dao.getAllDocuments()
    val userWallet: Flow<UserWallet?> = dao.getWallet()
    val allTransactions: Flow<List<CreditTransaction>> = dao.getAllTransactions()

    fun getTasksByModule(moduleId: String): Flow<List<TaskRecord>> = dao.getTasksByModule(moduleId)

    fun switchTenant(tenantId: String, userId: String) {
        _activeTenantId.value = tenantId
        _activeUserId.value = userId
    }

    fun setActiveTenantAndUser(tenantId: String, userId: String) {
        switchTenant(tenantId, userId)
    }

    suspend fun executeTask(
        moduleId: String,
        taskType: String,
        title: String,
        description: String,
        baseCreditCost: Int,
        timeSavedMinutes: Int,
        generatedResult: String,
        metadataJson: String = "{}",
        createSafeDocument: Boolean = false,
        documentCategory: String = "Général"
    ): Result<TaskRecord> {
        val currentTenantId = _activeTenantId.value
        val currentUserId = _activeUserId.value

        // 1. Process balance update & credit deduction via PaymentCreditService
        val deductionResult = paymentCreditService.processTaskCompletionBalanceUpdate(
            tenantId = currentTenantId,
            userId = currentUserId,
            taskId = null,
            taskTitle = title,
            baseCreditCost = baseCreditCost,
            timeSavedMinutes = timeSavedMinutes
        )

        if (!deductionResult.isSuccess) {
            return Result.failure(Exception(deductionResult.errorMessage ?: "Solde insuffisant"))
        }

        // 2. Save task record with actual effective cost deducted
        val task = TaskRecord(
            tenantId = currentTenantId,
            userId = currentUserId,
            moduleId = moduleId,
            taskType = taskType,
            title = title,
            description = description,
            creditsCost = deductionResult.creditsDeducted,
            timeSavedMinutes = timeSavedMinutes,
            status = TaskStatus.COMPLETED,
            generatedResult = generatedResult,
            metadataJson = metadataJson
        )
        val id = dao.insertTask(task)
        val savedTask = task.copy(id = id)
        ioScope.launch {
            FirebaseFirestoreManager.syncTask(savedTask)
        }

        // 3. If requested, add to digital safe
        if (createSafeDocument) {
            val doc = DigitalDocument(
                tenantId = currentTenantId,
                userId = currentUserId,
                title = title,
                category = documentCategory,
                fileFormat = "PDF",
                contentSummary = generatedResult.take(180) + "...",
                isVerifiedLegal = true,
                expirationDateMillis = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)
            )
            val docId = dao.insertDocument(doc)
            ioScope.launch {
                FirebaseFirestoreManager.syncDocument(doc.copy(id = docId))
            }
        }

        return Result.success(savedTask)
    }

    suspend fun purchaseCreditPack(packName: String, credits: Int, priceEuros: Double) {
        val currentTenantId = _activeTenantId.value
        val currentUserId = _activeUserId.value
        paymentCreditService.topUpCredits(
            tenantId = currentTenantId,
            userId = currentUserId,
            packName = packName,
            credits = credits,
            priceEuros = priceEuros
        )
    }

    suspend fun upgradeToPro() {
        val currentTenantId = _activeTenantId.value
        val currentUserId = _activeUserId.value
        paymentCreditService.processSubscriptionUpgrade(
            tenantId = currentTenantId,
            userId = currentUserId,
            tier = SubscriptionTier.PRO,
            bonusCredits = 50,
            priceEuros = 49.0
        )
    }

    suspend fun bookHumanEscalation(
        serviceName: String,
        providerType: String, // "Juriste / Avocat" or "Freelance Expert"
        quotedPriceEuros: Double,
        commissionRate: Double = 0.20
    ): Result<Double> {
        val currentTenantId = _activeTenantId.value
        val currentUserId = _activeUserId.value
        val commission = paymentCreditService.processHumanEscalationCommission(
            tenantId = currentTenantId,
            userId = currentUserId,
            serviceName = serviceName,
            providerType = providerType,
            quotedPriceEuros = quotedPriceEuros,
            commissionRate = commissionRate
        )
        return Result.success(commission)
    }

    suspend fun addCustomDocumentToSafe(title: String, category: String, summary: String) {
        val currentTenantId = _activeTenantId.value
        val currentUserId = _activeUserId.value
        val doc = DigitalDocument(
            tenantId = currentTenantId,
            userId = currentUserId,
            title = title,
            category = category,
            fileFormat = "PDF",
            contentSummary = summary,
            isVerifiedLegal = true,
            expirationDateMillis = System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000)
        )
        val docId = dao.insertDocument(doc)
        ioScope.launch {
            FirebaseFirestoreManager.syncDocument(doc.copy(id = docId))
        }
    }

    suspend fun createTenant(name: String, slug: String, plan: SubscriptionTier) {
        val tenantId = "tenant_${slug.replace("-", "_")}"
        val newTenant = TenantEntity(
            tenantId = tenantId,
            name = name,
            slug = slug,
            plan = plan,
            creditsBalance = if (plan == SubscriptionTier.PRO) 50 else 20
        )
        dao.insertTenant(newTenant)
        ioScope.launch {
            FirebaseFirestoreManager.syncTenant(newTenant)
        }
    }

    suspend fun deleteDocument(docId: Long) {
        dao.deleteDocument(docId)
    }
}
