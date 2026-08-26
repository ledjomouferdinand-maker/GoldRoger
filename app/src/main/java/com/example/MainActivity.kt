package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firestore.FirebaseFirestoreManager
import com.example.data.model.ModuleType
import com.example.ui.components.GeminiSmartSuggestionCard
import com.example.ui.components.TaskFlowBottomNav
import com.example.ui.components.TaskFlowTopBar
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ArchitectureBlueprintScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ContentStudioScreen
import com.example.ui.screens.FreelanceHubScreen
import com.example.ui.screens.GrowthEngineScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PaperasseExpressScreen
import com.example.ui.screens.TaskDashboardScreen
import com.example.ui.screens.UserWalletScreen
import com.example.ui.screens.WalletAndPricingScreen
import com.example.ui.screens.WebLaunchScreen
import com.example.ui.theme.TaskFlowTheme
import com.example.ui.viewmodel.AuthenticationViewModel
import com.example.ui.viewmodel.ScreenDestination
import com.example.ui.viewmodel.TaskFlowViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseFirestoreManager.initialize(this)
        enableEdgeToEdge()
        setContent {
            TaskFlowTheme {
                TaskFlowApp()
            }
        }
    }
}

@Composable
fun TaskFlowApp(
    viewModel: TaskFlowViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val wallet by viewModel.userWallet.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val adminMetrics by viewModel.adminMetrics.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecutingTask.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TaskFlowTopBar(
                currentScreen = currentScreen,
                wallet = wallet,
                onNavigateBack = { viewModel.navigateTo(ScreenDestination.Home) },
                onOpenWallet = { viewModel.navigateTo(ScreenDestination.WalletAndPricing) },
                onOpenAdmin = { viewModel.navigateTo(ScreenDestination.AdminDashboard) },
                onOpenArchitecture = { viewModel.navigateTo(ScreenDestination.ArchitectureBlueprint) },
                onOpenAuth = { viewModel.navigateTo(ScreenDestination.Auth) },
                onOpenTaskDashboard = { viewModel.navigateTo(ScreenDestination.TaskDashboard) }
            )
        },
        bottomBar = {
            TaskFlowBottomNav(
                currentScreen = currentScreen,
                onNavigate = { dest -> viewModel.navigateTo(dest) }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is ScreenDestination.Home -> {
                    HomeScreen(
                        wallet = wallet,
                        tasks = tasks,
                        onSelectModule = { mod -> viewModel.navigateTo(ScreenDestination.ModuleView(mod)) },
                        onOpenWallet = { viewModel.navigateTo(ScreenDestination.WalletAndPricing) },
                        onOpenAdmin = { viewModel.navigateTo(ScreenDestination.AdminDashboard) },
                        onExecuteQuickTask = { mod, taskTitle, credits, timeSaved ->
                            viewModel.executeModuleTask(
                                moduleId = mod.id,
                                taskType = "QUICK_AUTOMATION",
                                title = taskTitle,
                                description = "Tâche 1-Clic automatisée : $taskTitle",
                                baseCreditCost = credits,
                                timeSavedMinutes = timeSaved,
                                generatedPayload = "Résultat généré pour $taskTitle avec succès."
                            )
                        },
                        onOpenTaskDashboard = { viewModel.navigateTo(ScreenDestination.TaskDashboard) }
                    )
                }

                is ScreenDestination.ModuleView -> {
                    when (screen.module) {
                        ModuleType.PAPERASSE_EXPRESS -> {
                            PaperasseExpressScreen(
                                wallet = wallet,
                                documents = documents,
                                isProcessing = isExecuting,
                                onExecuteTask = { type, title, desc, cost, timeSaved, payload, toSafe, cat ->
                                    viewModel.executeModuleTask(
                                        moduleId = screen.module.id,
                                        taskType = type,
                                        title = title,
                                        description = desc,
                                        baseCreditCost = cost,
                                        timeSavedMinutes = timeSaved,
                                        generatedPayload = payload,
                                        createSafeDoc = toSafe,
                                        docCategory = cat
                                    )
                                },
                                onBookEscalation = { service, expert, quote ->
                                    viewModel.bookEscalation(service, expert, quote)
                                },
                                onDeleteDoc = { id ->
                                    viewModel.deleteSafeDocument(id)
                                }
                            )
                        }

                        ModuleType.CONTENT_STUDIO -> {
                            ContentStudioScreen(
                                wallet = wallet,
                                isProcessing = isExecuting,
                                onExecuteTask = { type, title, desc, cost, timeSaved, payload, toSafe, cat ->
                                    viewModel.executeModuleTask(
                                        moduleId = screen.module.id,
                                        taskType = type,
                                        title = title,
                                        description = desc,
                                        baseCreditCost = cost,
                                        timeSavedMinutes = timeSaved,
                                        generatedPayload = payload,
                                        createSafeDoc = toSafe,
                                        docCategory = cat
                                    )
                                }
                            )
                        }

                        ModuleType.WEB_LAUNCH -> {
                            WebLaunchScreen(
                                wallet = wallet,
                                isProcessing = isExecuting,
                                onExecuteTask = { type, title, desc, cost, timeSaved, payload, toSafe, cat ->
                                    viewModel.executeModuleTask(
                                        moduleId = screen.module.id,
                                        taskType = type,
                                        title = title,
                                        description = desc,
                                        baseCreditCost = cost,
                                        timeSavedMinutes = timeSaved,
                                        generatedPayload = payload,
                                        createSafeDoc = toSafe,
                                        docCategory = cat
                                    )
                                }
                            )
                        }

                        ModuleType.FREELANCE_HUB -> {
                            FreelanceHubScreen(
                                wallet = wallet,
                                isProcessing = isExecuting,
                                onExecuteTask = { type, title, desc, cost, timeSaved, payload, toSafe, cat ->
                                    viewModel.executeModuleTask(
                                        moduleId = screen.module.id,
                                        taskType = type,
                                        title = title,
                                        description = desc,
                                        baseCreditCost = cost,
                                        timeSavedMinutes = timeSaved,
                                        generatedPayload = payload,
                                        createSafeDoc = toSafe,
                                        docCategory = cat
                                    )
                                },
                                onBookEscalation = { service, expert, quote ->
                                    viewModel.bookEscalation(service, expert, quote)
                                }
                            )
                        }

                        ModuleType.GROWTH_ENGINE -> {
                            GrowthEngineScreen(
                                wallet = wallet,
                                isProcessing = isExecuting,
                                onExecuteTask = { type, title, desc, cost, timeSaved, payload, toSafe, cat ->
                                    viewModel.executeModuleTask(
                                        moduleId = screen.module.id,
                                        taskType = type,
                                        title = title,
                                        description = desc,
                                        baseCreditCost = cost,
                                        timeSavedMinutes = timeSaved,
                                        generatedPayload = payload,
                                        createSafeDoc = toSafe,
                                        docCategory = cat
                                    )
                                }
                            )
                        }
                    }
                }

                is ScreenDestination.TaskDashboard -> {
                    TaskDashboardScreen(
                        localTasks = tasks,
                        activeTenantId = "tenant_alpha",
                        onSelectModule = { mod ->
                            viewModel.navigateTo(ScreenDestination.ModuleView(mod))
                        },
                        onExecuteQuickTask = { mod, taskTitle, credits, timeSaved ->
                            viewModel.executeModuleTask(
                                moduleId = mod.id,
                                taskType = "DASHBOARD_AUTOMATION",
                                title = taskTitle,
                                description = "Tâche exécutée depuis le Dashboard : $taskTitle",
                                baseCreditCost = credits,
                                timeSavedMinutes = timeSaved,
                                generatedPayload = "Résultat généré pour $taskTitle avec succès."
                            )
                        },
                        onNavigateBack = {
                            viewModel.navigateTo(ScreenDestination.Home)
                        }
                    )
                }

                is ScreenDestination.WalletAndPricing -> {
                    UserWalletScreen(
                        wallet = wallet,
                        transactions = transactions,
                        onPurchasePack = { name, credits, price ->
                            viewModel.purchaseCredits(name, credits, price)
                        },
                        onUpgradePro = {
                            viewModel.upgradeSubscriptionToPro()
                        }
                    )
                }

                is ScreenDestination.AdminDashboard -> {
                    AdminDashboardScreen(
                        metrics = adminMetrics,
                        onUpdateCommissionRate = { rate ->
                            viewModel.updatePlatformCommissionRate(rate)
                        }
                    )
                }

                is ScreenDestination.ArchitectureBlueprint -> {
                    ArchitectureBlueprintScreen()
                }

                is ScreenDestination.Auth -> {
                    val authViewModel: AuthenticationViewModel = viewModel()
                    LoginScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = {
                            viewModel.navigateTo(ScreenDestination.Home)
                        },
                        onNavigateBack = {
                            viewModel.navigateTo(ScreenDestination.Home)
                        }
                    )
                }
            }
        }
    }
}
