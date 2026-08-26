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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary

enum class FreelanceHubTab(val label: String) {
    PROPOSALS("1. Propositions IA"),
    TIME_BILLING("2. Facturation & Relance"),
    PORTFOLIO("3. Portefeuille"),
    SUBCONTRACTING("4. Matching Freelances"),
    ACTIVITY_REPORT("5. Rapports Fiscaux")
}

@Composable
fun FreelanceHubScreen(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecuteTask: (String, String, String, Int, Int, String, Boolean, String) -> Unit,
    onBookEscalation: (String, String, Double) -> Unit
) {
    var selectedTab by remember { mutableStateOf(FreelanceHubTab.PROPOSALS) }
    val emeraldColor = EmeraldTertiary

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
            FreelanceHubTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab) emeraldColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("tab_freelance_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            FreelanceHubTab.PROPOSALS -> {
                var clientName by remember { mutableStateOf("Banque Digitale SAS") }
                var projectScope by remember { mutableStateOf("Refonte UX/UI de l'espace onboarding client et intégration design system") }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Générateur de Propositions Commerciales Gagnantes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Créez une proposition personnalisée avec plan d'action, jalons, garanties et tarification à haute valeur.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        OutlinedTextField(
                            value = clientName,
                            onValueChange = { clientName = it },
                            label = { Text("Nom du prospect / Entreprise") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = projectScope,
                            onValueChange = { projectScope = it },
                            label = { Text("Besoin du client & Enjeux") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 3,
                            timeSavedMinutes = 90,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "PROPOSAL_AI",
                                    "Proposition Commerciale ($clientName)",
                                    "Proposition de 6 pages avec phasage méthodologique, roadmap et conditions de succès.",
                                    3,
                                    90,
                                    "Proposition commerciale générée au format PDF professionnel avec grille tarifaire packagée.",
                                    true,
                                    "Proposition"
                                )
                            }
                        )
                    }
                }
            }

            FreelanceHubTab.TIME_BILLING -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Suivi Temps, Facturation & Relances Clients", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Automatisation des séquences de relance amiable pour réduire les délais de paiement de 42 jours à 11 jours.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Factures en attente d'encaissement :", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• Facture #2026-08 (Acme Corp) : 3 500 € (Échue depuis 5j)", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                Text("• Facture #2026-09 (Startup X) : 1 800 € (Échéance dans 12j)", color = EmeraldTertiary, fontSize = 12.sp)
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
                                    "AUTO_CHASE_BILLING",
                                    "Séquence de Relance Automatique",
                                    "Génération d'un email de relance poli et structuré avec QR code de paiement immédiat.",
                                    2,
                                    45,
                                    "Séquence de 3 emails programmée pour envoi automatique à J+3, J+7 et J+15.",
                                    false,
                                    "Facturation"
                                )
                            }
                        )
                    }
                }
            }

            FreelanceHubTab.PORTFOLIO -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Portefeuille de Projets & Jalons", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    item {
                        TaskCostPaywallCard(
                            baseCreditCost = 2,
                            timeSavedMinutes = 40,
                            userCreditsBalance = wallet.creditsBalance,
                            subscriptionTier = wallet.subscriptionTier,
                            isProcessing = isProcessing,
                            onExecuteClick = {
                                onExecuteTask(
                                    "PORTFOLIO_SYNC",
                                    "Synchronisation Portefeuille Projets",
                                    "Calcul automatique du reste à facturer et des jalons livrables.",
                                    2,
                                    40,
                                    "Tableau de bord de rentabilité projets mis à jour (Taux horaire effectif : 85€/h).",
                                    false,
                                    "Gestion"
                                )
                            }
                        )
                    }
                }
            }

            FreelanceHubTab.SUBCONTRACTING -> {
                var showDialog by remember { mutableStateOf(false) }
                var selectedExpert by remember { mutableStateOf("Alexandre D. (Développeur Flutter / Kotlin)") }
                var price by remember { mutableStateOf(650.0) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Matching Sous-Traitance Freelance (Commission 20%)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Déléguez une partie de vos projets à des pairs vérifiés avec contrat de sous-traitance pré-rempli et séquestre.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    val freelancers = listOf(
                        Triple("Alexandre D.", "Développeur Mobile Android / Kotlin (Senior 7 ans)", 650.0),
                        Triple("Sarah M.", "Lead UI/UX Designer & Prototypage Figma", 450.0),
                        Triple("Thomas B.", "Expert DevOps & Architecture Cloud AWS/GCP", 800.0)
                    )

                    items(freelancers) { (name, skill, cost) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("freelance_${name.take(5).lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(CircleShape).background(EmeraldTertiary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Work, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Freelance Vérifié (Note 4.9/5)", color = EmeraldTertiary, fontSize = 11.sp)
                                        }
                                    }
                                    Text("$cost €/j", fontWeight = FontWeight.Black, fontSize = 15.sp, color = EmeraldTertiary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(skill, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        selectedExpert = name
                                        price = cost
                                        showDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldTertiary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Sous-traiter la mission ($cost €)")
                                }
                            }
                        }
                    }
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Séquestre Sous-Traitance") },
                        text = {
                            val commission = price * 0.20
                            Text(
                                "Vous sous-traitez une mission à $selectedExpert pour $price €.\n\n" +
                                "• Rémunération plateforme (20%) : ${String.format("%.2f", commission)} €\n" +
                                "• Versement au freelance sous-traitant (80%) : ${String.format("%.2f", price - commission)} €\n\n" +
                                "Le contrat de sous-traitance et l'engagement de confidentialité sont signés automatiquement."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    onBookEscalation(selectedExpert, "Sous-traitant Freelance", price)
                                    showDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldTertiary)
                            ) {
                                Text("Confirmer le matching")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Annuler")
                            }
                        }
                    )
                }
            }

            FreelanceHubTab.ACTIVITY_REPORT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Génération Rapports d'Activité & Déclarations Fiscales", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
                                    "ACTIVITY_REPORT",
                                    "Rapport d'Activité Trimestriel",
                                    "Synthèse CA, charges déductibles, TVA collectée et rentabilité par client.",
                                    3,
                                    60,
                                    "Rapport PDF complet prêt pour l'expert-comptable ou l'administration fiscale.",
                                    true,
                                    "Rapport"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
