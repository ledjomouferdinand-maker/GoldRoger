package com.example.data.firestore.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore Multi-Tenant Entity Models
 *
 * Path Schema:
 * /tenants/{tenantId}/users/{userId}
 * /tenants/{tenantId}/modules/{moduleId}
 * /tenants/{tenantId}/tasks/{taskId}
 * /tenants/{tenantId}/transactions/{transactionId}
 */

data class FirestoreUser(
    @DocumentId
    val userId: String = "",
    val tenantId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "MEMBER", // OWNER, ADMIN, MEMBER, AUDITOR
    val avatarUrl: String? = null,
    val creditsBalance: Int = 40,
    val totalTasksCompleted: Int = 0,
    val totalTimeSavedMinutes: Int = 0,
    val totalSpentEuros: Double = 0.0,
    @get:PropertyName("isActive")
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    @ServerTimestamp
    val serverUpdatedAt: Date? = null
)

data class FirestoreModule(
    @DocumentId
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
    @get:PropertyName("isEnabled")
    val isEnabled: Boolean = true,
    val supportedTaskTypes: List<String> = emptyList(),
    val version: String = "1.0.0",
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirestoreTask(
    @DocumentId
    val taskId: String = "",
    val tenantId: String = "",
    val userId: String = "",
    val moduleId: String = "",
    val taskType: String = "",
    val title: String = "",
    val description: String = "",
    val creditsCost: Int = 2,
    val timeSavedMinutes: Int = 60,
    val status: String = "COMPLETED", // DRAFT, QUEUED, PROCESSING, COMPLETED, FAILED
    val inputPayload: Map<String, String> = emptyMap(),
    val generatedResult: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    @ServerTimestamp
    val serverTimestamp: Date? = null
)

data class FirestoreTransaction(
    @DocumentId
    val transactionId: String = "",
    val tenantId: String = "",
    val userId: String = "",
    val taskId: Long? = null,
    val title: String = "",
    val amountCredits: Int = 0,
    val amountEuros: Double = 0.0,
    val type: String = "SPEND", // TOPUP, SPEND, SUBSCRIPTION, ESCROW_HOLD, ESCROW_RELEASE, HUMAN_COMMISSION, REFUND
    val status: String = "SUCCEEDED", // PENDING, SUCCEEDED, FAILED, REFUNDED
    val stripePaymentIntentId: String? = null,
    val platformCommissionEuros: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    @ServerTimestamp
    val serverTimestamp: Date? = null
)

data class FirestoreTenant(
    @DocumentId
    val tenantId: String = "",
    val name: String = "",
    val slug: String = "",
    val plan: String = "FREE", // FREE, PRO, ENTERPRISE
    val maxUsers: Int = 5,
    val creditsBalance: Int = 40,
    val storageQuotaBytes: Long = 1073741824L,
    val stripeCustomerId: String? = null,
    val customDomain: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirestoreWallet(
    @DocumentId
    val walletId: String = "",
    val tenantId: String = "",
    val userId: String = "",
    val creditsBalance: Int = 35,
    val subscriptionTier: String = "FREE",
    val totalTimeSavedMinutes: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalSpentEuros: Double = 0.0,
    val renewalDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val updatedAt: Long = System.currentTimeMillis(),
    @ServerTimestamp
    val serverUpdatedAt: Date? = null
)

