package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.data.model.CreditTransaction
import com.example.data.model.DigitalDocument
import com.example.data.model.ModuleEntity
import com.example.data.model.TaskRecord
import com.example.data.model.TenantEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserWallet
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Central Firebase Firestore Configuration and Cloud Storage Manager
 *
 * Implements:
 * 1. Safe FirebaseApp & Firestore initialization with offline persistent caching.
 * 2. Multi-tenant document mapping & CRUD operations.
 * 3. Realtime collection listeners for cloud synchronization.
 */
object FirebaseFirestoreManager {
    private const val TAG = "FirebaseFirestoreMgr"

    private var firestoreInstance: FirebaseFirestore? = null

    /**
     * Initializes Firebase and configures Firestore settings with offline caching enabled.
     */
    fun initialize(context: Context): FirebaseFirestore? {
        if (firestoreInstance != null) return firestoreInstance

        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val db = FirebaseFirestore.getInstance()
            
            // Configure persistent on-disk cache for offline resilience
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(100L * 1024 * 1024) // 100 MB cache
                        .build()
                )
                .build()

            db.firestoreSettings = settings
            firestoreInstance = db
            Log.i(TAG, "Firebase Firestore successfully initialized with persistent cache.")
            db
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization skipped or falling back to local-only mode: ${e.message}")
            null
        }
    }

    fun getFirestore(): FirebaseFirestore? {
        return firestoreInstance ?: run {
            try {
                val db = FirebaseFirestore.getInstance()
                firestoreInstance = db
                db
            } catch (e: Exception) {
                Log.w(TAG, "Firestore instance not available: ${e.message}")
                null
            }
        }
    }

    // =========================================================================
    // MULTI-TENANT CLOUD SYNC OPERATIONS
    // =========================================================================

    /**
     * Uploads or updates tenant organization info in Cloud Firestore.
     */
    suspend fun syncTenant(tenant: TenantEntity): Result<Unit> {
        val db = getFirestore() ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val dto = FirestoreTenantDto.fromRoomEntity(tenant)
            val docRef = db.collection(FirestoreCollections.TENANTS).document(tenant.tenantId)
            docRef.set(dto, SetOptions.merge()).await()
            Log.d(TAG, "Tenant synced to Firestore: ${tenant.tenantId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync tenant ${tenant.tenantId} to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads or updates user profile in Cloud Firestore under /tenants/{tenantId}/users/{userId}.
     */
    suspend fun syncUser(user: UserEntity): Result<Unit> {
        val db = getFirestore() ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val dto = FirestoreUserDto.fromRoomEntity(user)
            val docRef = db.collection(FirestoreCollections.TENANTS)
                .document(user.tenantId)
                .collection(FirestoreCollections.USERS)
                .document(user.userId)
            docRef.set(dto, SetOptions.merge()).await()
            Log.d(TAG, "User synced to Firestore: ${user.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user ${user.userId} to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads an automated task record to Cloud Firestore under /tenants/{tenantId}/tasks/{taskId}.
     */
    suspend fun syncTask(task: TaskRecord): Result<Unit> {
        val db = getFirestore() ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val dto = FirestoreTaskDto.fromRoomEntity(task)
            val docId = if (task.id > 0) task.id.toString() else "task_${System.currentTimeMillis()}"
            val docRef = db.collection(FirestoreCollections.TENANTS)
                .document(task.tenantId)
                .collection(FirestoreCollections.TASKS)
                .document(docId)
            docRef.set(dto, SetOptions.merge()).await()
            Log.d(TAG, "Task synced to Firestore: $docId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync task to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads a credit transaction to Cloud Firestore ledger under /tenants/{tenantId}/transactions/{txId}.
     */
    suspend fun syncTransaction(transaction: CreditTransaction): Result<Unit> {
        val db = getFirestore() ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val dto = FirestoreTransactionDto.fromRoomEntity(transaction)
            val docId = if (transaction.id > 0) transaction.id.toString() else "tx_${System.currentTimeMillis()}"
            val docRef = db.collection(FirestoreCollections.TENANTS)
                .document(transaction.tenantId)
                .collection(FirestoreCollections.TRANSACTIONS)
                .document(docId)
            docRef.set(dto, SetOptions.merge()).await()
            Log.d(TAG, "Transaction ledger synced to Firestore: $docId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync transaction to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads a digital document to Cloud Firestore safe under /tenants/{tenantId}/documents/{docId}.
     */
    suspend fun syncDocument(document: DigitalDocument): Result<Unit> {
        val db = getFirestore() ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val dto = FirestoreDocumentDto.fromRoomEntity(document)
            val docId = if (document.id > 0) document.id.toString() else "doc_${System.currentTimeMillis()}"
            val docRef = db.collection(FirestoreCollections.TENANTS)
                .document(document.tenantId)
                .collection(FirestoreCollections.DOCUMENTS)
                .document(docId)
            docRef.set(dto, SetOptions.merge()).await()
            Log.d(TAG, "Digital document synced to Firestore: $docId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync document to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time tasks from Firestore for a specific tenant.
     */
    fun observeCloudTasks(tenantId: String): Flow<List<FirestoreTaskDto>> = callbackFlow {
        val db = getFirestore()
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection(FirestoreCollections.TENANTS)
            .document(tenantId)
            .collection(FirestoreCollections.TASKS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to cloud tasks", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tasks = snapshot.documents.mapNotNull { it.toObject(FirestoreTaskDto::class.java) }
                    trySend(tasks)
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Observes real-time transactions ledger from Firestore for a specific tenant.
     */
    fun observeCloudTransactions(tenantId: String): Flow<List<FirestoreTransactionDto>> = callbackFlow {
        val db = getFirestore()
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection(FirestoreCollections.TENANTS)
            .document(tenantId)
            .collection(FirestoreCollections.TRANSACTIONS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to cloud transactions", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val txs = snapshot.documents.mapNotNull { it.toObject(FirestoreTransactionDto::class.java) }
                    trySend(txs)
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Uploads or updates wallet state in Cloud Firestore under /tenants/{tenantId}/wallets/{userId}.
     */
    suspend fun syncWallet(wallet: UserWallet, userId: String = "user_default"): Result<Unit> {
        val db = getFirestore() ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val dto = FirestoreWalletDto.fromRoomEntity(wallet, userId)
            val docRef = db.collection(FirestoreCollections.TENANTS)
                .document(wallet.tenantId)
                .collection(FirestoreCollections.WALLETS)
                .document(userId)
            docRef.set(dto, SetOptions.merge()).await()
            Log.d(TAG, "Wallet synced to Firestore for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync wallet to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time wallet state from Firestore for a specific user.
     */
    fun observeCloudWallet(tenantId: String, userId: String): Flow<FirestoreWalletDto?> = callbackFlow {
        val db = getFirestore()
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = db.collection(FirestoreCollections.TENANTS)
            .document(tenantId)
            .collection(FirestoreCollections.WALLETS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to cloud wallet for $userId", error)
                    return@addSnapshotListener
                }
                val wallet = snapshot?.toObject(FirestoreWalletDto::class.java)
                trySend(wallet)
            }

        awaitClose {
            registration.remove()
        }
    }
}

