package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.FirebaseFirestoreManager
import com.example.data.firestore.FirestoreTaskDto
import com.example.data.model.ModuleType
import com.example.data.model.TaskRecord
import com.example.data.model.TaskStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TaskStatusFilter(val label: String) {
    ALL("Toutes"),
    ACTIVE("En cours / Actives"),
    COMPLETED("Terminées"),
    FAILED("Échecs")
}

/**
 * TaskDashboardScreen displays:
 * 1. Live synchronization status with Cloud Firestore
 * 2. Visual summary of progress (Completion rate, Total Time Saved, Credits Spent, Module Breakdown)
 * 3. Categorized task list partitioned by modules (Paperasse Express, Content Studio, WebLaunch, Freelance Hub, Growth Engine)
 * 4. Status filtering (Active, Completed, Failed), search, and grouped module view
 * 5. Interactive task detail sheet with copyable deliverables
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDashboardScreen(
    localTasks: List<TaskRecord>,
    activeTenantId: String = "tenant_alpha",
    onSelectModule: (ModuleType) -> Unit,
    onExecuteQuickTask: (ModuleType, String, Int, Int) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time Firestore task observation with Room fallback
    val cloudTasksFlow = remember(activeTenantId) {
        FirebaseFirestoreManager.observeCloudTasks(activeTenantId)
    }
    val cloudTasksDto by cloudTasksFlow.collectAsState(initial = emptyList())

    // Merge cloud and local tasks seamlessly, preferring cloud when present
    val allTasks = remember(cloudTasksDto, localTasks) {
        if (cloudTasksDto.isNotEmpty()) {
            cloudTasksDto.map { it.toRoomEntity() }
        } else {
            localTasks
        }
    }

    var selectedStatusFilter by remember { mutableStateOf(TaskStatusFilter.ALL) }
    var selectedModuleFilter by remember { mutableStateOf<ModuleType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isGroupedByModule by remember { mutableStateOf(true) }
    var selectedTaskForDetails by remember { mutableStateOf<TaskRecord?>(null) }
    var showQuickCreateModal by remember { mutableStateOf(false) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    // Visual Progress Calculations
    val totalCount = allTasks.size
    val completedCount = allTasks.count { it.status == TaskStatus.COMPLETED }
    val activeCount = allTasks.count {
        it.status == TaskStatus.PROCESSING || it.status == TaskStatus.QUEUED || it.status == TaskStatus.DRAFT
    }
    val failedCount = allTasks.count { it.status == TaskStatus.FAILED }

    val completionRate = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()) else 0f
    val totalTimeSavedMinutes = allTasks.filter { it.status == TaskStatus.COMPLETED }.sumOf { it.timeSavedMinutes }
    val totalCreditsSpent = allTasks.sumOf { it.creditsCost }
    val successRate = if (completedCount + failedCount > 0) {
        ((completedCount.toFloat() / (completedCount + failedCount).toFloat()) * 100).toInt()
    } else 100

    // Filter tasks based on search, status, and module
    val filteredTasks = remember(allTasks, selectedStatusFilter, selectedModuleFilter, searchQuery) {
        allTasks.filter { task ->
            val matchesStatus = when (selectedStatusFilter) {
                TaskStatusFilter.ALL -> true
                TaskStatusFilter.ACTIVE -> task.status == TaskStatus.PROCESSING ||
                        task.status == TaskStatus.QUEUED ||
                        task.status == TaskStatus.DRAFT
                TaskStatusFilter.COMPLETED -> task.status == TaskStatus.COMPLETED
                TaskStatusFilter.FAILED -> task.status == TaskStatus.FAILED
            }

            val matchesModule = if (selectedModuleFilter == null) true else {
                task.moduleId.equals(selectedModuleFilter!!.id, ignoreCase = true)
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                task.title.contains(searchQuery, ignoreCase = true) ||
                        task.description.contains(searchQuery, ignoreCase = true) ||
                        task.taskType.contains(searchQuery, ignoreCase = true) ||
                        task.id.toString().contains(searchQuery)
            }

            matchesStatus && matchesModule && matchesSearch
        }
    }

    // Grouping by Module
    val tasksByModule = remember(filteredTasks) {
        filteredTasks.groupBy { task ->
            ModuleType.values().find { it.id.equals(task.moduleId, ignoreCase = true) }
                ?: ModuleType.PAPERASSE_EXPRESS
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("task_dashboard_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickCreateModal = true },
                containerColor = CyanSecondary,
                contentColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_create_task")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nouvelle Tâche")
                    Text(
                        text = "Nouvelle Tâche",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // =================================================================
            // 1. FIRESTORE REAL-TIME SYNC BANNER
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firestore_sync_status_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pulsing live dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldTertiary.copy(alpha = pulseAlpha))
                            )
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Firestore Synced",
                                tint = CyanSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Cloud Firestore Live Sync",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Espace : $activeTenantId • ${allTasks.size} tâches synchronisées",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = EmeraldTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Temps Réel",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldTertiary
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 2. VISUAL SUMMARY OF PROGRESS (HERO CARD + GAUGES)
            // =================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_progress_summary_card"),
                    shape = RoundedCornerShape(22.dp),
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
                            .padding(20.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SYNTHÈSE DU WORKFLOW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Progression Globale",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            // Completion Percentage Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldTertiary.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, EmeraldTertiary)
                            ) {
                                Text(
                                    text = "${(completionRate * 100).toInt()}% Terminé",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = EmeraldTertiary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Progress Bar with Rounded Ends
                        LinearProgressIndicator(
                            progress = { completionRate },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .testTag("task_global_progress_bar"),
                            color = CyanSecondary,
                            trackColor = Color(0xFF334155),
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Multi-status breakdown indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Completed
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldTertiary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$completedCount Terminées",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }

                            // Active / In Progress
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AmberAccent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$activeCount En cours",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }

                            // Failed
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$failedCount Échecs",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Key Metrics Row (4 summary stat blocks)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Time Saved
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = EmeraldTertiary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Temps Gagné", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                val hours = totalTimeSavedMinutes / 60
                                val mins = totalTimeSavedMinutes % 60
                                Text(
                                    text = if (hours > 0) "${hours}h ${mins}m" else "$mins min",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }

                            // Credits Spent
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = CyanSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Crédits Utilisés", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Text(
                                    text = "$totalCreditsSpent cr",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }

                            // Success Rate
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = AmberAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Taux Réussite", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Text(
                                    text = "$successRate%",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }

                            // Total Pipelines
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFFA78BFA),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Total Tâches", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Text(
                                    text = "$totalCount exéc.",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 3. MODULE-BY-MODULE PROGRESS BREAKDOWN
            // =================================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progression par Module (5 Domaines)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "5 Modules Actifs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ModuleType.values()) { module ->
                        val moduleTasks = allTasks.filter { it.moduleId.equals(module.id, ignoreCase = true) }
                        val moduleTotal = moduleTasks.size
                        val moduleCompleted = moduleTasks.count { it.status == TaskStatus.COMPLETED }
                        val moduleActive = moduleTasks.count {
                            it.status == TaskStatus.PROCESSING || it.status == TaskStatus.QUEUED || it.status == TaskStatus.DRAFT
                        }
                        val moduleTimeSaved = moduleTasks.filter { it.status == TaskStatus.COMPLETED }.sumOf { it.timeSavedMinutes }
                        val moduleRatio = if (moduleTotal > 0) (moduleCompleted.toFloat() / moduleTotal.toFloat()) else 0f
                        val isSelected = selectedModuleFilter == module

                        val moduleColor = Color(module.primaryColorHex)

                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clickable {
                                    selectedModuleFilter = if (isSelected) null else module
                                }
                                .testTag("module_progress_card_${module.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E1B4B) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) moduleColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(moduleColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getModuleIcon(module),
                                            contentDescription = module.title,
                                            tint = moduleColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (moduleActive > 0) AmberAccent.copy(alpha = 0.2f) else EmeraldTertiary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (moduleActive > 0) "$moduleActive actif" else "$moduleCompleted/$moduleTotal",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (moduleActive > 0) AmberAccent else EmeraldTertiary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = module.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = module.subtitle,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { moduleRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = moduleColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${(moduleRatio * 100).toInt()}% terminé",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = moduleColor
                                    )
                                    Text(
                                        text = "+${moduleTimeSaved}m",
                                        fontSize = 10.sp,
                                        color = EmeraldTertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 4. SEARCH & FILTER CONTROLS
            // =================================================================
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Liste des Tâches (${filteredTasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // View Mode Toggle (Grouped vs Flat)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isGroupedByModule = !isGroupedByModule }
                                .testTag("btn_toggle_view_mode"),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isGroupedByModule) Icons.Default.ViewAgenda else Icons.Default.ViewList,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = IndigoPrimary
                                )
                                Text(
                                    text = if (isGroupedByModule) "Par Module" else "Liste Simple",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IndigoPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Rechercher une tâche, livrable ou ID...",
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
                        .testTag("task_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanSecondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }

            // Status Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("task_status_filter_chips")
                ) {
                    items(TaskStatusFilter.values()) { filter ->
                        val count = when (filter) {
                            TaskStatusFilter.ALL -> allTasks.size
                            TaskStatusFilter.ACTIVE -> activeCount
                            TaskStatusFilter.COMPLETED -> completedCount
                            TaskStatusFilter.FAILED -> failedCount
                        }

                        FilterChip(
                            selected = selectedStatusFilter == filter,
                            onClick = { selectedStatusFilter = filter },
                            label = {
                                Text("${filter.label} ($count)", fontSize = 12.sp)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Module Filter Chips (Horizontal)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("task_module_filter_chips")
                ) {
                    item {
                        FilterChip(
                            selected = selectedModuleFilter == null,
                            onClick = { selectedModuleFilter = null },
                            label = { Text("Tous les modules", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanSecondary,
                                selectedLabelColor = Color(0xFF0F172A)
                            )
                        )
                    }

                    items(ModuleType.values()) { module ->
                        val isSelected = selectedModuleFilter == module
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedModuleFilter = if (isSelected) null else module
                            },
                            label = { Text(module.title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(module.primaryColorHex),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // =================================================================
            // 5. TASK ITEMS (GROUPED OR LINEAR)
            // =================================================================
            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Aucune tâche trouvée",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Modifiez les filtres de recherche ou lancez une nouvelle automatisation.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedStatusFilter = TaskStatusFilter.ALL
                                    selectedModuleFilter = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                            ) {
                                Text("Réinitialiser les filtres", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else if (isGroupedByModule) {
                // Grouped by Module Layout
                tasksByModule.forEach { (module, moduleTasks) ->
                    val moduleColor = Color(module.primaryColorHex)
                    val moduleDone = moduleTasks.count { it.status == TaskStatus.COMPLETED }

                    item(key = "header_${module.id}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(moduleColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getModuleIcon(module),
                                        contentDescription = null,
                                        tint = moduleColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = module.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = moduleColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${moduleTasks.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = moduleColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            TextButton(
                                onClick = { onSelectModule(module) }
                            ) {
                                Text("Ouvrir Module", fontSize = 11.sp, color = moduleColor)
                            }
                        }
                    }

                    items(moduleTasks, key = { it.id }) { task ->
                        TaskCardItem(
                            task = task,
                            module = module,
                            dateFormatter = dateFormatter,
                            onClick = { selectedTaskForDetails = task }
                        )
                    }
                }
            } else {
                // Linear List Layout
                items(filteredTasks, key = { it.id }) { task ->
                    val module = ModuleType.values().find { it.id.equals(task.moduleId, ignoreCase = true) }
                        ?: ModuleType.PAPERASSE_EXPRESS
                    TaskCardItem(
                        task = task,
                        module = module,
                        dateFormatter = dateFormatter,
                        onClick = { selectedTaskForDetails = task }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // =========================================================================
    // 6. TASK DETAIL INSPECTOR BOTTOM SHEET
    // =========================================================================
    if (selectedTaskForDetails != null) {
        val task = selectedTaskForDetails!!
        val module = ModuleType.values().find { it.id.equals(task.moduleId, ignoreCase = true) }
            ?: ModuleType.PAPERASSE_EXPRESS
        val moduleColor = Color(module.primaryColorHex)

        ModalBottomSheet(
            onDismissRequest = { selectedTaskForDetails = null },
            sheetState = detailSheetState,
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("task_detail_bottom_sheet")
            ) {
                // Header
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(moduleColor.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getModuleIcon(module),
                                contentDescription = null,
                                tint = moduleColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = module.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Tâche #${task.id} • ${task.taskType}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(onClick = {
                        coroutineScope.launch { detailSheetState.hide() }.invokeOnCompletion {
                            selectedTaskForDetails = null
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    TaskStatusBadge(status = task.status)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.description,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Crédits Déduits", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("${task.creditsCost} crédits", fontWeight = FontWeight.Bold, color = IndigoPrimary, fontSize = 14.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Temps Gagné", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("+${task.timeSavedMinutes} min", fontWeight = FontWeight.Bold, color = EmeraldTertiary, fontSize = 14.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1.2f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Date Exécution", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text(dateFormatter.format(Date(task.createdAt)), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Deliverable Result Viewer
                Text(
                    text = "Livrable Généré par l'IA :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        item {
                            Text(
                                text = if (task.generatedResult.isNotBlank()) task.generatedResult else "Aucun résultat généré pour cette tâche.",
                                fontSize = 12.sp,
                                color = Color(0xFFF1F5F9),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Copy & Re-run
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Task Deliverable", task.generatedResult)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Livrable copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_copy_deliverable"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanSecondary)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onExecuteQuickTask(
                                module,
                                "Relance : ${task.title}",
                                task.creditsCost,
                                task.timeSavedMinutes
                            )
                            coroutineScope.launch { detailSheetState.hide() }.invokeOnCompletion {
                                selectedTaskForDetails = null
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_rerun_task"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary, contentColor = Color(0xFF0F172A))
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Relancer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // =========================================================================
    // 7. QUICK TASK LAUNCH MODAL
    // =========================================================================
    if (showQuickCreateModal) {
        QuickCreateTaskDialog(
            onDismiss = { showQuickCreateModal = false },
            onLaunch = { module, title, credits, timeSaved ->
                onExecuteQuickTask(module, title, credits, timeSaved)
                showQuickCreateModal = false
            }
        )
    }
}

/**
 * Task Card Item displaying task title, module badge, execution status, and cost/time saved.
 */
@Composable
private fun TaskCardItem(
    task: TaskRecord,
    module: ModuleType,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    val moduleColor = Color(module.primaryColorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("task_card_${task.id}"),
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
                // Module Icon Box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(moduleColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getModuleIcon(module),
                        contentDescription = null,
                        tint = moduleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = dateFormatter.format(Date(task.createdAt)),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TaskStatusBadge(status = task.status)
                    }
                }
            }

            // Credits & Time saved
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${task.creditsCost} cr",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = IndigoPrimary
                )
                Text(
                    text = "+${task.timeSavedMinutes}m",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = EmeraldTertiary
                )
            }
        }
    }
}

/**
 * Colored Status Badge for Tasks (COMPLETED, PROCESSING, QUEUED, DRAFT, FAILED)
 */
@Composable
private fun TaskStatusBadge(status: TaskStatus) {
    val (label, bgColor, textColor, icon) = when (status) {
        TaskStatus.COMPLETED -> Quadruple("Terminé", EmeraldTertiary.copy(alpha = 0.18f), EmeraldTertiary, Icons.Default.CheckCircle)
        TaskStatus.PROCESSING -> Quadruple("En cours", CyanSecondary.copy(alpha = 0.18f), CyanSecondary, Icons.Default.Pending)
        TaskStatus.QUEUED -> Quadruple("En attente", AmberAccent.copy(alpha = 0.18f), AmberAccent, Icons.Default.HourglassEmpty)
        TaskStatus.DRAFT -> Quadruple("Brouillon", Color(0xFF64748B).copy(alpha = 0.18f), Color(0xFF94A3B8), Icons.Default.Description)
        TaskStatus.FAILED -> Quadruple("Échec", Color(0xFFEF4444).copy(alpha = 0.18f), Color(0xFFEF4444), Icons.Default.ErrorOutline)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Returns the appropriate ImageVector for a given ModuleType.
 */
private fun getModuleIcon(module: ModuleType): ImageVector {
    return when (module) {
        ModuleType.PAPERASSE_EXPRESS -> Icons.Default.Description
        ModuleType.CONTENT_STUDIO -> Icons.Default.AutoAwesome
        ModuleType.WEB_LAUNCH -> Icons.Default.Language
        ModuleType.FREELANCE_HUB -> Icons.Default.WorkOutline
        ModuleType.GROWTH_ENGINE -> Icons.Default.TrendingUp
    }
}

/**
 * Quick Task Launch Dialog allowing the user to initiate a new task in any category.
 */
@Composable
private fun QuickCreateTaskDialog(
    onDismiss: () -> Unit,
    onLaunch: (ModuleType, String, Int, Int) -> Unit
) {
    var selectedModule by remember { mutableStateOf(ModuleType.PAPERASSE_EXPRESS) }
    var taskTitle by remember { mutableStateOf("") }
    var customCost by remember { mutableStateOf(selectedModule.defaultCost.toString()) }
    var customTimeSaved by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Lancer une Automatisation IA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sélectionnez le module cible :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ModuleType.values()) { mod ->
                        FilterChip(
                            selected = selectedModule == mod,
                            onClick = {
                                selectedModule = mod
                                customCost = mod.defaultCost.toString()
                            },
                            label = { Text(mod.title, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Titre de la tâche (ex: Devis Audit SEO)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customCost,
                        onValueChange = { customCost = it },
                        label = { Text("Coût (crédits)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customTimeSaved,
                        onValueChange = { customTimeSaved = it },
                        label = { Text("Gain (minutes)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val title = if (taskTitle.isBlank()) "Tâche ${selectedModule.title}" else taskTitle
                    val cost = customCost.toIntOrNull() ?: selectedModule.defaultCost
                    val time = customTimeSaved.toIntOrNull() ?: 45
                    onLaunch(selectedModule, title, cost, time)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary, contentColor = Color(0xFF0F172A))
            ) {
                Text("Exécuter", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
