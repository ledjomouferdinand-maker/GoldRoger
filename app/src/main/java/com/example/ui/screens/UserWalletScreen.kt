package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreditTransaction
import com.example.data.model.SubscriptionTier
import com.example.data.model.TransactionStatus
import com.example.data.model.UserWallet
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WalletTxFilter(val label: String) {
    ALL("Toutes"),
    SPENDS("Tâches IA"),
    TOPUPS("Recharges"),
    SUBSCRIPTIONS("Abonnements"),
    ESCROW("Missions")
}

data class CreditPackItem(
    val name: String,
    val credits: Int,
    val priceEuros: Double,
    val tag: String?,
    val description: String,
    val isPopular: Boolean = false
)

/**
 * UserWalletScreen displaying:
 * 1. Current credit balance & subscription tier
 * 2. Recent transactions ledger with search & filtering
 * 3. Quick purchase buttons and a modal bottom sheet to initiate credit purchases
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserWalletScreen(
    wallet: UserWallet,
    transactions: List<CreditTransaction>,
    onPurchasePack: (String, Int, Double) -> Unit,
    onUpgradePro: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(WalletTxFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showPurchaseSheet by remember { mutableStateOf(false) }
    var selectedTxForDetails by remember { mutableStateOf<CreditTransaction?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    val creditPacks = remember {
        listOf(
            CreditPackItem(
                name = "Pack Starter",
                credits = 50,
                priceEuros = 29.0,
                tag = "Découverte",
                description = "Idéal pour 15 à 25 tâches (0,58€ / crédit)",
                isPopular = false
            ),
            CreditPackItem(
                name = "Pack Business",
                credits = 100,
                priceEuros = 50.0,
                tag = "Populaire (-15%)",
                description = "Recommandé TPE / Freelance (0,50€ / crédit)",
                isPopular = true
            ),
            CreditPackItem(
                name = "Pack Enterprise",
                credits = 300,
                priceEuros = 129.0,
                tag = "Meilleur Prix (-25%)",
                description = "Usage intensif multi-collaborateurs (0,43€ / crédit)",
                isPopular = false
            )
        )
    }

    val filteredTransactions = remember(transactions, selectedFilter, searchQuery) {
        transactions.filter { tx ->
            val typeStr = tx.type.uppercase()
            val matchesFilter = when (selectedFilter) {
                WalletTxFilter.ALL -> true
                WalletTxFilter.SPENDS -> typeStr == "SPEND" || tx.amountCredits < 0
                WalletTxFilter.TOPUPS -> typeStr == "TOPUP" || tx.amountCredits > 0
                WalletTxFilter.SUBSCRIPTIONS -> typeStr == "SUBSCRIPTION"
                WalletTxFilter.ESCROW -> typeStr.contains("ESCROW") || typeStr.contains("COMMISSION")
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                tx.title.contains(searchQuery, ignoreCase = true) ||
                        tx.id.toString().contains(searchQuery)
            }
            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("user_wallet_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // =====================================================================
        // 1. CREDIT BALANCE HERO CARD
        // =====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_balance_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1B4B)
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        listOf(IndigoPrimary, CyanSecondary, EmeraldTertiary)
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Header: Wallet icon, tenant & subscription badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(IndigoPrimary, CyanSecondary))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "TaskFlow Wallet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Organisation : ${wallet.tenantId}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Subscription Tier Tag
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when (wallet.subscriptionTier) {
                                SubscriptionTier.PRO -> EmeraldTertiary.copy(alpha = 0.25f)
                                SubscriptionTier.ENTERPRISE -> AmberAccent.copy(alpha = 0.25f)
                                SubscriptionTier.FREE -> Color(0xFF334155)
                            },
                            border = BorderStroke(
                                1.dp,
                                when (wallet.subscriptionTier) {
                                    SubscriptionTier.PRO -> EmeraldTertiary
                                    SubscriptionTier.ENTERPRISE -> AmberAccent
                                    SubscriptionTier.FREE -> Color(0xFF64748B)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (wallet.subscriptionTier == SubscriptionTier.PRO) Icons.Default.Star else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (wallet.subscriptionTier == SubscriptionTier.PRO) EmeraldTertiary else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (wallet.subscriptionTier == SubscriptionTier.PRO) "PRO (-30%)" else wallet.subscriptionTier.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (wallet.subscriptionTier == SubscriptionTier.PRO) EmeraldTertiary else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "SOLDE DE CRÉDITS D'USAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${wallet.creditsBalance}",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                ),
                                modifier = Modifier.testTag("wallet_balance_number")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "crédits dispos",
                                fontSize = 14.sp,
                                color = CyanSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Purchase Button
                        Button(
                            onClick = { showPurchaseSheet = true },
                            modifier = Modifier
                                .testTag("btn_open_purchase_modal")
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanSecondary,
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Recharger",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recharger",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats row: Time saved, completed tasks, total spent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = EmeraldTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Temps Gagné",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            val hours = wallet.totalTimeSavedMinutes / 60
                            val minutes = wallet.totalTimeSavedMinutes % 60
                            Text(
                                text = if (hours > 0) "${hours}h ${minutes}m" else "$minutes min",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CyanSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Automatisations",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = "${wallet.totalTasksCompleted} finies",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = AmberAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Total Acheté",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = "${wallet.totalSpentEuros.toInt()} €",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. PRO SUBSCRIPTION BANNER
        // =====================================================================
        if (wallet.subscriptionTier == SubscriptionTier.FREE) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_pro_upgrade_banner"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = IndigoPrimary.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.5.dp, IndigoPrimary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AmberAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Passer à TaskFlow Pro",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Économisez 30% sur tous les modules + 50 crédits offerts/mois.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        Button(
                            onClick = onUpgradePro,
                            modifier = Modifier
                                .testTag("btn_upgrade_pro_wallet")
                                .padding(start = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "49€ / mois",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 3. CREDIT PACKS QUICK ROW
        // =====================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recharger mes Crédits",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                TextButton(
                    onClick = { showPurchaseSheet = true },
                    modifier = Modifier.testTag("btn_view_all_packs")
                ) {
                    Text("Tous les packs", fontSize = 12.sp, color = CyanSecondary)
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(creditPacks) { pack ->
                    Card(
                        modifier = Modifier
                            .width(220.dp)
                            .testTag("quick_pack_${pack.name.replace(" ", "_").lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (pack.isPopular) Color(0xFF1E1B4B) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (pack.isPopular) CyanSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pack.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (pack.isPopular) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                pack.tag?.let { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (pack.isPopular) CyanSecondary else MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pack.isPopular) Color(0xFF0F172A) else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "+${pack.credits} cr",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = if (pack.isPopular) CyanSecondary else IndigoPrimary
                                )
                                Text(
                                    text = "${pack.priceEuros.toInt()} €",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = if (pack.isPopular) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pack.description,
                                fontSize = 10.sp,
                                color = if (pack.isPopular) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onPurchasePack(pack.name, pack.credits, pack.priceEuros) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pack.isPopular) CyanSecondary else IndigoPrimary,
                                    contentColor = if (pack.isPopular) Color(0xFF0F172A) else Color.White
                                )
                            ) {
                                Text("Acheter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 4. TRANSACTION HISTORY HEADER & FILTERS
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = CyanSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Historique des Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${filteredTransactions.size} logs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Rechercher une transaction ou module...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanSecondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )
        }

        // Filter chips row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("tx_filter_chips_row")
            ) {
                items(WalletTxFilter.values()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // =====================================================================
        // 5. TRANSACTION LIST
        // =====================================================================
        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Aucune transaction enregistrée",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Les déductions de crédits d'automatisation et recharges apparaîtront ici.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                val isCreditDeduction = tx.amountCredits < 0 || tx.type.equals("SPEND", ignoreCase = true)
                val isTopup = tx.amountCredits > 0 || tx.type.equals("TOPUP", ignoreCase = true)

                val icon: ImageVector = when {
                    isTopup -> Icons.Default.ArrowUpward
                    tx.type.equals("SUBSCRIPTION", ignoreCase = true) -> Icons.Default.Star
                    tx.type.contains("ESCROW", ignoreCase = true) -> Icons.Default.Lock
                    tx.type.contains("COMMISSION", ignoreCase = true) -> Icons.Default.Payments
                    isCreditDeduction -> Icons.Default.ArrowDownward
                    else -> Icons.Default.Refresh
                }

                val iconTint: Color = when {
                    isTopup -> EmeraldTertiary
                    tx.type.equals("SUBSCRIPTION", ignoreCase = true) -> AmberAccent
                    isCreditDeduction -> IndigoPrimary
                    else -> CyanSecondary
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTxForDetails = tx }
                        .testTag("tx_item_${tx.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(iconTint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = tx.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = dateFormatter.format(Date(tx.timestamp)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (tx.status) {
                                            TransactionStatus.SUCCEEDED -> EmeraldTertiary.copy(alpha = 0.15f)
                                            TransactionStatus.PENDING -> AmberAccent.copy(alpha = 0.15f)
                                            TransactionStatus.FAILED -> Color.Red.copy(alpha = 0.15f)
                                            TransactionStatus.REFUNDED -> CyanSecondary.copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = when (tx.status) {
                                                TransactionStatus.SUCCEEDED -> "Succès"
                                                TransactionStatus.PENDING -> "En cours"
                                                TransactionStatus.FAILED -> "Échec"
                                                TransactionStatus.REFUNDED -> "Remboursé"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (tx.status) {
                                                TransactionStatus.SUCCEEDED -> EmeraldTertiary
                                                TransactionStatus.PENDING -> AmberAccent
                                                TransactionStatus.FAILED -> Color.Red
                                                TransactionStatus.REFUNDED -> CyanSecondary
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (tx.amountCredits != 0) {
                                Text(
                                    text = if (tx.amountCredits > 0) "+${tx.amountCredits} cr" else "${tx.amountCredits} cr",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = if (tx.amountCredits > 0) EmeraldTertiary else IndigoPrimary
                                )
                            }
                            if (tx.amountEuros > 0) {
                                Text(
                                    text = "${String.format("%.2f", tx.amountEuros)} €",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = AmberAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // =========================================================================
    // 6. PURCHASE BOTTOM SHEET
    // =========================================================================
    if (showPurchaseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPurchaseSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            var selectedPack by remember { mutableStateOf(creditPacks[1]) }
            var selectedPaymentMethod by remember { mutableStateOf("Stripe / Carte") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("credit_purchase_sheet_content")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CyanSecondary, IndigoPrimary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Acheter des Crédits",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Solde actuel : ${wallet.creditsBalance} crédits",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            showPurchaseSheet = false
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Choisissez votre formule de recharge :",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    creditPacks.forEach { pack ->
                        val isSelected = selectedPack.name == pack.name
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPack = pack }
                                .testTag("sheet_pack_option_${pack.credits}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E1B4B) else Color(0xFF1E293B)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) CyanSecondary else Color(0xFF334155)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) CyanSecondary else Color(0xFF334155)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF0F172A),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = pack.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            pack.tag?.let { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = CyanSecondary.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = tag,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CyanSecondary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = pack.description,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+${pack.credits} cr",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = CyanSecondary
                                    )
                                    Text(
                                        text = "${pack.priceEuros.toInt()} €",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Moyen de paiement sécurisé :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Stripe / Carte", "Apple Pay", "Virement Pro").forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedPaymentMethod = method },
                            color = if (isSelected) IndigoPrimary.copy(alpha = 0.3f) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (isSelected) IndigoPrimary else Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = if (isSelected) CyanSecondary else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = method,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            showPurchaseSheet = false
                            onPurchasePack(selectedPack.name, selectedPack.credits, selectedPack.priceEuros)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_confirm_credit_purchase"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanSecondary,
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Payer ${selectedPack.priceEuros.toInt()} € (+${selectedPack.credits} crédits)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🔒 Paiement 100% sécurisé via Stripe SSL 256-bit • Facture instantanée.",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // =========================================================================
    // 7. TRANSACTION DETAIL DIALOG
    // =========================================================================
    selectedTxForDetails?.let { tx ->
        AlertDialog(
            onDismissRequest = { selectedTxForDetails = null },
            confirmButton = {
                TextButton(onClick = { selectedTxForDetails = null }) {
                    Text("Fermer", color = CyanSecondary)
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanSecondary
                    )
                    Text(
                        text = "Détail de la Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    TxDetailRow("Réf. Transaction :", "#${tx.id}")
                    TxDetailRow("Description :", tx.title)
                    TxDetailRow("Type :", tx.type)
                    TxDetailRow("Statut :", tx.status.name)
                    TxDetailRow("Date / Heure :", dateFormatter.format(Date(tx.timestamp)))
                    if (tx.amountCredits != 0) {
                        TxDetailRow(
                            "Variation Crédits :",
                            "${if (tx.amountCredits > 0) "+" else ""}${tx.amountCredits} crédits"
                        )
                    }
                    if (tx.amountEuros > 0) {
                        TxDetailRow("Montant TTC :", "${String.format("%.2f", tx.amountEuros)} €")
                    }
                    tx.stripePaymentIntentId?.let { pi ->
                        TxDetailRow("Stripe Ref :", pi)
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun TxDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1
        )
    }
}
