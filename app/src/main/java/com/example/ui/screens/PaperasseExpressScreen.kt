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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DigitalDocument
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserWallet
import com.example.ui.components.TaskCostPaywallCard
import com.example.ui.components.WizardStepBar
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseAccent

enum class PaperasseWorkflow(val label: String) {
    DOC_GENERATOR("1. Documents"),
    ADMIN_FORMS("2. Formulaires URSSAF"),
    LEGAL_AUDIT("3. Audit Juridique"),
    DIGITAL_SAFE("4. Coffre & Alertes"),
    LAWYER_MATCH("5. Expert Avocat")
}

@Composable
fun PaperasseExpressScreen(
    wallet: UserWallet,
    documents: List<DigitalDocument>,
    isProcessing: Boolean,
    onExecuteTask: (String, String, String, Int, Int, String, Boolean, String) -> Unit,
    onBookEscalation: (String, String, Double) -> Unit,
    onDeleteDoc: (Long) -> Unit
) {
    var selectedWorkflow by remember { mutableStateOf(PaperasseWorkflow.DOC_GENERATOR) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Workflow Subtabs
        ScrollableTabRow(
            selectedTabIndex = selectedWorkflow.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp
        ) {
            PaperasseWorkflow.values().forEach { wf ->
                Tab(
                    selected = selectedWorkflow == wf,
                    onClick = { selectedWorkflow = wf },
                    text = {
                        Text(
                            text = wf.label,
                            fontWeight = if (selectedWorkflow == wf) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedWorkflow == wf) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("tab_paperasse_${wf.name.lowercase()}")
                )
            }
        }

        // Active Workflow Content
        when (selectedWorkflow) {
            PaperasseWorkflow.DOC_GENERATOR -> DocumentGeneratorWizard(
                wallet = wallet,
                isProcessing = isProcessing,
                onExecute = onExecuteTask
            )
            PaperasseWorkflow.ADMIN_FORMS -> AdminFormFillerWizard(
                wallet = wallet,
                isProcessing = isProcessing,
                onExecute = onExecuteTask
            )
            PaperasseWorkflow.LEGAL_AUDIT -> LegalDocAuditor(
                wallet = wallet,
                isProcessing = isProcessing,
                onExecute = onExecuteTask
            )
            PaperasseWorkflow.DIGITAL_SAFE -> DigitalSafeView(
                documents = documents,
                onDelete = onDeleteDoc
            )
            PaperasseWorkflow.LAWYER_MATCH -> LawyerEscalationView(
                onBook = onBookEscalation
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WORKFLOW 1: DOCUMENT GENERATOR (WIZARD PAS-À-PAS)
// ─────────────────────────────────────────────────────────────
@Composable
private fun DocumentGeneratorWizard(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecute: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var docType by remember { mutableStateOf("Contrat de Prestation B2B") }
    var providerName by remember { mutableStateOf("Mon Entreprise / Freelance") }
    var providerSiret by remember { mutableStateOf("849 123 456 00012") }
    var clientName by remember { mutableStateOf("Acme Global SAS") }
    var serviceDescription by remember { mutableStateOf("Développement d'une application mobile & intégration API") }
    var priceAmount by remember { mutableStateOf("4500") }
    var deadlineDays by remember { mutableStateOf("30") }
    var hasNDA by remember { mutableStateOf(true) }
    var generatedDocPreview by remember { mutableStateOf<String?>(null) }

    val stepTitles = listOf("Type de Document", "Parties & Mandat", "Conditions & Tarifs", "Génération & Séquestre")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WizardStepBar(
                currentStep = step,
                totalSteps = stepTitles.size,
                stepTitles = stepTitles,
                activeColor = IndigoPrimary
            )
        }

        when (step) {
            0 -> {
                item {
                    Text(
                        text = "Sélectionnez le type d'acte à générer :",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val types = listOf(
                    "Contrat de Prestation B2B" to "Prestation de services, propriété intellectuelle, clause de non-concurrence",
                    "Devis Professionnel Conforme" to "Mention TVA légale, délais de rétractation, acompte 30%",
                    "Facture Conforme (Chorus Pro)" to "Facturation électronique standardisé avec pénalités de retard L441-10",
                    "Mise en Demeure de Payer" to "Courrier juridique avec injonction de payer sous 8 jours et intérêts légaux",
                    "Lettre de Résiliation Contrat" to "Notification avec respect du préavis contractuel et solde de tout compte"
                )

                items(types) { (type, desc) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { docType = type }
                            .testTag("doc_type_${type.take(10).replace(" ", "_")}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (docType == type) IndigoPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (docType == type) androidx.compose.foundation.BorderStroke(1.5.dp, IndigoPrimary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (docType == type) Icons.Default.CheckCircle else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (docType == type) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = type, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Suivant : Renseigner les Parties")
                    }
                }
            }

            1 -> {
                item {
                    Text(text = "Informations des cocontractants", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = providerName,
                        onValueChange = { providerName = it },
                        label = { Text("Prestataire / Votre Société") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = providerSiret,
                        onValueChange = { providerSiret = it },
                        label = { Text("Numéro SIRET (14 chiffres)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client / Donneur d'ordre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = serviceDescription,
                        onValueChange = { serviceDescription = it },
                        label = { Text("Objet de la mission") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { step = 0 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Retour", color = MaterialTheme.colorScheme.onSurface)
                        }
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Suivant : Tarifs")
                        }
                    }
                }
            }

            2 -> {
                item {
                    Text(text = "Conditions financières & clauses", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    OutlinedTextField(
                        value = priceAmount,
                        onValueChange = { priceAmount = it },
                        label = { Text("Montant Total (€ HT)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = deadlineDays,
                        onValueChange = { deadlineDays = it },
                        label = { Text("Délai de réalisation (en jours)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hasNDA = !hasNDA },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasNDA) IndigoPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasNDA) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (hasNDA) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Inclure Clause de Confidentialité (NDA) & Cession PI", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Protège vos droits d'auteur et secrets de fabrication", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Retour", color = MaterialTheme.colorScheme.onSurface)
                        }
                        Button(
                            onClick = {
                                generatedDocPreview = """
                                    ══════════════════════════════════════════════════
                                    DOCUMENT LÉGAL : ${docType.uppercase()}
                                    ══════════════════════════════════════════════════
                                    ENTRE LES SOUSSIGNÉS :
                                    1. LE PRESTATAIRE : $providerName (SIRET: $providerSiret)
                                    2. LE CLIENT : $clientName
                                    
                                    ARTICLE 1 — OBJET DE LA PRESTATION
                                    Le Client confie au Prestataire la réalisation de :
                                    "$serviceDescription"
                                    
                                    ARTICLE 2 — PRIX ET CONDITIONS DE RÈGLEMENT
                                    Le montant total forfaitaire est fixé à $priceAmount € HT.
                                    Acompte de 30% à la commande, solde à livraison ($deadlineDays jours).
                                    Pénalités de retard : 3x le taux légal + indemnité forfaitaire 40€.
                                    
                                    ARTICLE 3 — PROPRIÉTÉ INTELLECTUELLE & NDA
                                    ${if (hasNDA) "Transfert exclusif de propriété à complet paiement. Confidentialité stricte 3 ans." else "Droit d'usage non exclusif."}
                                    
                                    Fait en 2 exemplaires certifiés conformes.
                                """.trimIndent()
                                step = 3
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Prévisualiser")
                        }
                    }
                }
            }

            3 -> {
                item {
                    Text(text = "Prévisualisation du Document Juridique", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldTertiary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Conformité juridique vérifiée (Droit Français)", color = EmeraldTertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = generatedDocPreview ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }

                item {
                    TaskCostPaywallCard(
                        baseCreditCost = 2,
                        timeSavedMinutes = 90,
                        userCreditsBalance = wallet.creditsBalance,
                        subscriptionTier = wallet.subscriptionTier,
                        isProcessing = isProcessing,
                        onExecuteClick = {
                            onExecute(
                                "GEN_DOC",
                                docType,
                                "Génération automatique d'un acte certifié ($docType) pour $clientName",
                                2,
                                90,
                                generatedDocPreview ?: "Document généré",
                                true,
                                "Contrat"
                            )
                        }
                    )
                }

                item {
                    TextButton(
                        onClick = { step = 0 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Modifier les informations du document")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WORKFLOW 2: REMPLISSAGE FORMULAIRES URSSAF / TVA
// ─────────────────────────────────────────────────────────────
@Composable
private fun AdminFormFillerWizard(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecute: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    var caAmount by remember { mutableStateOf("12500") }
    var activityType by remember { mutableStateOf("Prestations de Services BNC (22%)") }
    var quarterPeriod by remember { mutableStateOf("Trimestre 3 (Juillet - Septembre)") }

    val cotisationsRate = if (activityType.contains("BNC")) 0.22 else 0.123
    val totalCotisations = (caAmount.toDoubleOrNull() ?: 0.0) * cotisationsRate
    val totalTva = (caAmount.toDoubleOrNull() ?: 0.0) * 0.20

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Remplissage Intelligent de Déclarations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Calcul automatique des cotisations sociales URSSAF et déclarations TVA sans erreur",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = caAmount,
                onValueChange = { caAmount = it },
                label = { Text("Chiffre d'Affaires Encaissé (€ HT)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Type d'activité déclarée :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activityType.contains("BNC"),
                    onClick = { activityType = "Prestations de Services BNC (22%)" },
                    label = { Text("Services BNC (22%)") }
                )
                FilterChip(
                    selected = activityType.contains("BIC"),
                    onClick = { activityType = "Vente de Marchandises BIC (12.3%)" },
                    label = { Text("Vente BIC (12.3%)") }
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Calcul Automatisé du Formulaire URSSAF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base imposable :", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$caAmount €", fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Taux de cotisations :", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(cotisationsRate * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Montant net à payer URSSAF :", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.2f", totalCotisations)} €", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            TaskCostPaywallCard(
                baseCreditCost = 3,
                timeSavedMinutes = 60,
                userCreditsBalance = wallet.creditsBalance,
                subscriptionTier = wallet.subscriptionTier,
                isProcessing = isProcessing,
                onExecuteClick = {
                    onExecute(
                        "URSSAF_FILL",
                        "Déclaration URSSAF Auto-remplie",
                        "Calcul et remplissage automatique de la déclaration URSSAF ($quarterPeriod). Net à payer : ${String.format("%.2f", totalCotisations)} €.",
                        3,
                        60,
                        "Déclaration pré-remplie prête pour transmission télépaiement URSSAF. Total: ${String.format("%.2f", totalCotisations)}€",
                        true,
                        "URSSAF"
                    )
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WORKFLOW 3: AUDIT JURIDIQUE & VÉRIFICATION DES CLAUSES
// ─────────────────────────────────────────────────────────────
@Composable
private fun LegalDocAuditor(
    wallet: UserWallet,
    isProcessing: Boolean,
    onExecute: (String, String, String, Int, Int, String, Boolean, String) -> Unit
) {
    var rawContractText by remember {
        mutableStateOf(
            "Le prestataire s'engage à livrer le code sous 15 jours. Le client peut résilier à tout moment sans indemnité. Toute la propriété intellectuelle appartient immédiatement au client."
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Audit & Score de Risque Juridique", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Collez un extrait ou contrat pour détecter les clauses léonines, risques RGPD ou failles de facturation.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            OutlinedTextField(
                value = rawContractText,
                onValueChange = { rawContractText = it },
                label = { Text("Texte de la clause ou du contrat à auditer") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Score de Risque Détecté", fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Risque Modéré (68/100)", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("⚠️ Clause de résiliation unilatérale sans préavis défavorable au prestataire.", color = AmberAccent, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("⚠️ Cession de PI sans condition de complet paiement du prix.", color = RoseAccent, fontSize = 12.sp)
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
                    onExecute(
                        "LEGAL_AUDIT",
                        "Rapport d'Audit Juridique",
                        "Audit complet des clauses contractuelles et proposition d'avenants protecteurs.",
                        3,
                        75,
                        "Audit validé : 2 clauses corrigées avec formulation conforme code civil art. 1104.",
                        true,
                        "Audit"
                    )
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WORKFLOW 4: COFFRE-FORT NUMÉRIQUE & RAPPELS D'ÉCHÉANCES
// ─────────────────────────────────────────────────────────────
@Composable
private fun DigitalSafeView(
    documents: List<DigitalDocument>,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Coffre-Fort Numérique & Échéances", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("${documents.size} documents certifiés conservés", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = IndigoPrimary)
            }
        }

        if (documents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Aucun document archivé pour le moment.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(documents) { doc ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("doc_item_${doc.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IndigoPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = IndigoPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(doc.contentSummary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            if (doc.expirationDateMillis != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("⏳ Échéance de renouvellement dans 45 jours", fontSize = 10.sp, color = AmberAccent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        IconButton(onClick = { onDelete(doc.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = RoseAccent.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WORKFLOW 5: ESCALADE AVOCAT / JURISTE HUMAIN (COMMISSION 20%)
// ─────────────────────────────────────────────────────────────
@Composable
private fun LawyerEscalationView(
    onBook: (String, String, Double) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedLawyer by remember { mutableStateOf("Maître Laurent (Droit des Affaires & SaaS)") }
    var lawyerPrice by remember { mutableStateOf(250.0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Mise en Relation Avocat & Juriste Agréé", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("Pour les contentieux lourds, levées de fonds ou pactes d'actionnaires. Séquestre sécurisé + Rémunération plateforme 20%.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val lawyers = listOf(
            Triple("Maître Laurent", "Droit des Affaires, SaaS & Propriété Intellectuelle (Barreau de Paris)", 250.0),
            Triple("Cabinet Juridix", "Contentieux Impayés, Recouvrement & Mise en demeure judiciaire", 180.0),
            Triple("Me Sophie Bernard", "Contrats Internationaux & Conformité RGPD / DORA", 320.0)
        )

        items(lawyers) { (name, specialty, price) ->
            Card(
                modifier = Modifier.fillMaxWidth().testTag("lawyer_${name.replace(" ", "_").lowercase()}"),
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
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(IndigoPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Avocat Partenaire Vérifié", color = EmeraldTertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Text("$price €", fontWeight = FontWeight.Black, fontSize = 16.sp, color = IndigoPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(specialty, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            selectedLawyer = name
                            lawyerPrice = price
                            showDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confier le dossier ($price € TTC)")
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Séquestre & Mise en Relation") },
            text = {
                val commission = lawyerPrice * 0.20
                Text(
                    "Vous allez transmettre votre dossier à $selectedLawyer pour un montant de $lawyerPrice €.\n\n" +
                    "• Rémunération plateforme TaskFlow (20%) : ${String.format("%.2f", commission)} €\n" +
                    "• Rémunération de l'expert avocat (80%) : ${String.format("%.2f", lawyerPrice - commission)} €\n\n" +
                    "Les fonds sont placés sous séquestre jusqu'à validation de la prestation."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBook(selectedLawyer, "Avocat / Juriste Agréé", lawyerPrice)
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Valider la commande")
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
