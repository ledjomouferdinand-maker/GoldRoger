package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CreditTransaction
import com.example.data.model.DigitalDocument
import com.example.data.model.ModuleEntity
import com.example.data.model.TaskRecord
import com.example.data.model.TenantEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserWallet
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskFlowDao {

    // ==========================================
    // TENANTS (Multi-Tenant Organization partition)
    // ==========================================
    @Query("SELECT * FROM tenants ORDER BY createdAt DESC")
    fun getAllTenants(): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants WHERE tenantId = :tenantId")
    fun getTenantById(tenantId: String): Flow<TenantEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: TenantEntity)

    @Update
    suspend fun updateTenant(tenant: TenantEntity)

    @Query("DELETE FROM tenants WHERE tenantId = :tenantId")
    suspend fun deleteTenant(tenantId: String)

    // ==========================================
    // USERS (Multi-Tenant Membership)
    // ==========================================
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE tenantId = :tenantId ORDER BY createdAt DESC")
    fun getUsersByTenant(tenantId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUser(userId: String)

    // ==========================================
    // MODULES (Catalog & Tenant Overrides)
    // ==========================================
    @Query("SELECT * FROM modules ORDER BY title ASC")
    fun getAllModules(): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE tenantId = :tenantId OR tenantId = 'global' ORDER BY title ASC")
    fun getModulesForTenant(tenantId: String): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE moduleId = :moduleId LIMIT 1")
    fun getModuleById(moduleId: String): Flow<ModuleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: ModuleEntity)

    // ==========================================
    // TASKS (Multi-Tenant Partitioned)
    // ==========================================
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskRecord>>

    @Query("SELECT * FROM tasks WHERE tenantId = :tenantId ORDER BY createdAt DESC")
    fun getTasksByTenant(tenantId: String): Flow<List<TaskRecord>>

    @Query("SELECT * FROM tasks WHERE moduleId = :moduleId ORDER BY createdAt DESC")
    fun getTasksByModule(moduleId: String): Flow<List<TaskRecord>>

    @Query("SELECT * FROM tasks WHERE tenantId = :tenantId AND moduleId = :moduleId ORDER BY createdAt DESC")
    fun getTasksByTenantAndModule(tenantId: String, moduleId: String): Flow<List<TaskRecord>>

    @Query("SELECT * FROM tasks WHERE tenantId = :tenantId AND userId = :userId ORDER BY createdAt DESC")
    fun getTasksByUser(tenantId: String, userId: String): Flow<List<TaskRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskRecord): Long

    @Update
    suspend fun updateTask(task: TaskRecord)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE tenantId = :tenantId")
    fun getTaskCountForTenant(tenantId: String): Flow<Int>

    // ==========================================
    // DIGITAL VAULT DOCUMENTS (Multi-Tenant Partitioned)
    // ==========================================
    @Query("SELECT * FROM digital_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DigitalDocument>>

    @Query("SELECT * FROM digital_documents WHERE tenantId = :tenantId ORDER BY createdAt DESC")
    fun getDocumentsByTenant(tenantId: String): Flow<List<DigitalDocument>>

    @Query("SELECT * FROM digital_documents WHERE tenantId = :tenantId AND category = :category ORDER BY createdAt DESC")
    fun getDocumentsByTenantAndCategory(tenantId: String, category: String): Flow<List<DigitalDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DigitalDocument): Long

    @Query("DELETE FROM digital_documents WHERE id = :docId")
    suspend fun deleteDocument(docId: Long)

    // ==========================================
    // WALLET & LEGACY LOCAL CACHE
    // ==========================================
    @Query("SELECT * FROM wallet WHERE id = 1")
    fun getWallet(): Flow<UserWallet?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWallet(wallet: UserWallet)

    @Update
    suspend fun updateWallet(wallet: UserWallet)

    // ==========================================
    // TRANSACTIONS & FINANCIAL LEDGER (Multi-Tenant Partitioned)
    // ==========================================
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<CreditTransaction>>

    @Query("SELECT * FROM transactions WHERE tenantId = :tenantId ORDER BY timestamp DESC")
    fun getTransactionsByTenant(tenantId: String): Flow<List<CreditTransaction>>

    @Query("SELECT * FROM transactions WHERE tenantId = :tenantId AND userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsByUser(tenantId: String, userId: String): Flow<List<CreditTransaction>>

    @Query("SELECT SUM(amountEuros) FROM transactions WHERE tenantId = :tenantId")
    fun getTotalSpentForTenant(tenantId: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CreditTransaction): Long
}
