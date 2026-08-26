package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CreditTransaction
import com.example.data.model.DigitalDocument
import com.example.data.model.ModuleEntity
import com.example.data.model.ModuleType
import com.example.data.model.SubscriptionTier
import com.example.data.model.TaskRecord
import com.example.data.model.TaskStatus
import com.example.data.model.TenantEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.UserWallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TenantEntity::class,
        UserEntity::class,
        ModuleEntity::class,
        TaskRecord::class,
        DigitalDocument::class,
        UserWallet::class,
        CreditTransaction::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TaskFlowDatabase : RoomDatabase() {
    abstract fun taskFlowDao(): TaskFlowDao

    companion object {
        @Volatile
        private var INSTANCE: TaskFlowDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TaskFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskFlowDatabase::class.java,
                    "taskflow_pro.db"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.taskFlowDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: TaskFlowDao) {
            val now = System.currentTimeMillis()

            // 1. Initial Multi-Tenant Organizations
            val tenant1 = TenantEntity(
                tenantId = "tenant_alpha",
                name = "Agence Alpha & Co",
                slug = "agence-alpha",
                plan = SubscriptionTier.PRO,
                maxUsers = 5,
                creditsBalance = 58,
                storageQuotaBytes = 5368709120L, // 5 GB
                stripeCustomerId = "cus_alpha_9817234",
                createdAt = now - (60L * 24 * 3600 * 1000)
            )

            val tenant2 = TenantEntity(
                tenantId = "tenant_solo",
                name = "Studio Freelance Solo",
                slug = "studio-solo",
                plan = SubscriptionTier.FREE,
                maxUsers = 1,
                creditsBalance = 24,
                storageQuotaBytes = 1073741824L, // 1 GB
                stripeCustomerId = "cus_solo_1029384",
                createdAt = now - (15L * 24 * 3600 * 1000)
            )

            dao.insertTenant(tenant1)
            dao.insertTenant(tenant2)

            // 2. Initial Users per Tenant
            val user1 = UserEntity(
                userId = "user_alex",
                tenantId = "tenant_alpha",
                email = "alexandre.founder@agencealpha.fr",
                displayName = "Alexandre Dupont",
                role = UserRole.OWNER,
                creditsBalance = 58,
                totalTasksCompleted = 18,
                totalTimeSavedMinutes = 720,
                totalSpentEuros = 145.0,
                createdAt = now - (60L * 24 * 3600 * 1000)
            )

            val user2 = UserEntity(
                userId = "user_claire",
                tenantId = "tenant_alpha",
                email = "claire.marketing@agencealpha.fr",
                displayName = "Claire Morel",
                role = UserRole.MEMBER,
                creditsBalance = 58,
                totalTasksCompleted = 6,
                totalTimeSavedMinutes = 240,
                totalSpentEuros = 0.0,
                createdAt = now - (30L * 24 * 3600 * 1000)
            )

            val user3 = UserEntity(
                userId = "user_julien",
                tenantId = "tenant_solo",
                email = "julien.freelance@gmail.com",
                displayName = "Julien Bernard",
                role = UserRole.OWNER,
                creditsBalance = 24,
                totalTasksCompleted = 5,
                totalTimeSavedMinutes = 180,
                totalSpentEuros = 29.0,
                createdAt = now - (15L * 24 * 3600 * 1000)
            )

            dao.insertUser(user1)
            dao.insertUser(user2)
            dao.insertUser(user3)

            // 3. Initial Global Modules Catalog
            ModuleType.values().forEach { moduleType ->
                dao.insertModule(
                    ModuleEntity(
                        moduleId = moduleType.id,
                        tenantId = "global",
                        title = moduleType.title,
                        subtitle = moduleType.subtitle,
                        description = "Module d'automatisation IA et d'escalade d'experts pour ${moduleType.subtitle}.",
                        iconName = moduleType.iconName,
                        primaryColorHex = moduleType.primaryColorHex,
                        baseCreditCost = moduleType.defaultCost,
                        estimatedTimeSavedMinutes = when (moduleType) {
                            ModuleType.PAPERASSE_EXPRESS -> 90
                            ModuleType.CONTENT_STUDIO -> 120
                            ModuleType.WEB_LAUNCH -> 75
                            ModuleType.FREELANCE_HUB -> 60
                            ModuleType.GROWTH_ENGINE -> 90
                        },
                        category = moduleType.category.label,
                        isEnabled = true,
                        supportedTaskTypesJson = "[\"GENERATE\", \"AUDIT\", \"OPTIMIZE\", \"ESCALATE\"]",
                        version = "1.2.0"
                    )
                )
            }

            // 4. Initial Wallet (Local quick state for Tenant Alpha)
            dao.insertOrUpdateWallet(
                UserWallet(
                    id = 1,
                    tenantId = "tenant_alpha",
                    creditsBalance = 42,
                    subscriptionTier = SubscriptionTier.FREE,
                    totalTimeSavedMinutes = 580,
                    totalTasksCompleted = 8,
                    totalSpentEuros = 45.0
                )
            )

            // 5. Initial Digital Safe Documents (Partitioned by Tenant)
            dao.insertDocument(
                DigitalDocument(
                    tenantId = "tenant_alpha",
                    userId = "user_alex",
                    title = "Contrat de Prestation Dev SaaS",
                    category = "Contrat",
                    fileFormat = "PDF",
                    contentSummary = "Contrat de développement logiciel avec clause de cession de PI conforme droit FR.",
                    isVerifiedLegal = true,
                    expirationDateMillis = now + (180L * 24 * 60 * 60 * 1000)
                )
            )
            dao.insertDocument(
                DigitalDocument(
                    tenantId = "tenant_alpha",
                    userId = "user_alex",
                    title = "Déclaration URSSAF T2",
                    category = "URSSAF",
                    fileFormat = "PDF",
                    contentSummary = "Déclaration trimestrielle auto-entrepreneur calculée avec abattement BNC (22%).",
                    isVerifiedLegal = true,
                    expirationDateMillis = now + (45L * 24 * 60 * 60 * 1000)
                )
            )
            dao.insertDocument(
                DigitalDocument(
                    tenantId = "tenant_alpha",
                    userId = "user_alex",
                    title = "Facture Client Acme Corp #FAC-2026-08",
                    category = "Facture",
                    fileFormat = "PDF",
                    contentSummary = "Facture proforme conforme Chorus Pro avec mentions légales et TVA intracommunautaire.",
                    isVerifiedLegal = true,
                    expirationDateMillis = null
                )
            )

            // 6. Initial Tasks Partitioned
            dao.insertTask(
                TaskRecord(
                    tenantId = "tenant_alpha",
                    userId = "user_alex",
                    moduleId = "paperasse",
                    taskType = "GEN_DOC",
                    title = "Génération Contrat Freelance",
                    description = "Génération d'un contrat de prestation avec clause de confidentialité et pénalités de retard.",
                    creditsCost = 2,
                    timeSavedMinutes = 90,
                    status = TaskStatus.COMPLETED,
                    generatedResult = "Contrat de Prestation de Services intellectuels généré avec succès. Signatures prêtes via DocuSign/YouSign."
                )
            )
            dao.insertTask(
                TaskRecord(
                    tenantId = "tenant_alpha",
                    userId = "user_claire",
                    moduleId = "content",
                    taskType = "REPURPOSE_10X",
                    title = "Repurposing Article de Blog SaaS",
                    description = "Transformation d'un article de blog en 1 Thread X, 1 Carousel LinkedIn et 3 Scripts Vidéo TikTok.",
                    creditsCost = 4,
                    timeSavedMinutes = 120,
                    status = TaskStatus.COMPLETED,
                    generatedResult = "10 formats dérivés générés : 1 thread Twitter/X (7 tweets), 1 carousel LinkedIn (5 slides), 3 scripts Reels/Shorts."
                )
            )
            dao.insertTask(
                TaskRecord(
                    tenantId = "tenant_alpha",
                    userId = "user_alex",
                    moduleId = "weblaunch",
                    taskType = "AUDIT_SEO",
                    title = "Audit Technique & SEO vitrine",
                    description = "Analyse Core Web Vitals, balises meta et conformité RGPD.",
                    creditsCost = 3,
                    timeSavedMinutes = 75,
                    status = TaskStatus.COMPLETED,
                    generatedResult = "Score Lighthouse : Performance 98/100, Accessibilité 100/100, SEO 96/100. 2 recommandations appliquées."
                )
            )

            // 7. Initial Transactions Partitioned
            dao.insertTransaction(
                CreditTransaction(
                    tenantId = "tenant_alpha",
                    userId = "user_alex",
                    title = "Pack Starter (50 Crédits)",
                    amountCredits = 50,
                    amountEuros = 29.0,
                    type = "TOPUP",
                    status = TransactionStatus.SUCCEEDED
                )
            )
            dao.insertTransaction(
                CreditTransaction(
                    tenantId = "tenant_alpha",
                    userId = "user_alex",
                    title = "Exécution Paperasse Express",
                    amountCredits = -2,
                    amountEuros = 0.0,
                    type = "SPEND",
                    status = TransactionStatus.SUCCEEDED
                )
            )
            dao.insertTransaction(
                CreditTransaction(
                    tenantId = "tenant_alpha",
                    userId = "user_claire",
                    title = "Exécution Content Studio (10x Repurposing)",
                    amountCredits = -4,
                    amountEuros = 0.0,
                    type = "SPEND",
                    status = TransactionStatus.SUCCEEDED
                )
            )
        }
    }
}
