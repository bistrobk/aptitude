package com.griffith.chatbot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.griffith.chatbot.utils.AnimatedScaffold
import com.griffith.chatbot.viewModel.HomeViewModel
import com.griffith.chatbot.viewModel.HomeUiState
import com.griffith.chatbot.data.models.Module
import com.griffith.chatbot.data.models.Lecture
import com.griffith.chatbot.data.models.StudyEvent
import java.text.SimpleDateFormat
import java.util.*



@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToModules: () -> Unit = {},
    onNavigateToLectures: () -> Unit = {},
    onNavigateToStudyPlanner: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onModuleClick: (String) -> Unit = {},
    onLectureClick: (String, String) -> Unit = { _, _ -> },
    onEventClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Get user data from DatabaseManager directly
    val databaseManager = remember { com.griffith.chatbot.data.repository.DatabaseManager.getInstance() }
    var userName by remember { mutableStateOf("User") }
    var userEmail by remember { mutableStateOf("user@example.com") }

    LaunchedEffect(Unit) {
        // Load user profile
        databaseManager.getUserProfile().onSuccess { profile ->
            userName = profile?.name ?: "User"
        }
        // Get email from auth
        userEmail = databaseManager.getCurrentUserEmail() ?: "user@example.com"
    }

    AnimatedScaffold(
        title = "aptitude",
        userName = userName,
        userEmail = userEmail
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is HomeUiState.Loading -> {
                    LoadingState()
                }
                is HomeUiState.Error -> {
                    ErrorState(
                        message = (uiState as HomeUiState.Error).message,
                        onRetry = { viewModel.refresh() }
                    )
                }
                is HomeUiState.Success -> {
                    HomeContent(
                        modules = (uiState as HomeUiState.Success).modules,
                        recentLectures = (uiState as HomeUiState.Success).recentLectures,
                        upcomingEvents = (uiState as HomeUiState.Success).upcomingEvents,
                        studyStats = (uiState as HomeUiState.Success).studyStats,
                        onNavigateToModules = onNavigateToModules,
                        onNavigateToLectures = onNavigateToLectures,
                        onNavigateToStudyPlanner = onNavigateToStudyPlanner,
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToNotes = onNavigateToNotes,
                        onNavigateToProfile = onNavigateToProfile,
                        onModuleClick = onModuleClick,
                        onLectureClick = onLectureClick,
                        onEventClick = onEventClick
                    )
                }
            }
        }
    }
}



@Composable
fun HomeContent(
    modules: List<Module>,
    recentLectures: List<Lecture>,
    upcomingEvents: List<StudyEvent>,
    studyStats: Map<String, Any>,
    onNavigateToModules: () -> Unit,
    onNavigateToLectures: () -> Unit,
    onNavigateToStudyPlanner: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onModuleClick: (String) -> Unit,
    onLectureClick: (String, String) -> Unit,
    onEventClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome Section
        item {
            WelcomeSection()
        }

        // Quick Actions
        item {
            QuickActionsSection(
                onNavigateToModules = onNavigateToModules,
                onNavigateToLectures = onNavigateToLectures,
                onNavigateToStudyPlanner = onNavigateToStudyPlanner,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToNotes = onNavigateToNotes,
                onNavigateToProfile = onNavigateToProfile
            )
        }

        // Study Overview
        item {
            StudyOverviewSection(studyStats = studyStats)
        }

        // Active Modules
        if (modules.isNotEmpty()) {
            item {
                ActiveModulesSection(
                    modules = modules,
                    onModuleClick = onModuleClick,
                    onSeeAllClick = onNavigateToModules
                )
            }
        }

        // Recent Lectures
        if (recentLectures.isNotEmpty()) {
            item {
                RecentLecturesSection(
                    lectures = recentLectures,
                    onLectureClick = onLectureClick,
                    onSeeAllClick = onNavigateToLectures
                )
            }
        }

        // Upcoming Events
        if (upcomingEvents.isNotEmpty()) {
            item {
                UpcomingEventsSection(
                    events = upcomingEvents,
                    onEventClick = onEventClick,
                    onSeeAllClick = onNavigateToStudyPlanner
                )
            }
        }

        // Spacer for bottom navigation
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun WelcomeSection() {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 0..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = greeting,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ready to continue your learning journey?",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onNavigateToModules: () -> Unit,
    onNavigateToLectures: () -> Unit,
    onNavigateToStudyPlanner: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val quickActions = listOf(
        QuickAction(
            title = "Modules",
            icon = Icons.Outlined.LibraryBooks,
            color = MaterialTheme.colorScheme.primary,
            onClick = onNavigateToModules
        ),
        QuickAction(
            title = "Lectures",
            icon = Icons.Outlined.PlayCircleOutline,
            color = MaterialTheme.colorScheme.secondary,
            onClick = onNavigateToLectures
        ),

        QuickAction(
            title = "Analytics",
            icon = Icons.Outlined.Analytics,
            color = MaterialTheme.colorScheme.primary,
            onClick = onNavigateToAnalytics
        ),

        QuickAction(
            title = "Profile",
            icon = Icons.Outlined.Person,
            color = MaterialTheme.colorScheme.tertiary,
            onClick = onNavigateToProfile
        )
    )

    Column {
        Text(
            text = "Quick Actions",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Create rows of 3 actions each
        quickActions.chunked(3).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowActions.forEach { action ->
                    QuickActionCard(
                        action = action,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if row has less than 3 items
                repeat(3 - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun QuickActionCard(
    action: QuickAction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { action.onClick() },
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        action.color.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = action.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudyOverviewSection(studyStats: Map<String, Any>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Study Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${studyStats["total_modules"] ?: 0}",
                    label = "Modules",
                    icon = Icons.Outlined.LibraryBooks,
                    containerColor = colorScheme.primary
                )
                StatItem(
                    value = "${studyStats["total_lectures"] ?: 0}",
                    label = "Lectures",
                    icon = Icons.Outlined.PlayCircleOutline,
                    containerColor = colorScheme.secondary
                )
                StatItem(
                    value = "${studyStats["total_study_events"] ?: 0}",
                    label = "Events",
                    icon = Icons.Outlined.CalendarToday,
                    containerColor = colorScheme.onTertiaryContainer
                )
            }
        }
    }
}



@Composable
fun ActiveModulesSection(
    modules: List<Module>,
    onModuleClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Modules",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onSeeAllClick) {
                Text("See All")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(modules.take(5)) { module ->
                ModuleCard(
                    module = module,
                    onClick = { onModuleClick(module.id) }
                )
            }
        }
    }
}

@Composable
fun ModuleCard(
    module: Module,
    onClick: () -> Unit
) {
    val moduleColor = Color(android.graphics.Color.parseColor(module.color))

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            moduleColor.copy(alpha = 0.9f),
                            moduleColor.copy(alpha = 0.7f),
                            moduleColor.copy(alpha = 0.5f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    ),
                    RoundedCornerShape(20.dp)
                )
        ) {
            // Background pattern overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent,
                                moduleColor.copy(alpha = 0.2f)
                            ),
                            radius = 300f
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with module code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = module.moduleCode,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Credits or semester info if available
                    if (module.credits > 0) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${module.credits}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Middle section with module name
                Column {
                    Text(
                        text = module.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = module.lecturerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom section with progress or additional info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Progress indicator if available
                    if (module.totalLectures > 0) {
                        Column {
                            Text(
                                text = "${module.completedLectures}/${module.totalLectures} lectures",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = if (module.totalLectures > 0)
                                    module.completedLectures.toFloat() / module.totalLectures.toFloat()
                                else 0f,
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // Arrow indicator
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentLecturesSection(
    lectures: List<Lecture>,
    onLectureClick: (String, String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Lectures",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onSeeAllClick) {
                Text("See All")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        lectures.take(3).forEach { lecture ->
            LectureListItem(
                lecture = lecture,
                onClick = { onLectureClick(lecture.moduleId, lecture.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun LectureListItem(
    lecture: Lecture,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.PlayCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lecture.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (lecture.description.isNotEmpty()) {
                    Text(
                        text = lecture.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
fun UpcomingEventsSection(
    events: List<StudyEvent>,
    onEventClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Events",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onSeeAllClick) {
                Text("See All")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        events.take(3).forEach { event ->
            EventListItem(
                event = event,
                onClick = { onEventClick(event.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun EventListItem(
    event: StudyEvent,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(Date(event.startTime)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}