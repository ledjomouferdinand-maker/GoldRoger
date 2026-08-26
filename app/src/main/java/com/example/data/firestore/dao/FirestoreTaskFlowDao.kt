package com.example.data.firestore.dao

import android.util.Log
import com.example.data.firestore.model.FirestoreModule
import com.example.data.firestore.model.FirestoreTask
import com.example.data.firestore.model.FirestoreTenant
import com.example.data.firestore.model.FirestoreTransaction
import com.example.data.firestore.model.FirestoreUser
import com.example.data.firestore.model.FirestoreWallet
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Data Access Object (DAO) for Cloud Firestore.
 * Supports multi-tenancy by scoping collections under /tenants/{tenantId}/ and using user IDs / resource IDs as document keys.
 */
class FirestoreTaskFlowDao(private val firestore: FirebaseFirestore) {

    private val tag = "FirestoreTaskFlowDao"

    // Path Helpers
    fun tenantRef(tenantId: String): DocumentReference =
        firestore.collection("tenants").document(tenantId)

    fun userRef(tenantId: String, userId: String): DocumentReference =
        tenantRef(tenantId).collection("users").document(userId)

    fun moduleRef(tenantId: String, moduleId: String): DocumentReference =
        tenantRef(tenantId).collection("modules").document(moduleId)

    fun taskRef(tenantId: String, taskId: String): DocumentReference =
        tenantRef(tenantId).collection("tasks").document(taskId)

    fun transactionRef(tenantId: String, transactionId: String): DocumentReference =
        tenantRef(tenantId).collection("transactions").document(transactionId)

    fun walletRef(tenantId: String, userId: String): DocumentReference =
        tenantRef(tenantId).collection("wallets").document(userId)

    // =========================================================================
    // USER DAO OPERATIONS (Scoped by Tenant & User ID)
    // =========================================================================

    suspend fun getUser(tenantId: String, userId: String): FirestoreUser? {
        return try {
            val snapshot = userRef(tenantId, userId).get().await()
            if (snapshot.exists()) snapshot.toObject(FirestoreUser::class.java) else null
        } catch (e: Exception) {
            Log.e(tag, "Error fetching user $userId in tenant $tenantId", e)
            null
        }
    }

    suspend fun saveUser(user: FirestoreUser): Result<Unit> {
        return try {
            userRef(user.tenantId, user.userId).set(user, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving user ${user.userId}", e)
            Result.failure(e)
        }
    }

    fun observeUser(tenantId: String, userId: String): Flow<FirestoreUser?> = callbackFlow {
        val listener = userRef(tenantId, userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(tag, "Error observing user $userId", error)
                return@addSnapshotListener
            }
            val user = snapshot?.toObject(FirestoreUser::class.java)
            trySend(user)
        }
        awaitClose { listener.remove() }
    }

    fun observeTenantUsers(tenantId: String): Flow<List<FirestoreUser>> = callbackFlow {
        val listener = tenantRef(tenantId).collection("users")
            .orderBy("displayName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Error observing tenant users for $tenantId", error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { it.toObject(FirestoreUser::class.java) } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    // =========================================================================
    // MODULE DAO OPERATIONS (Tenant modules & Catalog)
    // =========================================================================

    suspend fun getModule(tenantId: String, moduleId: String): FirestoreModule? {
        return try {
            val snapshot = moduleRef(tenantId, moduleId).get().await()
            if (snapshot.exists()) snapshot.toObject(FirestoreModule::class.java) else null
        } catch (e: Exception) {
            Log.e(tag, "Error fetching module $moduleId", e)
            null
        }
    }

    suspend fun saveModule(module: FirestoreModule): Result<Unit> {
        return try {
            moduleRef(module.tenantId, module.moduleId).set(module, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving module ${module.moduleId}", e)
            Result.failure(e)
        }
    }

    fun observeTenantModules(tenantId: String): Flow<List<FirestoreModule>> = callbackFlow {
        val listener = tenantRef(tenantId).collection("modules")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Error observing modules for $tenantId", error)
                    return@addSnapshotListener
                }
                val modules = snapshot?.documents?.mapNotNull { it.toObject(FirestoreModule::class.java) } ?: emptyList()
                trySend(modules)
            }
        awaitClose { listener.remove() }
    }

    // =========================================================================
    // TASK DAO OPERATIONS (Scoped by Tenant, User ID & Task ID)
    // =========================================================================

    suspend fun getTask(tenantId: String, taskId: String): FirestoreTask? {
        return try {
            val snapshot = taskRef(tenantId, taskId).get().await()
            if (snapshot.exists()) snapshot.toObject(FirestoreTask::class.java) else null
        } catch (e: Exception) {
            Log.e(tag, "Error fetching task $taskId", e)
            null
        }
    }

    suspend fun saveTask(task: FirestoreTask): Result<String> {
        return try {
            val docId = if (task.taskId.isNotBlank()) task.taskId else "task_${System.currentTimeMillis()}"
            val finalTask = if (task.taskId.isBlank()) task.copy(taskId = docId) else task
            taskRef(task.tenantId, docId).set(finalTask, SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(tag, "Error saving task", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(tenantId: String, taskId: String): Result<Unit> {
        return try {
            taskRef(tenantId, taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting task $taskId", e)
            Result.failure(e)
        }
    }

    fun observeTasksByUser(tenantId: String, userId: String): Flow<List<FirestoreTask>> = callbackFlow {
        val listener = tenantRef(tenantId).collection("tasks")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Error observing tasks for user $userId", error)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { it.toObject(FirestoreTask::class.java) } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    fun observeTenantTasks(tenantId: String): Flow<List<FirestoreTask>> = callbackFlow {
        val listener = tenantRef(tenantId).collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Error observing tenant tasks for $tenantId", error)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { it.toObject(FirestoreTask::class.java) } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    // =========================================================================
    // TRANSACTION DAO OPERATIONS (Ledger entries)
    // =========================================================================

    suspend fun getTransaction(tenantId: String, transactionId: String): FirestoreTransaction? {
        return try {
            val snapshot = transactionRef(tenantId, transactionId).get().await()
            if (snapshot.exists()) snapshot.toObject(FirestoreTransaction::class.java) else null
        } catch (e: Exception) {
            Log.e(tag, "Error fetching transaction $transactionId", e)
            null
        }
    }

    suspend fun saveTransaction(transaction: FirestoreTransaction): Result<String> {
        return try {
            val docId = if (transaction.transactionId.isNotBlank()) transaction.transactionId else "tx_${System.currentTimeMillis()}"
            val finalTx = if (transaction.transactionId.isBlank()) transaction.copy(transactionId = docId) else transaction
            transactionRef(transaction.tenantId, docId).set(finalTx, SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(tag, "Error saving transaction", e)
            Result.failure(e)
        }
    }

    fun observeTransactionsByUser(tenantId: String, userId: String): Flow<List<FirestoreTransaction>> = callbackFlow {
        val listener = tenantRef(tenantId).collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Error observing transactions for user $userId", error)
                    return@addSnapshotListener
                }
                val txs = snapshot?.documents?.mapNotNull { it.toObject(FirestoreTransaction::class.java) } ?: emptyList()
                trySend(txs)
            }
        awaitClose { listener.remove() }
    }

    fun observeTenantTransactions(tenantId: String): Flow<List<FirestoreTransaction>> = callbackFlow {
        val listener = tenantRef(tenantId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Error observing transactions for tenant $tenantId", error)
                    return@addSnapshotListener
                }
                val txs = snapshot?.documents?.mapNotNull { it.toObject(FirestoreTransaction::class.java) } ?: emptyList()
                trySend(txs)
            }
        awaitClose { listener.remove() }
    }

    // =========================================================================
    // WALLET DAO OPERATIONS (Real-time Wallet state)
    // =========================================================================

    suspend fun getWallet(tenantId: String, userId: String): FirestoreWallet? {
        return try {
            val snapshot = walletRef(tenantId, userId).get().await()
            if (snapshot.exists()) snapshot.toObject(FirestoreWallet::class.java) else null
        } catch (e: Exception) {
            Log.e(tag, "Error fetching wallet for $userId", e)
            null
        }
    }

    suspend fun saveWallet(wallet: FirestoreWallet): Result<Unit> {
        return try {
            val docId = if (wallet.walletId.isNotBlank()) wallet.walletId else wallet.userId
            walletRef(wallet.tenantId, docId).set(wallet.copy(walletId = docId), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving wallet ${wallet.walletId}", e)
            Result.failure(e)
        }
    }

    fun observeWallet(tenantId: String, userId: String): Flow<FirestoreWallet?> = callbackFlow {
        val listener = walletRef(tenantId, userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(tag, "Error observing wallet for $userId", error)
                return@addSnapshotListener
            }
            val wallet = snapshot?.toObject(FirestoreWallet::class.java)
            trySend(wallet)
        }
        awaitClose { listener.remove() }
    }

    // =========================================================================
    // TENANT ORGANIZATION DAO OPERATIONS
    // =========================================================================

    suspend fun getTenant(tenantId: String): FirestoreTenant? {
        return try {
            val snapshot = tenantRef(tenantId).get().await()
            if (snapshot.exists()) snapshot.toObject(FirestoreTenant::class.java) else null
        } catch (e: Exception) {
            Log.e(tag, "Error fetching tenant $tenantId", e)
            null
        }
    }

    suspend fun saveTenant(tenant: FirestoreTenant): Result<Unit> {
        return try {
            tenantRef(tenant.tenantId).set(tenant, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving tenant ${tenant.tenantId}", e)
            Result.failure(e)
        }
    }

    fun observeTenant(tenantId: String): Flow<FirestoreTenant?> = callbackFlow {
        val listener = tenantRef(tenantId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(tag, "Error observing tenant $tenantId", error)
                return@addSnapshotListener
            }
            val tenant = snapshot?.toObject(FirestoreTenant::class.java)
            trySend(tenant)
        }
        awaitClose { listener.remove() }
    }

    // =========================================================================
    // ATOMIC MULTI-TENANT BATCH TRANSACTION
    // =========================================================================

    suspend fun executeTaskWithCreditsDeduction(
        task: FirestoreTask,
        transaction: FirestoreTransaction
    ): Result<Unit> {
        return try {
            firestore.runTransaction { firestoreTx ->
                val userRef = userRef(task.tenantId, task.userId)
                val userSnapshot = firestoreTx.get(userRef)
                val currentCredits = userSnapshot.getLong("creditsBalance") ?: 0L

                if (currentCredits < task.creditsCost) {
                    throw IllegalStateException("Solde insuffisant: $currentCredits crédits disponibles, ${task.creditsCost} requis.")
                }

                // 1. Deduct credits and increment task count on User
                val newCredits = currentCredits - task.creditsCost
                val newTasksCount = (userSnapshot.getLong("totalTasksCompleted") ?: 0L) + 1
                val newTimeSaved = (userSnapshot.getLong("totalTimeSavedMinutes") ?: 0L) + task.timeSavedMinutes
                val now = System.currentTimeMillis()

                firestoreTx.update(
                    userRef,
                    mapOf(
                        "creditsBalance" to newCredits,
                        "totalTasksCompleted" to newTasksCount,
                        "totalTimeSavedMinutes" to newTimeSaved,
                        "lastActiveAt" to now
                    )
                )

                // 2. Synchronize Wallet document
                val walletDocRef = walletRef(task.tenantId, task.userId)
                firestoreTx.set(
                    walletDocRef,
                    mapOf(
                        "walletId" to task.userId,
                        "tenantId" to task.tenantId,
                        "userId" to task.userId,
                        "creditsBalance" to newCredits,
                        "totalTasksCompleted" to newTasksCount,
                        "totalTimeSavedMinutes" to newTimeSaved,
                        "updatedAt" to now
                    ),
                    SetOptions.merge()
                )

                // 3. Save task
                val taskId = if (task.taskId.isNotBlank()) task.taskId else "task_${System.currentTimeMillis()}"
                val taskDocRef = taskRef(task.tenantId, taskId)
                firestoreTx.set(taskDocRef, task.copy(taskId = taskId))

                // 4. Save ledger transaction
                val txId = if (transaction.transactionId.isNotBlank()) transaction.transactionId else "tx_${System.currentTimeMillis()}"
                val txDocRef = transactionRef(transaction.tenantId, txId)
                firestoreTx.set(txDocRef, transaction.copy(transactionId = txId))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Atomic task execution failed", e)
            Result.failure(e)
        }
    }
}
