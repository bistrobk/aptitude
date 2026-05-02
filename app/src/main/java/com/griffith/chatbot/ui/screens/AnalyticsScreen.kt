package com.griffith.chatbot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.griffith.chatbot.viewModel.AnalyticsViewModel
import com.griffith.chatbot.viewModel.AnalyticsUiState

// IMPORTANT: Import from your data.models package, not ui.screens
import com.griffith.chatbot.data.models.StudyAnalytics
import com.griffith.chatbot.data.models.DayProgress
import com.griffith.chatbot.data.models.MonthProgress
import com.griffith.chatbot.data.models.ModuleStats
import com.griffith.chatbot.data.models.ProductivityData
import com.griffith.chatbot.data.models.StudyGoal
import com.griffith.chatbot.data.models.Achievement

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Enhanced Data Models for Analytics
data class StudyAnalytics(
    val totalHours: Double = 0.0,
    val streakDays: Int = 0,
    val completedSessions: Int = 0,
    val averageSessionLength: Double = 0.0,
    val mostStudiedModule: String = "",
    val weeklyProgress: List<DayProgress> = emptyList(),
    val monthlyProgress: List<MonthProgress> = emptyList(),
    val moduleBreakdown: List<ModuleStats> = emptyList(),
    val productivityTrends: ProductivityData = ProductivityData(),
    val goals: List<StudyGoal> = emptyList(),
    val achievements: List<Achievement> = emptyList()
)

data class DayProgress(
    val date: LocalDate,
    val hoursStudied: Double,
    val sessionsCompleted: Int,
    val targetMet: Boolean
)

data class MonthProgress(
    val month: String,
    val hoursStudied: Double,
    val targetHours: Double
)

data class ModuleStats(
    val moduleId: String,
    val moduleName: String,
    val hoursStudied: Double,
    val sessionsCompleted: Int,
    val averageScore: Double,
    val progress: Float,
    val color: String
)

data class ProductivityData(
    val bestStudyTime: String = "Morning", // Morning, Afternoon, Evening, Night
    val averageFocusScore: Double = 0.0,
    val mostProductiveDay: String = "Monday",
    val studyHeatmap: Map<String, Double> = emptyMap() // Day -> Hours
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTimeframe by remember { mutableStateOf("Week") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Study Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.shadow(
                    4.dp,
                    RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            when (uiState) {
                is AnalyticsUiState.Loading -> {
                    LoadingState()
                }

                is AnalyticsUiState.Error -> {
                    ErrorState(
                        message = (uiState as AnalyticsUiState.Error).message,
                        onRetry = viewModel::refresh
                    )
                }

                is AnalyticsUiState.Success -> {
                    // Get the real analytics data from the success state
                    val analytics = (uiState as AnalyticsUiState.Success).analytics

                    AnalyticsContent(
                        analytics = analytics,
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeChange = { selectedTimeframe = it }
                    )
                }
            }
        }
    }
}
@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Analyzing your study data...",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Outlined.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Unable to load analytics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

@Composable
fun AnalyticsContent(
    analytics: com.griffith.chatbot.data.models.StudyAnalytics,
    selectedTimeframe: String,
    onTimeframeChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeframe Selector
        item {
            TimeframeSelectorCard(
                selectedTimeframe = selectedTimeframe,
                onTimeframeChange = onTimeframeChange
            )
        }

        // Key Metrics Overview
        item {
            OverviewMetricsCard(analytics = analytics)
        }

        // Study Progress Chart
        item {
            StudyProgressCard(
                weeklyProgress = analytics.weeklyProgress,
                timeframe = selectedTimeframe
            )
        }

        // Module Breakdown
        item {
            ModuleBreakdownCard(modules = analytics.moduleBreakdown)
        }

        // Productivity Insights
        item {
            ProductivityInsightsCard(productivity = analytics.productivityTrends)
        }

        // Goals Progress
        if (analytics.goals.isNotEmpty()) {
            item {
                GoalsProgressCard(goals = analytics.goals)
            }
        }

        // Recent Achievements
        if (analytics.achievements.isNotEmpty()) {
            item {
                AchievementsCard(achievements = analytics.achievements)
            }
        }
    }
}

@Composable
fun TimeframeSelectorCard(
    selectedTimeframe: String,
    onTimeframeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Time Period",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val timeframes = listOf("Week", "Month", "Semester", "Year")
                items(timeframes) { timeframe ->
                    FilterChip(
                        selected = selectedTimeframe == timeframe,
                        onClick = { onTimeframeChange(timeframe) },
                        label = { Text(timeframe) }
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewMetricsCard(analytics: StudyAnalytics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Study Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    value = "${analytics.totalHours.toInt()}h",
                    label = "Total Hours",
                    icon = Icons.Outlined.Schedule,
                    color = MaterialTheme.colorScheme.primary
                )
                MetricItem(
                    value = "${analytics.streakDays}",
                    label = "Day Streak",
                    icon = Icons.Outlined.LocalFireDepartment,
                    color = MaterialTheme.colorScheme.secondary
                )
                MetricItem(
                    value = "${analytics.completedSessions}",
                    label = "Sessions",
                    icon = Icons.Outlined.CheckCircle,
                    color = MaterialTheme.colorScheme.tertiary
                )
                MetricItem(
                    value = "${analytics.averageSessionLength.toInt()}m",
                    label = "Avg Session",
                    icon = Icons.Outlined.Timer,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun StudyProgressCard(
    weeklyProgress: List<com.griffith.chatbot.data.models.DayProgress>,
    timeframe: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "$timeframe Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Simple progress visualization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyProgress.take(7).forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height((day.hoursStudied * 10 + 20).dp)
                                .background(
                                    if (day.targetMet)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            day.date.format(DateTimeFormatter.ofPattern("EEE")).take(1),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModuleBreakdownCard(modules: List<com.griffith.chatbot.data.models.ModuleStats>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Module Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            modules.take(5).forEach { module ->
                ModuleProgressItem(module = module)
                if (module != modules.last()) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ModuleProgressItem(module: ModuleStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    Color(android.graphics.Color.parseColor(module.color)),
                    CircleShape
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                module.moduleName,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                "${module.hoursStudied.toInt()}h • ${module.sessionsCompleted} sessions",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${(module.progress * 100).toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ProductivityInsightsCard(productivity: com.griffith.chatbot.data.models.ProductivityData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Productivity Insights",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightItem(
                    icon = Icons.Outlined.WbSunny,
                    label = "Best Time",
                    value = productivity.bestStudyTime
                )
                InsightItem(
                    icon = Icons.Outlined.TrendingUp,
                    label = "Focus Score",
                    value = "${(productivity.averageFocusScore * 10).toInt()}/10"
                )
                InsightItem(
                    icon = Icons.Outlined.CalendarToday,
                    label = "Best Day",
                    value = productivity.mostProductiveDay.take(3)
                )
            }
        }
    }
}

@Composable
fun InsightItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun GoalsProgressCard(goals: List<StudyGoal>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Goals Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            goals.take(3).forEach { goal ->
                GoalProgressItem(goal = goal)
                if (goal != goals.take(3).last()) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun GoalProgressItem(goal: StudyGoal) {
    val progress = if (goal.targetValue > 0) {
        (goal.currentValue / goal.targetValue).toFloat().coerceIn(0f, 1f)
    } else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                goal.title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun AchievementsCard(achievements: List<Achievement>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Recent Achievements",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements.take(5)) { achievement ->
                    AchievementBadge(achievement = achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementBadge(achievement: Achievement) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                achievement.icon.ifEmpty { "🏆" },
                fontSize = 24.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            achievement.title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 2
        )
    }
}

