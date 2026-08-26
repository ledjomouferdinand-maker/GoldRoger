package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserWallet
import com.example.ui.components.TaskCostPaywallCard
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary

enum class ContentStudioTab(val label: String) {
    REPURPOSING("1. Repurposing 10x"),
    TEXT_SEO("2. Textes & SEO"),
    CALENDAR("3. Calendrier"),
    MOCKUPS("4. Visuels"),
    CREATOR_BRIEF("5. Brief Créatif")
}

@Composable
fun ContentStudioScreen(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecuteTask: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(ContentStudioTab.REPURPOSING) }
    val pinkColor = Color(0xFFEC4899)

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
            ContentStudioTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab) pinkColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("tab_content_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            ContentStudioTab.REPURPOSING -> {
                RepurposingView(
                    wallet = wallet,
                    isProcessing = isProcessing,
                    onExecute = onExecuteTask
                )
            }
            ContentStudioTab.TEXT_SEO -> {
                TextSeoView(
                    wallet = wallet,
                    isProcessing = isProcessing,
                    onExecute = onExecuteTask
                )
            }
            ContentStudioTab.CALENDAR -> {
                EditorialCalendarView(
                    wallet = wallet,
                    isProcessing = isProcessing,
                    onExecute = onExecuteTask
                )
            }
            ContentStudioTab.MOCKUPS -> {
                VisualMockupView(
                    wallet = wallet,
                    isProcessing = isProcessing,
                    onExecute = onExecuteTask
                )
            }
            ContentStudioTab.CREATOR_BRIEF -> {
                CreatorBriefView(
                    wallet = wallet,
                    isProcessing = isProcessing,
                    onExecute = onExecuteTask
                )
            }
        }
    }
}

@Composable
private fun RepurposingView(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecute: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    var sourceContent by remember {
        mutableStateOf("Comment nous avons automatisé 80% des tâches juridiques et administratives pour économiser 20h par semaine grâce à TaskFlow Pro.")
    }

    val pinkColor = Color(0xFFEC4899)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Repurposing Multi-Format (1 Contenu → 10 Formats)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Transformez un simple texte en Thread X, Carousel LinkedIn, Script Shorts, Newsletter et Email de vente instantanément.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            OutlinedTextField(
                value = sourceContent,
                onValueChange = { sourceContent = it },
                label = { Text("Contenu source (Article, Vidéo ou Idée brute)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("10 Formats Dérivés Générés Automatiquement :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "1. Thread X / Twitter (7 tweets avec hooks & CTA)",
                        "2. Carousel LinkedIn (5 slides synthétiques)",
                        "3. Script Reel / TikTok (Format 60s avec indications visuelles)",
                        "4. Newsletter Dédiée (Format personnel & storytelling)",
                        "5. Email de prospection B2B orienté conversion",
                        "6. 3 Citations visuelles percutantes",
                        "7. Post Facebook communautaire",
                        "8. Fiche Mémo téléchargeable"
                    ).forEach { fmt ->
                        Text("• $fmt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            TaskCostPaywallCard(
                baseCreditCost = 4,
                timeSavedMinutes = 120,
                userCreditsBalance = wallet.creditsBalance,
                subscriptionTier = wallet.subscriptionTier,
                isProcessing = isProcessing,
                onExecuteClick = {
                    onExecute(
                        "REPURPOSE_10X",
                        "Repurposing 10x Formats Contenu",
                        "Transformation d'une source en 10 déclinaisons multi-réseaux (LinkedIn, X, TikTok, Newsletter).",
                        4,
                        120,
                        "10 formats générés : Thread X 7 posts, Carousel 5 slides, Script Reel 60s, Newsletter 450 mots.",
                        false,
                        "Contenu"
                    )
                }
            )
        }
    }
}

@Composable
private fun TextSeoView(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecute: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    var keyword by remember { mutableStateOf("Automatisation SaaS TPE PME") }
    var tone by remember { mutableStateOf("Expert & Inspirant") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Générateur & Optimiseur SEO / Réseaux", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("Mots-clés / Sujet ciblé") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            TaskCostPaywallCard(
                baseCreditCost = 2,
                timeSavedMinutes = 45,
                userCreditsBalance = wallet.creditsBalance,
                subscriptionTier = wallet.subscriptionTier,
                isProcessing = isProcessing,
                onExecuteClick = {
                    onExecute(
                        "SEO_TEXT_GEN",
                        "Article SEO Optimisé ($keyword)",
                        "Rédaction d'un article structuré H1/H2 avec méta-description et balises OpenGraph.",
                        2,
                        45,
                        "Article de 1200 mots optimisé avec score de lisibilité 92/100 et densité sémantique idéale.",
                        false,
                        "Contenu"
                    )
                }
            )
        }
    }
}

@Composable
private fun EditorialCalendarView(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecute: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Planificateur Éditorial Automatisé (30 Jours)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Création d'un calendrier éditorial complet avec thématiques par semaine, angles d'attaque et horaires optimaux.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            TaskCostPaywallCard(
                baseCreditCost = 3,
                timeSavedMinutes = 90,
                userCreditsBalance = wallet.creditsBalance,
                subscriptionTier = wallet.subscriptionTier,
                isProcessing = isProcessing,
                onExecuteClick = {
                    onExecute(
                        "EDITORIAL_CALENDAR",
                        "Calendrier Éditorial 30 Jours",
                        "Planning de 20 publications sur 4 semaines avec hooks, hashtags et visuels recommandés.",
                        3,
                        90,
                        "Calendrier mensuel validé et synchronisable Google Calendar / Notion.",
                        false,
                        "Contenu"
                    )
                }
            )
        }
    }
}

@Composable
private fun VisualMockupView(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecute: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Création de Visuels & Mockups Intelligents", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            TaskCostPaywallCard(
                baseCreditCost = 2,
                timeSavedMinutes = 40,
                userCreditsBalance = wallet.creditsBalance,
                subscriptionTier = wallet.subscriptionTier,
                isProcessing = isProcessing,
                onExecuteClick = {
                    onExecute(
                        "MOCKUP_GEN",
                        "Mockup Produit SaaS 3D",
                        "Génération de 3 déclinaisons visuelles HD pour landing page et réseaux sociaux.",
                        2,
                        40,
                        "Templates PNG haute résolution exportés avec charte graphique harmonisée.",
                        false,
                        "Contenu"
                    )
                }
            )
        }
    }
}

@Composable
private fun CreatorBriefView(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecute: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Brief Créatif Automatisé pour Externalisation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Générez un cahier des charges ultra-précis prêt à être envoyé à des freelances vidéo, graphistes ou copywriters.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            TaskCostPaywallCard(
                baseCreditCost = 2,
                timeSavedMinutes = 50,
                userCreditsBalance = wallet.creditsBalance,
                subscriptionTier = wallet.subscriptionTier,
                isProcessing = isProcessing,
                onExecuteClick = {
                    onExecute(
                        "CREATOR_BRIEF",
                        "Brief Graphiste & Copywriter",
                        "Cahier des charges complet : ton de marque, personas, formats attendus et délais.",
                        2,
                        50,
                        "Brief PDF exporté prêt pour délégation sur plateformes freelances.",
                        false,
                        "Contenu"
                    )
                }
            )
        }
    }
}
