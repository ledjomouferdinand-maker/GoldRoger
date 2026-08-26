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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary

enum class BlueprintSection(val label: String) {
    GLOBAL_STACK("1. Stack & Infra"),
    DATABASE_SCHEMA("2. Schéma DB"),
    API_ENDPOINTS("3. Spécifications API"),
    MONETIZATION_ENGINE("4. Moteur Rémunération"),
    DEVELOPMENT_PLAN("5. Roadmap MVP→Scale"),
    INFRA_COSTS("6. Coûts & Déploiement"),
    FINANCIAL_PROJECTIONS("7. Projections CA")
}

@Composable
fun ArchitectureBlueprintScreen() {
    var selectedSection by remember { mutableStateOf(BlueprintSection.GLOBAL_STACK) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedSection.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp
        ) {
            BlueprintSection.values().forEach { sec ->
                Tab(
                    selected = selectedSection == sec,
                    onClick = { selectedSection = sec },
                    text = {
                        Text(
                            text = sec.label,
                            fontWeight = if (selectedSection == sec) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedSection == sec) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("tab_blueprint_${sec.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedSection) {
                BlueprintSection.GLOBAL_STACK -> {
                    item {
                        BlueprintCard(
                            title = "Stack Technologique Optimale Justifiée",
                            icon = Icons.Default.Layers,
                            content = """
                                ══════════════════════════════════════════════════════
                                1. FRONTEND / CLIENT APPLICATIONS
                                • Web & Admin : Next.js 14 (App Router) + Tailwind CSS + Shadcn UI
                                • Mobile (iOS/Android) : Kotlin Multiplatform / Jetpack Compose & SwiftUI
                                • State Management : TanStack Query (React Query) + Zustand
                                
                                2. BACKEND & API (MONOLITHE MODULAIRE)
                                • Runtime : Node.js / TypeScript avec NestJS ou Go (Golang)
                                • Architecture : Modular Monolith (domaines isolés par module)
                                • Communication : REST API (OpenAPI 3.1) + GraphQL pour dashboards complexes
                                
                                3. FILE DE TRAITEMENT ASYNCHRONE & LLM ORCHESTRATION
                                • Message Broker : Redis + BullMQ pour les tâches lourdes
                                • Orchestration IA : LangChain / LlamaIndex + Gemini 1.5 Pro / Claude 3.5
                                • Workers : Node.js Worker Threads avec timeout et retry exponentiel
                                
                                4. SÉCURITÉ & AUTHENTIFICATION
                                • Auth : Supabase Auth / Clerk (OAuth2, Passkeys, SSO Google/GitHub)
                                • Conformité : Chiffrement AES-256 au repos, TLS 1.3 en transit, RGPD compliant
                            """.trimIndent()
                        )
                    }
                }

                BlueprintSection.DATABASE_SCHEMA -> {
                    item {
                        BlueprintCard(
                            title = "Schéma Multi-Tenant : Room DB (Local) & Firestore (Cloud)",
                            icon = Icons.Default.DataArray,
                            content = """
                                ══════════════════════════════════════════════════════
                                1. HIÉRARCHIE NO-SQL FIRESTORE (MULTI-TENANT ISOLATION)
                                
                                📁 /tenants/{tenantId}
                                   ├── name: "Agence Alpha"
                                   ├── slug: "agence-alpha"
                                   ├── plan: "PRO" | "FREE" | "ENTERPRISE"
                                   ├── creditsBalance: 58
                                   ├── maxUsers: 5
                                   │
                                   ├── 📂 /users/{userId}
                                   │     ├── email: "alexandre@agencealpha.fr"
                                   │     ├── role: "OWNER" | "ADMIN" | "MEMBER" | "AUDITOR"
                                   │     └── totalTimeSavedMinutes: 720
                                   │
                                   ├── 📂 /modules/{moduleId}
                                   │     ├── isEnabled: true
                                   │     ├── customCreditCost: 2
                                   │     └── overridesJson: "{}"
                                   │
                                   ├── 📂 /tasks/{taskId}
                                   │     ├── moduleId: "paperasse" | "content" | "weblaunch"
                                   │     ├── taskType: "GEN_DOC" | "REPURPOSE_10X" | "SEO_AUDIT"
                                   │     ├── status: "COMPLETED" | "PROCESSING" | "FAILED"
                                   │     ├── creditsCost: 2
                                   │     ├── timeSavedMinutes: 90
                                   │     ├── inputPayload: { ... }
                                   │     └── generatedResult: "Contrat généré..."
                                   │
                                   ├── 📂 /transactions/{transactionId}
                                   │     ├── type: "TOPUP" | "SPEND" | "SUBSCRIPTION" | "HUMAN_COMMISSION"
                                   │     ├── amountCredits: -2 (or +50)
                                   │     ├── amountEuros: 29.00
                                   │     ├── platformCommissionEuros: 9.60 (20%)
                                   │     └── status: "SUCCEEDED"
                                   │
                                   └── 📂 /documents/{docId}
                                         ├── title: "Contrat Freelance SaaS.pdf"
                                         ├── category: "Contrat" | "URSSAF" | "Facture"
                                         └── isVerifiedLegal: true
                                
                                📁 /system_catalog/modules/{moduleId} (Catalogue global)
                                
                                ══════════════════════════════════════════════════════
                                2. ENTITÉS RELATIIONNELLES ROOM (OFFLINE-FIRST SQLITE)
                                
                                • TABLE tenants (tenantId PK, name, slug UNIQUE, plan, creditsBalance, stripeCustomerId)
                                • TABLE users (userId PK, tenantId FK/INDEX, email UNIQUE, role, creditsBalance)
                                • TABLE modules (moduleId PK, tenantId, title, baseCreditCost, estimatedTimeSavedMinutes)
                                • TABLE tasks (id PK AUTO, tenantId INDEX, userId INDEX, moduleId INDEX, creditsCost, status, generatedResult)
                                • TABLE transactions (id PK AUTO, tenantId INDEX, userId INDEX, amountCredits, amountEuros, type, status)
                                • TABLE digital_documents (id PK AUTO, tenantId INDEX, title, category, isVerifiedLegal)
                                
                                ══════════════════════════════════════════════════════
                                3. SÉCURITÉ FIRESTORE RLS & RÈGLES DE PARTITIONNEMENT
                                
                                match /tenants/{tenantId} {
                                  allow read: if request.auth.token.tenantId == tenantId;
                                  allow write: if request.auth.token.role == 'OWNER';
                                  
                                  match /tasks/{taskId} {
                                    allow read, write: if request.auth.token.tenantId == tenantId;
                                  }
                                  
                                  match /transactions/{txId} {
                                    allow read: if request.auth.token.tenantId == tenantId;
                                    allow write: if false; // Append-only via backend Cloud Functions
                                  }
                                }
                            """.trimIndent()
                        )
                    }
                }

                BlueprintSection.API_ENDPOINTS -> {
                    item {
                        BlueprintCard(
                            title = "Spécifications de l'API REST",
                            icon = Icons.Default.Code,
                            content = """
                                ══════════════════════════════════════════════════════
                                MODULE 1 — LEGAL & ADMIN
                                • POST /api/v1/modules/paperasse/generate-doc
                                  Req: { docType, parties, clauses, nda } -> 200: { docUrl, taskCost: 2 }
                                • POST /api/v1/modules/paperasse/urssaf-fill
                                  Req: { caHt, activityType, quarter } -> 200: { cotisationsAmount, formPdf }
                                • POST /api/v1/modules/paperasse/audit-contract
                                  Req: { text } -> 200: { riskScore, warnings, suggestedClauses }
                                
                                MODULE 2 — CONTENT STUDIO
                                • POST /api/v1/modules/content/repurpose-10x
                                  Req: { sourceText } -> 200: { threadX, linkedinCarousel, reelScript, newsletter }
                                
                                MODULE 3 — WEBLAUNCH
                                • POST /api/v1/modules/weblaunch/build-landing
                                  Req: { name, valueProp, theme } -> 200: { previewHtml, deployUrl }
                                
                                WALLET & MONETIZATION
                                • GET  /api/v1/wallet/balance -> { credits: 42, plan: 'PRO' }
                                • POST /api/v1/wallet/purchase-pack -> { packId: 'business_100' }
                                • POST /api/v1/escrow/create-human-order -> { expertId, quoteEuros, commission: 0.20 }
                            """.trimIndent()
                        )
                    }
                }

                BlueprintSection.MONETIZATION_ENGINE -> {
                    item {
                        BlueprintCard(
                            title = "Modèle Économique & Moteur de Rémunération",
                            icon = Icons.Default.Euro,
                            content = """
                                ══════════════════════════════════════════════════════
                                4 LEVIERS DE RENTABILITÉ POUR LE PROPRIÉTAIRE :
                                
                                1. PRIX DU CRÉDIT UNITAIRE (Marges brutes > 85%)
                                • Coût API LLM par tâche : ~0.02€ à 0.08€
                                • Prix facturé à l'utilisateur : 0.50€ / crédit
                                • Marge unitaire nette : 84% à 96%
                                
                                2. ABONNEMENT TASKFLOW PRO (49€ / mois)
                                • Revenu Récurrent Prévisible (MRR)
                                • Offre 50 crédits + 30% de réduction sur les tâches premium
                                • LTV utilisateur Pro estimée à 588€ / an
                                
                                3. COMMISSION SUR SERVICES HUMAINS (15% à 25%)
                                • Pour chaque mise en relation avec un avocat agréé ou freelance expert
                                • Séquestre automatisé via Stripe Connect Custom Accounts
                                • Sur une mission à 500€, la plateforme encaisse 100€ (20%) sans coût opérationnel
                                
                                4. UPSELL ENTERPRISE & SUR-MESURE
                                • Multi-comptes, connecteurs CRM dédiés, SLA 99.9% (199€ à 499€ / mois)
                            """.trimIndent()
                        )
                    }
                }

                BlueprintSection.DEVELOPMENT_PLAN -> {
                    item {
                        BlueprintCard(
                            title = "Plan de Développement Phased (MVP → V1 → Scale)",
                            icon = Icons.Default.Timeline,
                            content = """
                                ══════════════════════════════════════════════════════
                                PHASE 1 — MVP CORE & PAPERASSE EXPRESS (Semaines 1-4)
                                • Auth & Multi-tenant RLS
                                • Module 1 : Générateur de documents & URSSAF filler
                                • Système de wallet basique & Stripe Checkout
                                • Objectif : Valider la willingness-to-pay sur les 50 premiers testeurs
                                
                                PHASE 2 — V1 EXPANSION DES 5 MODULES (Semaines 5-8)
                                • Déploiement Content Studio (Repurposing 10x)
                                • WebLaunch (Landing builder)
                                • Freelance Hub & Growth Engine
                                • Lancement formule Pro à 49€/mois
                                
                                PHASE 3 — INTELLIGENCE & ESCALADE HUMAINE (Semaines 9-10)
                                • Moteur de recommandation inter-modules
                                • Place de marché des experts (avocats & pairs freelances)
                                • Système de séquestre avec commission 20%
                                
                                PHASE 4 — SCALE & AUTOMATISATION (Semaines 11-14+)
                                • Kubernetes autoscaling & Redis cluster
                                • API Publique & webhooks pour intégrations Zapier/Make
                                • Dashboard analytics propriétaire avancé
                            """.trimIndent()
                        )
                    }
                }

                BlueprintSection.INFRA_COSTS -> {
                    item {
                        BlueprintCard(
                            title = "Estimation des Coûts d'Infrastructure Cloud",
                            icon = Icons.Default.CloudDone,
                            content = """
                                ══════════════════════════════════════════════════════
                                COÛTS MENSUELS AU LANCEMENT (0 à 1 000 Utilisateurs) :
                                • Vercel / Cloud Run (Frontend & API) : 40 € / mois
                                • Supabase / PostgreSQL Pro : 25 € / mois
                                • Upstash Redis (Queue BullMQ) : 10 € / mois
                                • LLM API (Gemini / Claude / OpenAI) : 120 € / mois
                                • Emailing transactionnel (Resend) : 20 € / mois
                                • TOTAL INFRA INITIAL : ~215 € / mois
                                
                                À 5 000 UTILISATEURS ACTIFS (MRR ~25 000 €) :
                                • Kubernetes / GCP Cloud Run autoscalé : 280 € / mois
                                • PostgreSQL géré (AWS RDS Multi-AZ) : 180 € / mois
                                • Cache Redis haute disponibilité : 60 € / mois
                                • API LLMs (Tier volume) : 950 € / mois
                                • TOTAL INFRA SCALE : ~1 470 € / mois (soit <6% du CA brut)
                            """.trimIndent()
                        )
                    }
                }

                BlueprintSection.FINANCIAL_PROJECTIONS -> {
                    item {
                        BlueprintCard(
                            title = "Projections Financières sur 24 Mois",
                            icon = Icons.Default.Timeline,
                            content = """
                                ══════════════════════════════════════════════════════
                                HYPOTHÈSES DE MODÉLISATION :
                                • Acquisition : 250 nouveaux utilisateurs freemium / mois
                                • Conversion Pro (49€/m) : 15%
                                • Achat moyen de crédits / freemium : 1.2 pack Starter (35€ / mois)
                                • Missions humaines externalisées : 40 missions / mois (panier moyen 350€, commission 20%)
                                
                                PROJECTIONS :
                                • Mois 6 : MRR 18 500 € | Commissions 2 800 € | Bénéfice net : 14 200 € / mois
                                • Mois 12 : MRR 58 000 € | Commissions 8 500 € | Bénéfice net : 51 000 € / mois
                                • Mois 24 : MRR 165 000 € | Commissions 24 000 € | Bénéfice net : 145 000 € / mois
                                
                                RÉSULTAT NET ANNUEL STABILISÉ (AN 2) : ~1.6M €
                            """.trimIndent()
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BlueprintCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("blueprint_card_${title.take(8).replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
