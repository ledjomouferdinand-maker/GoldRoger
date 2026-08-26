package com.example.data.firestore.repository

import com.example.data.firestore.dao.FirestoreTaskFlowDao
import com.example.data.firestore.model.FirestoreModule
import com.example.data.firestore.model.FirestoreTask
import com.example.data.firestore.model.FirestoreTenant
import com.example.data.firestore.model.FirestoreTransaction
import com.example.data.firestore.model.FirestoreUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * High-level Cloud Firestore Repository supporting multi-tenancy.
 * Uses tenant IDs and user IDs to partition data cleanly in cloud storage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTaskFlowRepository(
    private val firestoreDao: FirestoreTaskFlowDao
) {
    // Current multi-tenant active context
    private val _activeTenantId = MutableStateFlow("tenant_alpha")
    val activeTenantId = _activeTenantId.asStateFlow()

    private val _activeUserId = MutableStateFlow("user_alex")
    val activeUserId = _activeUserId.asStateFlow()

    fun setContext(tenantId: String, userId: String) {
        _activeTenantId.value = tenantId
        _activeUserId.value = userId
    }

    // =========================================================================
    // REACTIVE STREAMS (Scoped to Active Tenant & User)
    // =========================================================================

    val currentUser: Flow<FirestoreUser?> = _activeTenantId.flatMapLatest { tenantId ->
        _activeUserId.flatMapLatest { userId ->
            firestoreDao.observeUser(tenantId, userId)
        }
    }

    val currentTenant: Flow<FirestoreTenant?> = _activeTenantId.flatMapLatest { tenantId ->
        firestoreDao.observeTenant(tenantId)
    }

    val tenantUsers: Flow<List<FirestoreUser>> = _activeTenantId.flatMapLatest { tenantId ->
        firestoreDao.observeTenantUsers(tenantId)
    }

    val tenantModules: Flow<List<FirestoreModule>> = _activeTenantId.flatMapLatest { tenantId ->
        firestoreDao.observeTenantModules(tenantId)
    }

    val userTasks: Flow<List<FirestoreTask>> = _activeTenantId.flatMapLatest { tenantId ->
        _activeUserId.flatMapLatest { userId ->
            firestoreDao.observeTasksByUser(tenantId, userId)
        }
    }

    val tenantTasks: Flow<List<FirestoreTask>> = _activeTenantId.flatMapLatest { tenantId ->
        firestoreDao.observeTenantTasks(tenantId)
    }

    val userTransactions: Flow<List<FirestoreTransaction>> = _activeTenantId.flatMapLatest { tenantId ->
        _activeUserId.flatMapLatest { userId ->
            firestoreDao.observeTransactionsByUser(tenantId, userId)
        }
    }

    val tenantTransactions: Flow<List<FirestoreTransaction>> = _activeTenantId.flatMapLatest { tenantId ->
        firestoreDao.observeTenantTransactions(tenantId)
    }

    // =========================================================================
    // CRUD & WORKFLOW ACTIONS
    // =========================================================================

    suspend fun saveUser(user: FirestoreUser): Result<Unit> {
        return firestoreDao.saveUser(user)
    }

    suspend fun saveModule(module: FirestoreModule): Result<Unit> {
        return firestoreDao.saveModule(module)
    }

    suspend fun saveTask(task: FirestoreTask): Result<String> {
        return firestoreDao.saveTask(task)
    }

    suspend fun recordTransaction(transaction: FirestoreTransaction): Result<String> {
        return firestoreDao.saveTransaction(transaction)
    }

    suspend fun saveTenant(tenant: FirestoreTenant): Result<Unit> {
        return firestoreDao.saveTenant(tenant)
    }

    suspend fun executeTaskAtomically(
        task: FirestoreTask,
        transaction: FirestoreTransaction
    ): Result<Unit> {
        return firestoreDao.executeTaskWithCreditsDeduction(task, transaction)
    }
}
