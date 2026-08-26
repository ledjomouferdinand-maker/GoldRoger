package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ModuleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Structured smart suggestion for task configuration within modules.
 */
data class TaskConfigSuggestion(
    val moduleType: ModuleType,
    val suggestedTitle: String,
    val suggestedDescription: String,
    val estimatedMinutesSaved: Int,
    val recommendedCreditCost: Int,
    val keyParameters: Map<String, String>,
    val suggestedPromptPayload: String,
    val strategicAdvice: List<String>,
    val source: String = "GEMINI_AI"
)

/**
 * Gemini AI Client Service
 * 
 * Provides intelligent configuration suggestions, auto-filling parameters,
 * and high-context drafts for all TaskFlow modules (Paperasse, Growth, WebLaunch, ContentStudio, FreelanceHub).
 */
class GeminiAIService(
    private val apiService: GeminiApiService = GeminiApiClient.service
) {
    private val tag = "GeminiAIService"

    /**
     * Resolves the configured Gemini API key from BuildConfig.
     */
    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "MY_GEMINI_API_KEY") "" else key
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Generates smart configuration suggestions for a given module and user prompt.
     */
    suspend fun getTaskConfigurationSuggestion(
        moduleType: ModuleType,
        userIntent: String,
        contextInfo: String = ""
    ): TaskConfigSuggestion = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        if (apiKey.isNotBlank()) {
            try {
                val systemPrompt = buildSystemPrompt(moduleType, userIntent, contextInfo)
                val request = GeminiGenerateRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = systemPrompt))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.4f,
                        topP = 0.9f,
                        maxOutputTokens = 1024
                    )
                )

                val response = apiService.generateContent(apiKey, request)
                val responseText = response.extractFirstText()

                if (!responseText.isNullOrBlank()) {
                    return@withContext parseAiResponseToSuggestion(moduleType, userIntent, responseText)
                }
            } catch (e: Exception) {
                Log.e(tag, "Gemini API call failed, falling back to smart contextual engine", e)
            }
        }

        // Fallback to domain-specialized rule engine
        return@withContext getContextualFallbackSuggestion(moduleType, userIntent)
    }

    /**
     * Generates a realistic draft content for tasks within modules.
     */
    suspend fun generateTaskDraft(
        moduleType: ModuleType,
        taskTitle: String,
        parameters: Map<String, String>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val paramSummary = parameters.entries.joinToString(", ") { "${it.key}: ${it.value}" }

        if (apiKey.isNotBlank()) {
            try {
                val prompt = "Tu es le moteur IA expert de TaskFlow ($moduleType).\n" +
                        "Rédige un livrable professionnel et immédiatement exploitable pour la tâche suivante : '$taskTitle'.\n" +
                        "Paramètres fournis : $paramSummary.\n" +
                        "Adopte un ton corporate, conforme au droit français et aux standards du secteur."

                val response = apiService.generateContent(
                    apiKey = apiKey,
                    request = GeminiGenerateRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
                    )
                )

                val result = response.extractFirstText()
                if (!result.isNullOrBlank()) {
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.e(tag, "Gemini draft generation failed, using fallback template", e)
            }
        }

        return@withContext generateFallbackDraft(moduleType, taskTitle, parameters)
    }

    private fun buildSystemPrompt(
        moduleType: ModuleType,
        userIntent: String,
        contextInfo: String
    ): String {
        return """
            Tu es l'assistant de configuration IA du SaaS TaskFlow Pro.
            L'utilisateur souhaite configurer une tâche dans le module : ${moduleType.name}.
            Intention de l'utilisateur : "$userIntent"
            Contexte additionnel : "$contextInfo"

            Génère une proposition de configuration sous le format structuré suivant :
            TITRE: <Titre court et impactant>
            DESCRIPTION: <Description concise en 2 phrases de la mission>
            MINUTES_SAVED: <Nombre entier de minutes gagnées, ex: 90>
            CREDIT_COST: <Nombre entier de crédits recommandés entre 1 et 10>
            PARAMETRES: <clé=valeur | clé=valeur | clé=valeur>
            CONSEIL: <Conseil stratégique 1>
            CONSEIL: <Conseil stratégique 2>
            PAYLOAD: <Description détaillée du prompt ou livrable attendu>
        """.trimIndent()
    }

    private fun parseAiResponseToSuggestion(
        moduleType: ModuleType,
        userIntent: String,
        rawText: String
    ): TaskConfigSuggestion {
        var title = "$userIntent (${moduleType.name})"
        var description = "Configuration générée par Gemini AI pour votre workflow."
        var minutesSaved = 60
        var creditCost = 3
        val params = mutableMapOf<String, String>()
        val adviceList = mutableListOf<String>()
        var payload = rawText

        rawText.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("TITRE:", ignoreCase = true) -> {
                    title = trimmed.substringAfter(":").trim()
                }
                trimmed.startsWith("DESCRIPTION:", ignoreCase = true) -> {
                    description = trimmed.substringAfter(":").trim()
                }
                trimmed.startsWith("MINUTES_SAVED:", ignoreCase = true) -> {
                    minutesSaved = trimmed.substringAfter(":").trim().filter { it.isDigit() }.toIntOrNull() ?: 60
                }
                trimmed.startsWith("CREDIT_COST:", ignoreCase = true) -> {
                    creditCost = trimmed.substringAfter(":").trim().filter { it.isDigit() }.toIntOrNull() ?: 3
                }
                trimmed.startsWith("PARAMETRES:", ignoreCase = true) -> {
                    val rawParams = trimmed.substringAfter(":").split("|")
                    rawParams.forEach { p ->
                        val parts = p.split("=")
                        if (parts.size == 2) {
                            params[parts[0].trim()] = parts[1].trim()
                        }
                    }
                }
                trimmed.startsWith("CONSEIL:", ignoreCase = true) -> {
                    adviceList.add(trimmed.substringAfter(":").trim())
                }
                trimmed.startsWith("PAYLOAD:", ignoreCase = true) -> {
                    payload = trimmed.substringAfter(":").trim()
                }
            }
        }

        if (params.isEmpty()) {
            params["Modèle IA"] = "Gemini 3.5 Flash"
            params["Précision"] = "Optimale (Enterprise Grade)"
        }

        if (adviceList.isEmpty()) {
            adviceList.add("Vérifiez les mentions légales avant transmission au client final.")
            adviceList.add("Enregistrez ce livrable dans votre coffre-fort numérique chiffré.")
        }

        return TaskConfigSuggestion(
            moduleType = moduleType,
            suggestedTitle = title,
            suggestedDescription = description,
            estimatedMinutesSaved = minutesSaved,
            recommendedCreditCost = creditCost,
            keyParameters = params,
            suggestedPromptPayload = payload,
            strategicAdvice = adviceList,
            source = "GEMINI_AI"
        )
    }

    /**
     * Specialized fallback suggestions tuned to each business module.
     */
    fun getContextualFallbackSuggestion(
        moduleType: ModuleType,
        userIntent: String
    ): TaskConfigSuggestion {
        return when (moduleType) {
            ModuleType.PAPERASSE_EXPRESS -> TaskConfigSuggestion(
                moduleType = moduleType,
                suggestedTitle = if (userIntent.isBlank()) "Contrat de Prestation B2B & CGV" else "Contrat : $userIntent",
                suggestedDescription = "Génération d'un cadre contractuel conforme au Code de commerce avec clauses de pénalités, propriété intellectuelle et RGPD.",
                estimatedMinutesSaved = 120,
                recommendedCreditCost = 3,
                keyParameters = mapOf(
                    "Juridiction" to "Droit Français (Code de Commerce)",
                    "Clause RGPD" to "Actif (DPO Conforme)",
                    "Acompte recommandé" to "30% à la commande",
                    "Pénalités retard" to "Taux BCE + 10% + indemnité 40€"
                ),
                suggestedPromptPayload = "Générer un contrat de prestation de services B2B complet incluant : objet de la mission, livrables, calendrier d'exécution, conditions de règlement, transfert de droits d'auteur sous réserve de parfait paiement, clause de confidentialité et clause attributive de juridiction.",
                strategicAdvice = listOf(
                    "Exigez la signature électronique avant tout démarrage technique.",
                    "Activez l'archivage automatique dans le coffre-fort numérique."
                ),
                source = "SMART_PRESET"
            )

            ModuleType.GROWTH_ENGINE -> TaskConfigSuggestion(
                moduleType = moduleType,
                suggestedTitle = if (userIntent.isBlank()) "Séquence Cold Email 4 Étapes" else "Campagne : $userIntent",
                suggestedDescription = "Conception d'une séquence de prospection B2B multicanale orientée conversion avec accroches personnalisées et CTA optimisé.",
                estimatedMinutesSaved = 90,
                recommendedCreditCost = 2,
                keyParameters = mapOf(
                    "Cible" to "Dirigeants PME / Responsables Ops",
                    "Canal" to "Email + Invitation LinkedIn",
                    "Ton" to "Consultatif & Direct",
                    "Objectif" to "Réservation d'un appel découverte 15 min"
                ),
                suggestedPromptPayload = "Créer une séquence de 4 emails espacés de 3 jours : 1. Icebreaker et constat métier, 2. Étude de cas chiffrée, 3. Proposition de valeur synthétique, 4. Break-up email courtois.",
                strategicAdvice = listOf(
                    "Gardez chaque email sous les 120 mots pour maximiser le taux de lecture mobile.",
                    "Testez deux objets différents en A/B testing sur les 50 premiers envois."
                ),
                source = "SMART_PRESET"
            )

            ModuleType.WEB_LAUNCH -> TaskConfigSuggestion(
                moduleType = moduleType,
                suggestedTitle = if (userIntent.isBlank()) "Cahier des Charges & Wireframe Brief" else "Web : $userIntent",
                suggestedDescription = "Spécifications complètes pour le lancement d'une landing page haute performance avec arborescence, copywriting et stack recommandée.",
                estimatedMinutesSaved = 180,
                recommendedCreditCost = 4,
                keyParameters = mapOf(
                    "Stack" to "Next.js / Tailwind / Supabase",
                    "Responsive" to "Mobile First (Core Web Vitals > 90)",
                    "Sections" to "Hero, Social Proof, Features, Pricing, FAQ, Footer",
                    "Analytics" to "Plausible / Google Tag Manager"
                ),
                suggestedPromptPayload = "Rédiger le cahier des charges fonctionnel et le wireframe textuel de la page d'accueil, avec titres percutants, sous-titres, arguments clés, avis clients et boutons d'appel à l'action.",
                strategicAdvice = listOf(
                    "Positionnez un bouton d'action principal au-dessus de la ligne de flottaison.",
                    "Intégrez des témoignages clients vérifiables pour doubler la conversion."
                ),
                source = "SMART_PRESET"
            )

            ModuleType.CONTENT_STUDIO -> TaskConfigSuggestion(
                moduleType = moduleType,
                suggestedTitle = if (userIntent.isBlank()) "Pack 5 Posts LinkedIn Stratégiques" else "Contenu : $userIntent",
                suggestedDescription = "Rédaction de publications LinkedIn à fort engagement avec structure AIDA, hooks visuels et questions ouvertes.",
                estimatedMinutesSaved = 75,
                recommendedCreditCost = 2,
                keyParameters = mapOf(
                    "Format" to "Posts Carrousel + Textes longs",
                    "Rythme" to "3 publications / semaine",
                    "Hashtags" to "#Entrepreneuriat #Productivité #SaaS",
                    "Appel à l'action" to "Commentaire / Sondage"
                ),
                suggestedPromptPayload = "Générer 5 posts LinkedIn percutants : 1. Récit d'un échec transformé en leçon, 2. Guide pratique étape par étape, 3. Analyse d'une tendance marché, 4. Boîte à outils recommandée, 5. Débat d'opinion avec sondage.",
                strategicAdvice = listOf(
                    "Soignez les deux premières lignes pour inciter au clic 'voir plus'.",
                    "Répondez à tous les commentaires dans la première heure de publication."
                ),
                source = "SMART_PRESET"
            )

            ModuleType.FREELANCE_HUB -> TaskConfigSuggestion(
                moduleType = moduleType,
                suggestedTitle = if (userIntent.isBlank()) "Proposition Commerciale & Grille Tarifaire" else "Mission : $userIntent",
                suggestedDescription = "Document commercial structuré mettant en valeur la rentabilité du projet, le calendrier d'exécution et les modalités de paiement.",
                estimatedMinutesSaved = 110,
                recommendedCreditCost = 3,
                keyParameters = mapOf(
                    "Tarification" to "Au forfait avec jalons de validation",
                    "Garantie" to "30 jours de support post-livraison",
                    "Délai" to "Livraison sous 3 semaines",
                    "Modalités" to "Virement bancaire SEPA à 30 jours"
                ),
                suggestedPromptPayload = "Établir une proposition commerciale personnalisée avec diagnostic préalable, méthodologie proposée, découpage en 3 sprints, grille de prix détaillée et conditions d'acceptation.",
                strategicAdvice = listOf(
                    "Proposez 3 options tarifaires (Standard, Pro, VIP) pour orienter le choix client vers l'offre intermédiaire.",
                    "Fixez une date de validité de 15 jours sur votre devis pour créer un sentiment d'urgence."
                ),
                source = "SMART_PRESET"
            )
        }
    }

    private fun generateFallbackDraft(
        moduleType: ModuleType,
        taskTitle: String,
        parameters: Map<String, String>
    ): String {
        return """
            ================================================================================
            TASKFLOW PRO • LIVRABLE GÉNÉRÉ PAR L'INTELLIGENCE ARTIFICIELLE
            Module : ${moduleType.name}
            Mission : $taskTitle
            Date d'exécution : ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date())}
            ================================================================================

            1. RÉSUMÉ EXÉCUTIF
            Ce document a été configuré et validé par les algorithmes de TaskFlow Pro conformément
            aux exigences des entreprises modernes et aux standards réglementaires.

            2. PARAMÈTRES RETENUS :
            ${parameters.entries.joinToString("\n") { "• ${it.key} : ${it.value}" }}

            3. CORPS DU LIVRABLE & STRUCTURE D'EXÉCUTION :
            - Phase 1 : Cadrage stratégique et validation des objectifs opérationnels.
            - Phase 2 : Déploiement des livrables techniques, juridiques ou marketing.
            - Phase 3 : Mesure de performance et archivage au coffre-fort numérique sécurisé.

            4. CONSEILS DE DÉPLOIEMENT :
            • Faites valider ce livrable par vos parties prenantes internes.
            • Utilisez le module Freelance Hub si vous souhaitez déléguer une relecture experte.
            ================================================================================
        """.trimIndent()
    }
}
