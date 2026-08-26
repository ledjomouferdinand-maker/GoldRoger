package com.example.data.service

import android.util.Log
import com.example.data.firestore.FirebaseFirestoreManager
import com.example.data.local.TaskFlowDao
import com.example.data.model.CreditTransaction
import com.example.data.model.SubscriptionTier
import com.example.data.model.TaskRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserEntity
import com.example.data.model.UserWallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Result of a task execution balance update.
 */
data class CreditDeductionResult(
    val isSuccess: Boolean,
    val previousBalance: Int,
    val newBalance: Int,
    val creditsDeducted: Int,
    val timeSavedMinutes: Int,
    val transactionId: Long? = null,
    val errorMessage: String? = null
)

/**
 * Result of a credit pack topup or subscription update.
 */
data class BalanceTopupResult(
    val previousBalance: Int,
    val newBalance: Int,
    val creditsAdded: Int,
    val amountEuros: Double,
    val transactionId: Long
)

/**
 * Payment & Credit Service
 * 
 * Handles all business rules for:
 * 1. Tracking and applying user credit balance updates whenever a new task is completed.
 * 2. Calculating PRO tier discounts on execution cost.
 * 3. Atomic deductions across local Room DB and Cloud Firestore.
 * 4. Immutable financial ledger journalization (CreditTransaction).
 * 5. Multi-tenant balance synchronization.
 */
class PaymentCreditService(
    private val dao: TaskFlowDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val tag = "PaymentCreditService"

    /**
     * Observes real-time wallet balance for the active session.
     */
    fun observeWallet(): Flow<UserWallet?> = dao.getWallet()

    /**
     * Calculates the effective credit cost taking into account the user's active subscription tier.
     * PRO tier grants a 30% discount on base credit cost.
     */
    fun calculateEffectiveCost(baseCost: Int, subscriptionTier: SubscriptionTier): Int {
        return if (subscriptionTier == SubscriptionTier.PRO) {
            (baseCost * (1.0 - SubscriptionTier.PRO.discountRate)).toInt().coerceAtLeast(1)
        } else {
            baseCost
        }
    }

    /**
     * Core business method: Validates and deducts credits when a task is completed,
     * updates user stats (tasks completed, time saved), and records the ledger transaction.
     */
    suspend fun processTaskCompletionBalanceUpdate(
        tenantId: String,
        userId: String,
        taskId: Long?,
        taskTitle: String,
        baseCreditCost: Int,
        timeSavedMinutes: Int
    ): CreditDeductionResult {
        // 1. Fetch current local wallet
        val currentWallet = dao.getWallet().firstOrNull() ?: UserWallet(tenantId = tenantId)
        val effectiveCost = calculateEffectiveCost(baseCreditCost, currentWallet.subscriptionTier)
        val initialBalance = currentWallet.creditsBalance

        // 2. Pre-check balance
        if (initialBalance < effectiveCost) {
            val errorMsg = "Solde de crédits insuffisant ($initialBalance dispo, $effectiveCost requis)."
            Log.w(tag, "Credit deduction aborted: $errorMsg for user $userId in tenant $tenantId")
            return CreditDeductionResult(
                isSuccess = false,
                previousBalance = initialBalance,
                newBalance = initialBalance,
                creditsDeducted = 0,
                timeSavedMinutes = 0,
                errorMessage = errorMsg
            )
        }

        val newBalance = initialBalance - effectiveCost

        // 3. Update Wallet cache
        val updatedWallet = currentWallet.copy(
            tenantId = tenantId,
            creditsBalance = newBalance,
            totalTimeSavedMinutes = currentWallet.totalTimeSavedMinutes + timeSavedMinutes,
            totalTasksCompleted = currentWallet.totalTasksCompleted + 1
        )
        dao.insertOrUpdateWallet(updatedWallet)

        // 4. Update UserEntity if exists in Room
        val existingUser = dao.getUserById(userId).firstOrNull()
        if (existingUser != null) {
            val updatedUser = existingUser.copy(
                creditsBalance = newBalance,
                totalTasksCompleted = existingUser.totalTasksCompleted + 1,
                totalTimeSavedMinutes = existingUser.totalTimeSavedMinutes + timeSavedMinutes,
                lastActiveAt = System.currentTimeMillis()
            )
            dao.updateUser(updatedUser)
        }

        // 5. Append entry to financial ledger
        val transaction = CreditTransaction(
            tenantId = tenantId,
            userId = userId,
            taskId = taskId,
            title = "Exécution : $taskTitle",
            amountCredits = -effectiveCost,
            amountEuros = 0.0,
            type = TransactionType.SPEND.name,
            status = TransactionStatus.SUCCEEDED,
            timestamp = System.currentTimeMillis()
        )
        val txId = dao.insertTransaction(transaction)

        // 6. Asynchronous Cloud Firestore Synchronization
        scope.launch {
            try {
                FirebaseFirestoreManager.syncTransaction(transaction.copy(id = txId))
            } catch (e: Exception) {
                Log.e(tag, "Failed to sync transaction to Cloud Firestore", e)
            }
        }

        Log.i(tag, "Successfully processed task completion for user $userId. Deducted $effectiveCost credits. New balance: $newBalance")

        return CreditDeductionResult(
            isSuccess = true,
            previousBalance = initialBalance,
            newBalance = newBalance,
            creditsDeducted = effectiveCost,
            timeSavedMinutes = timeSavedMinutes,
            transactionId = txId
        )
    }

    /**
     * Adds credits to the user balance (e.g. Credit Pack top-up via Stripe).
     */
    suspend fun topUpCredits(
        tenantId: String,
        userId: String,
        packName: String,
        credits: Int,
        priceEuros: Double,
        stripePaymentIntentId: String? = null
    ): BalanceTopupResult {
        val currentWallet = dao.getWallet().firstOrNull() ?: UserWallet(tenantId = tenantId)
        val initialBalance = currentWallet.creditsBalance
        val newBalance = initialBalance + credits

        val updatedWallet = currentWallet.copy(
            tenantId = tenantId,
            creditsBalance = newBalance,
            totalSpentEuros = currentWallet.totalSpentEuros + priceEuros
        )
        dao.insertOrUpdateWallet(updatedWallet)

        // Update UserEntity balance
        val existingUser = dao.getUserById(userId).firstOrNull()
        if (existingUser != null) {
            dao.updateUser(
                existingUser.copy(
                    creditsBalance = newBalance,
                    totalSpentEuros = existingUser.totalSpentEuros + priceEuros
                )
            )
        }

        // Ledger record
        val tx = CreditTransaction(
            tenantId = tenantId,
            userId = userId,
            title = "Achat $packName (+$credits crédits)",
            amountCredits = credits,
            amountEuros = priceEuros,
            type = TransactionType.TOPUP.name,
            status = TransactionStatus.SUCCEEDED,
            stripePaymentIntentId = stripePaymentIntentId,
            timestamp = System.currentTimeMillis()
        )
        val txId = dao.insertTransaction(tx)

        scope.launch {
            FirebaseFirestoreManager.syncTransaction(tx.copy(id = txId))
        }

        return BalanceTopupResult(
            previousBalance = initialBalance,
            newBalance = newBalance,
            creditsAdded = credits,
            amountEuros = priceEuros,
            transactionId = txId
        )
    }

    /**
     * Activates or renews a Pro Subscription with bonus credits and discount.
     */
    suspend fun processSubscriptionUpgrade(
        tenantId: String,
        userId: String,
        tier: SubscriptionTier = SubscriptionTier.PRO,
        bonusCredits: Int = 50,
        priceEuros: Double = 49.0
    ): BalanceTopupResult {
        val currentWallet = dao.getWallet().firstOrNull() ?: UserWallet(tenantId = tenantId)
        val initialBalance = currentWallet.creditsBalance
        val newBalance = initialBalance + bonusCredits

        val updatedWallet = currentWallet.copy(
            tenantId = tenantId,
            subscriptionTier = tier,
            creditsBalance = newBalance,
            totalSpentEuros = currentWallet.totalSpentEuros + priceEuros,
            renewalDateMillis = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        )
        dao.insertOrUpdateWallet(updatedWallet)

        val tx = CreditTransaction(
            tenantId = tenantId,
            userId = userId,
            title = "Abonnement ${tier.label} Mensuel (${priceEuros.toInt()}€)",
            amountCredits = bonusCredits,
            amountEuros = priceEuros,
            type = TransactionType.SUBSCRIPTION.name,
            status = TransactionStatus.SUCCEEDED,
            timestamp = System.currentTimeMillis()
        )
        val txId = dao.insertTransaction(tx)

        scope.launch {
            FirebaseFirestoreManager.syncTransaction(tx.copy(id = txId))
        }

        return BalanceTopupResult(
            previousBalance = initialBalance,
            newBalance = newBalance,
            creditsAdded = bonusCredits,
            amountEuros = priceEuros,
            transactionId = txId
        )
    }

    /**
     * Records platform commission for human escalation expert services (20% commission).
     */
    suspend fun processHumanEscalationCommission(
        tenantId: String,
        userId: String,
        serviceName: String,
        providerType: String,
        quotedPriceEuros: Double,
        commissionRate: Double = 0.20
    ): Double {
        val platformCommission = quotedPriceEuros * commissionRate
        val tx = CreditTransaction(
            tenantId = tenantId,
            userId = userId,
            title = "Escalade Humaine : $serviceName ($providerType)",
            amountCredits = 0,
            amountEuros = quotedPriceEuros,
            type = TransactionType.HUMAN_COMMISSION.name,
            platformCommissionEuros = platformCommission,
            status = TransactionStatus.SUCCEEDED,
            timestamp = System.currentTimeMillis()
        )
        val txId = dao.insertTransaction(tx)
        scope.launch {
            FirebaseFirestoreManager.syncTransaction(tx.copy(id = txId))
        }
        return platformCommission
    }
}
