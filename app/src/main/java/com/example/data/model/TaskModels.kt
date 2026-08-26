package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ==========================================
// ENUMS & CONSTANTS
// ==========================================

enum class UserRole(val label: String, val permissions: List<String>) {
    OWNER("Propriétaire", listOf("ALL", "BILLING_MANAGE", "USER_MANAGE", "TASK_EXECUTE", "API_KEYS")),
    ADMIN("Administrateur", listOf("USER_MANAGE", "TASK_EXECUTE", "REPORTS_VIEW")),
    MEMBER("Collaborateur", listOf("TASK_EXECUTE", "REPORTS_VIEW")),
    AUDITOR("Auditeur / Juriste", listOf("AUDIT_READ", "VAULT_READ"))
}

enum class ModuleCategory(val label: String) {
    LEGAL_ADMIN("Légal & Administratif"),
    MARKETING_CONTENT("Contenu & Médias"),
    WEB_TECH("Web & Développement"),
    SALES_FREELANCE("Vente & Freelancing"),
    GROWTH_ADS("Acquisition & Ads")
}

enum class ModuleType(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val primaryColorHex: Long,
    val category: ModuleCategory = ModuleCategory.LEGAL_ADMIN,
    val defaultCost: Int = 2
) {
    PAPERASSE_EXPRESS(
        id = "paperasse",
        title = "Paperasse Express",
        subtitle = "Administration & Légal",
        iconName = "Description",
        primaryColorHex = 0xFF6366F1, // Indigo
        category = ModuleCategory.LEGAL_ADMIN,
        defaultCost = 2
    ),
    CONTENT_STUDIO(
        id = "content",
        title = "Content Studio",
        subtitle = "Création & Repurposing",
        iconName = "AutoAwesome",
        primaryColorHex = 0xFFEC4899, // Pink / Magenta
        category = ModuleCategory.MARKETING_CONTENT,
        defaultCost = 4
    ),
    WEB_LAUNCH(
        id = "weblaunch",
        title = "WebLaunch",
        subtitle = "No-Code & Déploiement",
        iconName = "Language",
        primaryColorHex = 0xFF06B6D4, // Cyan
        category = ModuleCategory.WEB_TECH,
        defaultCost = 3
    ),
    FREELANCE_HUB(
        id = "freelance",
        title = "Freelance Hub",
        subtitle = "Gestion & Devis",
        iconName = "WorkOutline",
        primaryColorHex = 0xFF10B981, // Emerald
        category = ModuleCategory.SALES_FREELANCE,
        defaultCost = 3
    ),
    GROWTH_ENGINE(
        id = "growth",
        title = "Growth Engine",
        subtitle = "Marketing & Acquisition",
        iconName = "TrendingUp",
        primaryColorHex = 0xFFF59E0B, // Amber
        category = ModuleCategory.GROWTH_ADS,
        defaultCost = 5
    )
}

enum class SubscriptionTier(val label: String, val priceMonthly: Double, val discountRate: Double, val maxUsers: Int = 5) {
    FREE("Freemium Starter", 0.0, 0.0, 1),
    PRO("TaskFlow Pro", 49.0, 0.30, 5),
    ENTERPRISE("TaskFlow Enterprise", 199.0, 0.50, 50)
}

enum class TaskStatus {
    DRAFT, QUEUED, PROCESSING, COMPLETED, FAILED
}

enum class TransactionType(val label: String) {
    TOPUP("Recharge Crédits"),
    SPEND("Consommation Tâche"),
    SUBSCRIPTION("Abonnement Mensuel"),
    ESCROW_HOLD("Séquestre Expert"),
    ESCROW_RELEASE("Paiement Expert"),
    HUMAN_COMMISSION("Commission Plateforme (20%)"),
    REFUND("Remboursement")
}

enum class TransactionStatus {
    PENDING, SUCCEEDED, FAILED, REFUNDED
}

// ==========================================
// MULTI-TENANT ROOM ENTITIES
// ==========================================

/**
 * Tenant entity for organization-level data partitioning.
 */
@Entity(
    tableName = "tenants",
    indices = [Index(value = ["slug"], unique = true)]
)
data class TenantEntity(
    @PrimaryKey val tenantId: String,
    val name: String,
    val slug: String,
    val plan: SubscriptionTier = SubscriptionTier.FREE,
    val maxUsers: Int = 5,
    val creditsBalance: Int = 40,
    val storageQuotaBytes: Long = 1073741824L, // 1 GB
    val stripeCustomerId: String? = null,
    val customDomain: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * User entity partitioned by tenantId.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val userId: String,
    val tenantId: String,
    val email: String,
    val displayName: String,
    val role: UserRole = UserRole.MEMBER,
    val avatarUrl: String? = null,
    val creditsBalance: Int = 35,
    val totalTasksCompleted: Int = 0,
    val totalTimeSavedMinutes: Int = 0,
    val totalSpentEuros: Double = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

/**
 * Module entity representing service modules in the platform catalog or tenant overrides.
 */
@Entity(
    tableName = "modules",
    indices = [
        Index(value = ["tenantId", "moduleId"], unique = true)
    ]
)
data class ModuleEntity(
    @PrimaryKey val moduleId: String,
    val tenantId: String = "global",
    val title: String,
    val subtitle: String,
    val description: String,
    val iconName: String,
    val primaryColorHex: Long,
    val baseCreditCost: Int,
    val estimatedTimeSavedMinutes: Int,
    val category: String,
    val isEnabled: Boolean = true,
    val supportedTaskTypesJson: String = "[]",
    val version: String = "1.0.0",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Task record representing automated workflow executions, partitioned by tenantId.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["userId"]),
        Index(value = ["moduleId"]),
        Index(value = ["createdAt"])
    ]
)
data class TaskRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "tenant_default",
    val userId: String = "user_default",
    val moduleId: String,
    val taskType: String,
    val title: String,
    val description: String,
    val creditsCost: Int,
    val timeSavedMinutes: Int,
    val status: TaskStatus = TaskStatus.COMPLETED,
    val inputPayloadJson: String = "{}",
    val generatedResult: String,
    val metadataJson: String = "{}",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Digital safe document storage entity partitioned by tenantId.
 */
@Entity(
    tableName = "digital_documents",
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["category"])
    ]
)
data class DigitalDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "tenant_default",
    val userId: String = "user_default",
    val title: String,
    val category: String, // "Contrat", "Facture", "URSSAF", "TVA", "Kbis"
    val fileFormat: String = "PDF",
    val contentSummary: String,
    val isVerifiedLegal: Boolean = true,
    val storageUri: String? = null,
    val expirationDateMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * User wallet state for quick local cache.
 */
@Entity(tableName = "wallet")
data class UserWallet(
    @PrimaryKey val id: Int = 1,
    val tenantId: String = "tenant_default",
    val creditsBalance: Int = 35,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val totalTimeSavedMinutes: Int = 410,
    val totalTasksCompleted: Int = 14,
    val totalSpentEuros: Double = 95.0,
    val renewalDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
)

/**
 * Financial and credit ledger transactions partitioned by tenantId.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["tenantId"]),
        Index(value = ["userId"]),
        Index(value = ["timestamp"])
    ]
)
data class CreditTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: String = "tenant_default",
    val userId: String = "user_default",
    val taskId: Long? = null,
    val title: String,
    val amountCredits: Int, // positive for topup/refund, negative for spend
    val amountEuros: Double = 0.0,
    val type: String = "SPEND", // "TOPUP", "SPEND", "SUBSCRIPTION", "HUMAN_COMMISSION", "ESCROW_HOLD"
    val status: TransactionStatus = TransactionStatus.SUCCEEDED,
    val stripePaymentIntentId: String? = null,
    val platformCommissionEuros: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

// ==========================================
// AGGREGATE / METRICS MODELS
// ==========================================

data class AdminPlatformMetrics(
    val mrrEuros: Double = 14850.0,
    val totalGrossRevenue: Double = 48200.0,
    val totalCommissionsEarned: Double = 9640.0, // 20% on human services
    val activeSubscribersCount: Int = 312,
    val activeFreemiumUsersCount: Int = 1840,
    val arpu: Double = 47.60,
    val cac: Double = 18.20,
    val netMarginRate: Double = 0.74,
    val totalTimeSavedHoursAllUsers: Int = 8940,
    val tasksCompletedThisMonth: Int = 5420,
    val humanServiceCommissionRate: Double = 0.20 // 20%
)
