package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ModuleType
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserWallet
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.ScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFlowTopBar(
    currentScreen: ScreenDestination,
    wallet: UserWallet,
    onNavigateBack: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenArchitecture: () -> Unit,
    onOpenAuth: () -> Unit = {},
    onOpenTaskDashboard: () -> Unit = {}
) {
    val title = when (currentScreen) {
        is ScreenDestination.Home -> "TaskFlow Pro"
        is ScreenDestination.ModuleView -> currentScreen.module.title
        is ScreenDestination.TaskDashboard -> "Tableau de Bord des Tâches"
        is ScreenDestination.WalletAndPricing -> "Mon Portefeuille & Tarifs"
        is ScreenDestination.AdminDashboard -> "Dashboard Revenus (Admin)"
        is ScreenDestination.ArchitectureBlueprint -> "Architecture & Spécifications"
        is ScreenDestination.Auth -> "Authentification & Sécurité"
    }

    val subtitle = when (currentScreen) {
        is ScreenDestination.Home -> "Automatisation SaaS & Rémunération"
        is ScreenDestination.ModuleView -> currentScreen.module.subtitle
        is ScreenDestination.TaskDashboard -> "Cloud Firestore Live & Progression"
        is ScreenDestination.WalletAndPricing -> "Crédits TaskFlow & Abonnements"
        is ScreenDestination.AdminDashboard -> "MRR, Commissions & Projections"
        is ScreenDestination.ArchitectureBlueprint -> "Livrables Techniques & Stack"
        is ScreenDestination.Auth -> "Firebase Auth & Credential Manager"
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            if (currentScreen !is ScreenDestination.Home) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("topbar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Retour"
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(IndigoPrimary, CyanSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        },
        actions = {
            // Credits balance pill (Interactive)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onOpenWallet() }
                    .testTag("wallet_credit_pill"),
                color = if (wallet.creditsBalance < 10) AmberAccent.copy(alpha = 0.15f) else IndigoPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Crédits",
                        modifier = Modifier.size(16.dp),
                        tint = if (wallet.creditsBalance < 10) AmberAccent else IndigoPrimary
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${wallet.creditsBalance} cr",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (wallet.creditsBalance < 10) AmberAccent else IndigoPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Pro badge
            if (wallet.subscriptionTier == SubscriptionTier.PRO) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldTertiary)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            // Quick access to Task Dashboard
            IconButton(
                onClick = onOpenTaskDashboard,
                modifier = Modifier.testTag("btn_open_tasks")
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = "Tableau de Bord des Tâches",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Quick access to Blueprint
            IconButton(
                onClick = onOpenArchitecture,
                modifier = Modifier.testTag("btn_open_architecture")
            ) {
                Icon(
                    imageVector = Icons.Default.Architecture,
                    contentDescription = "Spécifications Architecture",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Quick access to Auth & Profile
            IconButton(
                onClick = onOpenAuth,
                modifier = Modifier.testTag("btn_open_auth")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Mon Compte & Sécurité",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
fun TaskFlowBottomNav(
    currentScreen: ScreenDestination,
    onNavigate: (ScreenDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen is ScreenDestination.Home,
            onClick = { onNavigate(ScreenDestination.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Accueil") },
            label = { Text("Hub", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = IndigoPrimary,
                indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_home")
        )

        NavigationBarItem(
            selected = currentScreen is ScreenDestination.ModuleView && currentScreen.module == ModuleType.PAPERASSE_EXPRESS,
            onClick = { onNavigate(ScreenDestination.ModuleView(ModuleType.PAPERASSE_EXPRESS)) },
            icon = { Icon(Icons.Default.Description, contentDescription = "Paperasse Express") },
            label = { Text("Légal", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(ModuleType.PAPERASSE_EXPRESS.primaryColorHex),
                indicatorColor = Color(ModuleType.PAPERASSE_EXPRESS.primaryColorHex).copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_module_paperasse")
        )

        NavigationBarItem(
            selected = currentScreen is ScreenDestination.WalletAndPricing,
            onClick = { onNavigate(ScreenDestination.WalletAndPricing) },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Portefeuille") },
            label = { Text("Wallet", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = EmeraldTertiary,
                indicatorColor = EmeraldTertiary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_wallet")
        )

        NavigationBarItem(
            selected = currentScreen is ScreenDestination.AdminDashboard,
            onClick = { onNavigate(ScreenDestination.AdminDashboard) },
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Dashboard Revenus") },
            label = { Text("Revenus", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AmberAccent,
                indicatorColor = AmberAccent.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_admin")
        )

        NavigationBarItem(
            selected = currentScreen is ScreenDestination.ArchitectureBlueprint,
            onClick = { onNavigate(ScreenDestination.ArchitectureBlueprint) },
            icon = { Icon(Icons.Default.Architecture, contentDescription = "Architecture") },
            label = { Text("Arch.", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyanSecondary,
                indicatorColor = CyanSecondary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_architecture")
        )
    }
}
