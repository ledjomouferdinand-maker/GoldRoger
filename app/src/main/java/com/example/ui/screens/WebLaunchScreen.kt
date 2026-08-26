package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldTertiary

enum class WebLaunchTab(val label: String) {
    LANDING_BUILDER("1. Landing Page"),
    AUDIT_TECH("2. Audit SEO & Vitesse"),
    CONNECTORS("3. Connecteurs"),
    CLOUD_DEPLOY("4. Déploiement"),
    MAINTENANCE("5. Maintenance")
}

@Composable
fun WebLaunchScreen(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecuteTask: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(WebLaunchTab.LANDING_BUILDER) }
    val cyanColor = CyanSecondary

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
            WebLaunchTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab) cyanColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("tab_weblaunch_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            WebLaunchTab.LANDING_BUILDER -> {
                var businessName by remember { mutableStateOf("NovaPay SaaS") }
                var valueProp by remember { mutableStateOf("La passerelle de paiement automatisée pour freelances") }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Générateur de Landing Page & Vitrine No-Code", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Questionnaire guidé transformé en code web responsive prêt à l'emploi (Tailwind + React ou HTML5).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Nom du projet / Marque") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = valueProp,
                            onValueChange = { valueProp = it },
                            label = { Text("Proposition de valeur principale (Hook)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 6,
                            timeSavedMinutes = 240,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "LANDING_BUILD",
                                    "Génération Landing Page ($businessName)",
                                    "Création complète d'une landing page haute conversion avec sections Hero, Features, Pricing et CTA.",
                                    6,
                                    240,
                                    "Code source prêt au déploiement (HTML/Tailwind + React component). 100% responsive et SEO-friendly.",
                                    false,
                                    "Web"
                                )
                            }
                        )
                    }
                }
            }

            WebLaunchTab.AUDIT_TECH -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Audit Technique Automatique (Lighthouse & SEO)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Score Lighthouse Prévisionnel :", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("⚡ Perf: 98/100", color = EmeraldTertiary, fontWeight = FontWeight.Bold)
                                    Text("♿ Access: 100/100", color = EmeraldTertiary, fontWeight = FontWeight.Bold)
                                    Text("🔍 SEO: 96/100", color = EmeraldTertiary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 3,
                            timeSavedMinutes = 75,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "AUDIT_TECH",
                                    "Audit Technique & Recommandations",
                                    "Scan des Core Web Vitals, temps de chargement des images et balises canoniques.",
                                    3,
                                    75,
                                    "Rapport d'audit technique avec 5 optimisations CSS/JS appliquées.",
                                    false,
                                    "Web"
                                )
                            }
                        )
                    }
                }
            }

            WebLaunchTab.CONNECTORS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Connexion Automatique d'Outils Tiers", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Intégrez Stripe Checkout, Google Analytics 4, Notion DB et Mailchimp en 1 clic.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 4,
                            timeSavedMinutes = 120,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "CONNECTORS_SETUP",
                                    "Intégration Stripe & Analytics",
                                    "Configuration des webhooks et tags de tracking e-commerce.",
                                    4,
                                    120,
                                    "Connecteurs configurés : Webhook Stripe fonctionnel, GA4 configuré, formulaires reliés à Airtable.",
                                    false,
                                    "Web"
                                )
                            }
                        )
                    }
                }
            }

            WebLaunchTab.CLOUD_DEPLOY -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Déploiement One-Click & Configuration DNS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 5,
                            timeSavedMinutes = 90,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "CLOUD_DEPLOY",
                                    "Déploiement Cloud & SSL Automatique",
                                    "Mise en ligne sur CDN global avec certificat HTTPS Let's Encrypt et DNS configurés.",
                                    5,
                                    90,
                                    "Site en ligne avec CDN Cloudflare actif et SSL A+ validé.",
                                    false,
                                    "Web"
                                )
                            }
                        )
                    }
                }
            }

            WebLaunchTab.MAINTENANCE -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Maintenance Prédictive & Scan de Vulnérabilités", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 3,
                            timeSavedMinutes = 60,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "MAINTENANCE_SCAN",
                                    "Scan Sécurité & Mises à Jour",
                                    "Vérification des failles CVE et optimisation des assets statiques.",
                                    3,
                                    60,
                                    "Scan complété : 0 vulnérabilité critique. Sauvegarde incrémentale journalière active.",
                                    false,
                                    "Web"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
