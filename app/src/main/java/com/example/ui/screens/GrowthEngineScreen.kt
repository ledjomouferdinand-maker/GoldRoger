package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserWallet
import com.example.ui.components.TaskCostPaywallCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary

enum class GrowthEngineTab(val label: String) {
    AD_CAMPAIGNS("1. Campagnes Pubs"),
    METRIC_ALERTS("2. Alertes Métriques"),
    EMAIL_MARKETING("3. Emails & Segments"),
    LEAD_ENRICHMENT("4. B2B Leads"),
    ROI_DASHBOARD("5. Dashboard ROI")
}

@Composable
fun GrowthEngineScreen(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecuteTask: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(GrowthEngineTab.AD_CAMPAIGNS) }
    val amberColor = AmberAccent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp
        ) {
            GrowthEngineTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab) amberColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("tab_growth_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            GrowthEngineTab.AD_CAMPAIGNS -> {
                var targetAudience by remember { mutableStateOf("Dirigeants de PME, Freelances B2B en France & Belgique") }
                var budgetDaily by remember { mutableStateOf("25") }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Créateur de Campagnes Pubs Multi-Canal (Meta & Google)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Génération automatique des titres, accroches, visuels recommandés et ciblage d'audience optimisé.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        OutlinedTextField(
                            value = targetAudience,
                            onValueChange = { targetAudience = it },
                            label = { Text("Cible / Persona") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = budgetDaily,
                            onValueChange = { budgetDaily = it },
                            label = { Text("Budget Journalier (€/jour)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 4,
                            timeSavedMinutes = 100,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "AD_CAMPAIGN_CREATE",
                                    "Campagne Publicitaire Meta & Google Ads",
                                    "Création de 4 variations d'annonces A/B test avec mots-clés négatifs et tracking pixel.",
                                    4,
                                    100,
                                    "Pack d'annonces généré avec 4 variantes de copywriting à fort CTR et audiences Lookalike.",
                                    false,
                                    "Marketing"
                                )
                            }
                        )
                    }
                }
            }

            GrowthEngineTab.METRIC_ALERTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Analyse Automatique des Métriques & Alertes IA", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Détection d'opportunités en temps réel :", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("📈 Hausse du taux de conversion (+24%) sur la landing page.", color = EmeraldTertiary, fontSize = 12.sp)
                                Text("⚠️ Coût par lead (CPL) en légère hausse (+8%) sur le canal LinkedIn Ads.", color = AmberAccent, fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 2,
                            timeSavedMinutes = 45,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "METRICS_ALERT_SCAN",
                                    "Scan & Alertes Métriques Marketing",
                                    "Détection d'anomalies de tracking et recommandations d'optimisation de budget.",
                                    2,
                                    45,
                                    "Rapport d'optimisation généré : 2 campagnes ajustées pour économiser 15% de budget gaspillé.",
                                    false,
                                    "Marketing"
                                )
                            }
                        )
                    }
                }
            }

            GrowthEngineTab.EMAIL_MARKETING -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Segmentation & Séquences d'Emails Marketing", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 3,
                            timeSavedMinutes = 80,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "EMAIL_SEQUENCE_GEN",
                                    "Séquence Nurturing (5 Emails)",
                                    "Séquence de bienvenue automatisée avec storytelling et conversion progressive.",
                                    3,
                                    80,
                                    "5 emails rédigés avec objets à haut taux d'ouverture (>45% estimé).",
                                    false,
                                    "Marketing"
                                )
                            }
                        )
                    }
                }
            }

            GrowthEngineTab.LEAD_ENRICHMENT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Génération de Leads Qualifiés B2B & Enrichissement", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Extraction éthique d'emails vérifiés, postes et profils d'entreprises cibles.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 5,
                            timeSavedMinutes = 150,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "LEAD_SCRAPING",
                                    "Extraction 50 Leads Qualifiés B2B",
                                    "Recherche de décideurs ciblés avec emails professionnels vérifiés et scores de pertinence.",
                                    5,
                                    150,
                                    "Fichier CSV de 50 prospects B2B vérifiés (98% de délivrabilité garantie).",
                                    true,
                                    "Leads"
                                )
                            }
                        )
                    }
                }
            }

            GrowthEngineTab.ROI_DASHBOARD -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Dashboard ROI Multi-Canal en Temps Réel", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ROAS Global :", fontWeight = FontWeight.Bold)
                                    Text("4.8x", color = EmeraldTertiary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• Google Search : ROAS 5.2x (CAC 14.50€)", fontSize = 12.sp)
                                Text("• Meta Ads : ROAS 4.3x (CAC 19.80€)", fontSize = 12.sp)
                                Text("• Emailing : ROAS 8.1x (CAC 2.10€)", fontSize = 12.sp, color = EmeraldTertiary)
                            }
                        }
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 2,
                            timeSavedMinutes = 35,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "ROI_ATTRIBUTION_SYNC",
                                    "Calcul Attribution Multi-Touch",
                                    "Mise à jour des modèles d'attribution au premier et dernier clic.",
                                    2,
                                    35,
                                    "Rapport ROI consolidé exporté avec matrice d'attribution par canal.",
                                    false,
                                    "Marketing"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
