package com.example.data.firestore

import com.example.data.model.CreditTransaction
import com.example.data.model.DigitalDocument
import com.example.data.model.ModuleEntity
import com.example.data.model.SubscriptionTier
import com.example.data.model.TaskRecord
import com.example.data.model.TaskStatus
import com.example.data.model.TenantEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.UserWallet

/**
 * Multi-Tenant Firestore Schema Specification & Document Transfer Objects (DTOs)
 *
 * Firestore Document Hierarchy:
 * -------------------------------------------------------------
 * 1. /tenants/{tenantId}
 *      ├── /users/{userId}
 *      ├── /wallets/{userId}
 *      ├── /modules/{moduleId}
 *      ├── /tasks/{taskId}
 *      ├── /transactions/{transactionId}
 *      └── /documents/{docId}
 *
 * 2. /system_catalog/modules/{moduleId} (Global module catalog)
 * 3. /system_metrics/live (Platform-wide revenue and usage metrics)
 * -------------------------------------------------------------
 */

object FirestoreCollections {
    const val TENANTS = "tenants"
    const val USERS = "users"
    const val MODULES = "modules"
    const val TASKS = "tasks"
    const val TRANSACTIONS = "transactions"
    const val WALLETS = "wallets"
    const val DOCUMENTS = "documents"
    const val SYSTEM_CATALOG = "system_catalog"
    const val SYSTEM_METRICS = "system_metrics"

    fun tenantPath(tenantId: String): String = "$TENANTS/$tenantId"
    fun usersPath(tenantId: String): String = "$TENANTS/$tenantId/$USERS"
    fun modulesPath(tenantId: String): String = "$TENANTS/$tenantId/$MODULES"
    fun tasksPath(tenantId: String): String = "$TENANTS/$tenantId/$TASKS"
    fun transactionsPath(tenantId: String): String = "$TENANTS/$tenantId/$TRANSACTIONS"
    fun walletsPath(tenantId: String): String = "$TENANTS/$tenantId/$WALLETS"
    fun documentsPath(tenantId: String): String = "$TENANTS/$tenantId/$DOCUMENTS"
}

// ==========================================
// FIRESTORE DTOs
// ==========================================

data class FirestoreTenantDto(
    val tenantId: String = "",
    val name: String = "",
    val slug: String = "",
    val plan: String = "FREE",
    val maxUsers: Int = 5,
    val creditsBalance: Int = 40,
    val storageQuotaBytes: Long = 1073741824L,
    val stripeCustomerId: String? = null,
    val customDomain: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toRoomEntity(): TenantEntity = TenantEntity(
        tenantId = tenantId,
        name = name,
        slug = slug,
        plan = try { SubscriptionTier.valueOf(plan) } catch (e: Exception) { SubscriptionTier.FREE },
        maxUsers = maxUsers,
        creditsBalance = creditsBalance,
        storageQuotaBytes = storageQuotaBytes,
        stripeCustomerId = stripeCustomerId,
        customDomain = customDomain,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromRoomEntity(entity: TenantEntity): FirestoreTenantDto = FirestoreTenantDto(
            tenantId = entity.tenantId,
            name = entity.name,
            slug = entity.slug,
            plan = entity.plan.name,
            maxUsers = entity.maxUsers,
            creditsBalance = entity.creditsBalance,
            storageQuotaBytes = entity.storageQuotaBytes,
            stripeCustomerId = entity.stripeCustomerId,
            customDomain = entity.customDomain,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}

data class FirestoreUserDto(
    val userId: String = "",
    val tenantId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "MEMBER",
    val avatarUrl: String? = null,
    val creditsBalance: Int = 35,
    val totalTasksCompleted: Int = 0,
    val totalTimeSavedMinutes: Int = 0,
    val totalSpentEuros: Double = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val lastActiveAt: Long = 0L
) {
    fun toRoomEntity(): UserEntity = UserEntity(
        userId = userId,
        tenantId = tenantId,
        email = email,
        displayName = displayName,
        role = try { UserRole.valueOf(role) } catch (e: Exception) { UserRole.MEMBER },
        avatarUrl = avatarUrl,
        creditsBalance = creditsBalance,
        totalTasksCompleted = totalTasksCompleted,
        totalTimeSavedMinutes = totalTimeSavedMinutes,
        totalSpentEuros = totalSpentEuros,
        isActive = isActive,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt
    )

    companion object {
        fun fromRoomEntity(entity: UserEntity): FirestoreUserDto = FirestoreUserDto(
            userId = entity.userId,
            tenantId = entity.tenantId,
            email = entity.email,
            displayName = entity.displayName,
            role = entity.role.name,
            avatarUrl = entity.avatarUrl,
            creditsBalance = entity.creditsBalance,
            totalTasksCompleted = entity.totalTasksCompleted,
            totalTimeSavedMinutes = entity.totalTimeSavedMinutes,
            totalSpentEuros = entity.totalSpentEuros,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            lastActiveAt = entity.lastActiveAt
        )
    }
}

data class FirestoreModuleDto(
    val moduleId: String = "",
    val tenantId: String = "global",
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val iconName: String = "",
    val primaryColorHex: Long = 0xFF6366F1,
    val baseCreditCost: Int = 2,
    val estimatedTimeSavedMinutes: Int = 60,
    val category: String = "Légal & Administratif",
    val isEnabled: Boolean = true,
    val supportedTaskTypes: List<String> = emptyList(),
    val version: String = "1.0.0",
    val updatedAt: Long = 0L
) {
    fun toRoomEntity(): ModuleEntity = ModuleEntity(
        moduleId = moduleId,
        tenantId = tenantId,
        title = title,
        subtitle = subtitle,
        description = description,
        iconName = iconName,
        primaryColorHex = primaryColorHex,
        baseCreditCost = baseCreditCost,
        estimatedTimeSavedMinutes = estimatedTimeSavedMinutes,
        category = category,
        isEnabled = isEnabled,
        supportedTaskTypesJson = supportedTaskTypes.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]"),
        version = version,
        updatedAt = updatedAt
    )

    companion object {
        fun fromRoomEntity(entity: ModuleEntity): FirestoreModuleDto = FirestoreModuleDto(
            moduleId = entity.moduleId,
            tenantId = entity.tenantId,
            title = entity.title,
            subtitle = entity.subtitle,
            description = entity.description,
            iconName = entity.iconName,
            primaryColorHex = entity.primaryColorHex,
            baseCreditCost = entity.baseCreditCost,
            estimatedTimeSavedMinutes = entity.estimatedTimeSavedMinutes,
            category = entity.category,
            isEnabled = entity.isEnabled,
            supportedTaskTypes = entity.supportedTaskTypesJson
                .trim('[', ']')
                .split(',')
                .map { it.trim().trim('"', '\'') }
                .filter { it.isNotEmpty() },
            version = entity.version,
            updatedAt = entity.updatedAt
        )
    }
}

data class FirestoreTaskDto(
    val taskId: String = "",
    val tenantId: String = "",
    val userId: String = "",
    val moduleId: String = "",
    val taskType: String = "",
    val title: String = "",
    val description: String = "",
    val creditsCost: Int = 2,
    val timeSavedMinutes: Int = 60,
    val status: String = "COMPLETED",
    val inputPayload: Map<String, String> = emptyMap(),
    val generatedResult: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val createdAt: Long = 0L,
    val completedAt: Long? = null
) {
    fun toRoomEntity(): TaskRecord = TaskRecord(
        id = taskId.toLongOrNull() ?: 0L,
        tenantId = tenantId,
        userId = userId,
        moduleId = moduleId,
        taskType = taskType,
        title = title,
        description = description,
        creditsCost = creditsCost,
        timeSavedMinutes = timeSavedMinutes,
        status = try { TaskStatus.valueOf(status) } catch (e: Exception) { TaskStatus.COMPLETED },
        inputPayloadJson = "{}",
        generatedResult = generatedResult,
        metadataJson = "{}",
        errorMessage = errorMessage,
        createdAt = createdAt,
        completedAt = completedAt
    )

    companion object {
        fun fromRoomEntity(entity: TaskRecord): FirestoreTaskDto = FirestoreTaskDto(
            taskId = entity.id.toString(),
            tenantId = entity.tenantId,
            userId = entity.userId,
            moduleId = entity.moduleId,
            taskType = entity.taskType,
            title = entity.title,
            description = entity.description,
            creditsCost = entity.creditsCost,
            timeSavedMinutes = entity.timeSavedMinutes,
            status = entity.status.name,
            inputPayload = emptyMap(),
            generatedResult = entity.generatedResult,
            metadata = emptyMap(),
            errorMessage = entity.errorMessage,
            createdAt = entity.createdAt,
            completedAt = entity.completedAt
        )
    }
}

data class FirestoreTransactionDto(
    val transactionId: String = "",
    val tenantId: String = "",
    val userId: String = "",
    val taskId: Long? = null,
    val title: String = "",
    val amountCredits: Int = 0,
    val amountEuros: Double = 0.0,
    val type: String = "SPEND",
    val status: String = "SUCCEEDED",
    val stripePaymentIntentId: String? = null,
    val platformCommissionEuros: Double = 0.0,
    val timestamp: Long = 0L
) {
    fun toRoomEntity(): CreditTransaction = CreditTransaction(
        id = transactionId.toLongOrNull() ?: 0L,
        tenantId = tenantId,
        userId = userId,
        taskId = taskId,
        title = title,
        amountCredits = amountCredits,
        amountEuros = amountEuros,
        type = type,
        status = try { TransactionStatus.valueOf(status) } catch (e: Exception) { TransactionStatus.SUCCEEDED },
        stripePaymentIntentId = stripePaymentIntentId,
        platformCommissionEuros = platformCommissionEuros,
        timestamp = timestamp
    )

    companion object {
        fun fromRoomEntity(entity: CreditTransaction): FirestoreTransactionDto = FirestoreTransactionDto(
            transactionId = entity.id.toString(),
            tenantId = entity.tenantId,
            userId = entity.userId,
            taskId = entity.taskId,
            title = entity.title,
            amountCredits = entity.amountCredits,
            amountEuros = entity.amountEuros,
            type = entity.type,
            status = entity.status.name,
            stripePaymentIntentId = entity.stripePaymentIntentId,
            platformCommissionEuros = entity.platformCommissionEuros,
            timestamp = entity.timestamp
        )
    }
}

data class FirestoreWalletDto(
    val walletId: String = "",
    val tenantId: String = "",
    val userId: String = "",
    val creditsBalance: Int = 35,
    val subscriptionTier: String = "FREE",
    val totalTimeSavedMinutes: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalSpentEuros: Double = 0.0,
    val renewalDateMillis: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toRoomEntity(): UserWallet = UserWallet(
        id = 1,
        tenantId = tenantId,
        creditsBalance = creditsBalance,
        subscriptionTier = try { SubscriptionTier.valueOf(subscriptionTier) } catch (e: Exception) { SubscriptionTier.FREE },
        totalTimeSavedMinutes = totalTimeSavedMinutes,
        totalTasksCompleted = totalTasksCompleted,
        totalSpentEuros = totalSpentEuros,
        renewalDateMillis = renewalDateMillis
    )

    companion object {
        fun fromRoomEntity(entity: UserWallet, userId: String = "default_user"): FirestoreWalletDto = FirestoreWalletDto(
            walletId = userId,
            tenantId = entity.tenantId,
            userId = userId,
            creditsBalance = entity.creditsBalance,
            subscriptionTier = entity.subscriptionTier.name,
            totalTimeSavedMinutes = entity.totalTimeSavedMinutes,
            totalTasksCompleted = entity.totalTasksCompleted,
            totalSpentEuros = entity.totalSpentEuros,
            renewalDateMillis = entity.renewalDateMillis,
            updatedAt = System.currentTimeMillis()
        )
    }
}

data class FirestoreDocumentDto(
    val documentId: String = "",
    val tenantId: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Contrat",
    val fileFormat: String = "PDF",
    val contentSummary: String = "",
    val isVerifiedLegal: Boolean = true,
    val storageUri: String? = null,
    val expirationDateMillis: Long? = null,
    val createdAt: Long = 0L
) {
    fun toRoomEntity(): DigitalDocument = DigitalDocument(
        id = documentId.toLongOrNull() ?: 0L,
        tenantId = tenantId,
        userId = userId,
        title = title,
        category = category,
        fileFormat = fileFormat,
        contentSummary = contentSummary,
        isVerifiedLegal = isVerifiedLegal,
        storageUri = storageUri,
        expirationDateMillis = expirationDateMillis,
        createdAt = createdAt
    )

    companion object {
        fun fromRoomEntity(entity: DigitalDocument): FirestoreDocumentDto = FirestoreDocumentDto(
            documentId = entity.id.toString(),
            tenantId = entity.tenantId,
            userId = entity.userId,
            title = entity.title,
            category = entity.category,
            fileFormat = entity.fileFormat,
            contentSummary = entity.contentSummary,
            isVerifiedLegal = entity.isVerifiedLegal,
            storageUri = entity.storageUri,
            expirationDateMillis = entity.expirationDateMillis,
            createdAt = entity.createdAt
        )
    }
}

// ==========================================
// FIRESTORE SECURITY RULES & INDEX SPEC
// ==========================================

object FirestoreSecurityRules {
    val RULES_DEFINITION: String = """
        rules_version = '2';
        service cloud.firestore {
          match /databases/{database}/documents {
            
            // Helper function to check if request is authenticated
            function isAuthenticated() {
              return request.auth != null;
            }
            
            // Helper function to check tenant membership
            function belongsToTenant(tenantId) {
              return isAuthenticated() && 
                request.auth.token.tenantId == tenantId;
            }
            
            // Helper function to check role in tenant
            function hasTenantRole(tenantId, role) {
              return belongsToTenant(tenantId) && 
                request.auth.token.role == role;
            }

            // Tenants root isolation
            match /tenants/{tenantId} {
              allow read: if belongsToTenant(tenantId);
              allow write: if hasTenantRole(tenantId, 'OWNER');
              
              // Users inside tenant
              match /users/{userId} {
                allow read: if belongsToTenant(tenantId);
                allow write: if belongsToTenant(tenantId) && (request.auth.uid == userId || hasTenantRole(tenantId, 'ADMIN'));
              }
              
              // Module configurations
              match /modules/{moduleId} {
                allow read: if belongsToTenant(tenantId);
                allow write: if hasTenantRole(tenantId, 'OWNER');
              }
              
              // Workflow Tasks
              match /tasks/{taskId} {
                allow read: if belongsToTenant(tenantId);
                allow create: if belongsToTenant(tenantId) && request.resource.data.creditsCost <= get(/databases/$(database)/documents/tenants/$(tenantId)).data.creditsBalance;
                allow update, delete: if belongsToTenant(tenantId) && (request.auth.uid == resource.data.userId || hasTenantRole(tenantId, 'ADMIN'));
              }
              
              // Ledger & Transactions (Append-only & system writes)
              match /transactions/{txId} {
                allow read: if belongsToTenant(tenantId);
                allow create: if belongsToTenant(tenantId);
                allow update, delete: if false; // Immutable ledger
              }
              
              // Real-time Wallets (User & Organization balance state)
              match /wallets/{walletId} {
                allow read: if belongsToTenant(tenantId);
                allow write: if belongsToTenant(tenantId) && (request.auth.uid == walletId || hasTenantRole(tenantId, 'ADMIN'));
              }
              
              // Vault Digital Documents
              match /documents/{docId} {
                allow read: if belongsToTenant(tenantId);
                allow write: if belongsToTenant(tenantId);
              }
            }
            
            // Global System Catalog (Public read-only)
            match /system_catalog/{document=**} {
              allow read: if isAuthenticated();
              allow write: if false;
            }
          }
        }
    """.trimIndent()

    val COMPOSITE_INDEXES: List<String> = listOf(
        "tenants/{tenantId}/tasks : moduleId ASC, createdAt DESC",
        "tenants/{tenantId}/tasks : status ASC, createdAt DESC",
        "tenants/{tenantId}/tasks : userId ASC, createdAt DESC",
        "tenants/{tenantId}/transactions : type ASC, timestamp DESC",
        "tenants/{tenantId}/transactions : userId ASC, timestamp DESC",
        "tenants/{tenantId}/documents : category ASC, createdAt DESC"
    )
}
