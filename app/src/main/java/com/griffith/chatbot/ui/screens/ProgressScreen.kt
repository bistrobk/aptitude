package com.griffith.chatbot.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.griffith.chatbot.data.models.*
import com.griffith.chatbot.ui.common.CreateEventDialog
import com.griffith.chatbot.viewModel.ProgressUiState
import com.griffith.chatbot.viewModel.ProgressViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit = {}
)
@Composable
fun getEventColor(type: EventType): Color {
    return when (type) {
        EventType.STUDY -> colorScheme.primary
        EventType.EXAM -> colorScheme.error
        EventType.REVISION -> colorScheme.tertiary
        EventType.ASSIGNMENT -> colorScheme.secondary
        EventType.PROJECT -> colorScheme.secondary
        EventType.RESEARCH -> colorScheme.outline
    }
}

fun getEventIcon(type: EventType): ImageVector {
    return when (type) {
        EventType.STUDY -> Icons.Default.Book
        EventType.EXAM -> Icons.Default.Edit
        EventType.REVISION -> Icons.Default.Refresh
        EventType.ASSIGNMENT -> Icons.Default.CheckCircle
        EventType.PROJECT -> Icons.Default.Work
        EventType.RESEARCH -> Icons.Default.Search
    }
}

fun longToLocalDateTime(epochMillis: Long): LocalDateTime {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

@SuppressLint("DefaultLocale")
fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = viewModel(),
    onNavigateToTimer: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToModules: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Handle error messages with LaunchedEffect
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    if (uiState.showAddEventDialog) {
        CreateEventDialog(
            modules = uiState.modules,
            onDismiss = { viewModel.showAddEventDialog(false) },
            onConfirm = { event ->
                viewModel.addStudyEvent(event)
                viewModel.showAddEventDialog(false)
            }
        )
    }

    Scaffold(
        topBar = {
            ProgressTopBar(
                selectedTab = selectedTab,
                onAddEventClick = if (selectedTab == 0) {
                    { viewModel.showAddEventDialog(true) }
                } else null,
                onToggleTimetable = if (selectedTab == 1) {
                    { viewModel.toggleTimetableView() }
                } else null
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colorScheme.surface,
                                colorScheme.background
                            )
                        )
                    )
            ) {
                // Tab Row with icons
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = colorScheme.primary,
                            height = 3.dp
                        )
                    }
                ) {
                    val tabs = listOf(
                        "Overview" to Icons.Default.Dashboard,
                        "Calendar" to Icons.Default.CalendarToday,
                        "Timer" to Icons.Default.Timer
                    )

                    tabs.forEachIndexed { index, (title, icon) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = title,
                                    tint = if (selectedTab == index)
                                        colorScheme.primary
                                    else
                                        colorScheme.onSurfaceVariant
                                )
                            },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                // Tab content with animations
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { if (targetState > initialState) 300 else -300 }
                        ) + fadeIn() with
                                slideOutHorizontally(
                                    targetOffsetX = { if (targetState > initialState) -300 else 300 }
                                ) + fadeOut()
                    },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> OverviewTab(
                            uiState = uiState,
                            onAddEventClicked = { viewModel.showAddEventDialog(true) },
                            onNavigateToTimer = onNavigateToTimer,
                            onNavigateToStats = onNavigateToStats
                        )
                        1 -> CalendarTab(
                            selectedDate = uiState.selectedDate,
                            events = uiState.allEvents,
                            modules = uiState.modules,
                            onDateSelected = { viewModel.onDateSelected(it) },
                            showTimetable = uiState.showTimetableView,
                            onToggleTimetable = { viewModel.toggleTimetableView() }
                        )
                        2 -> TimerTab(
                            sessions = uiState.recentSessions,
                            modules = uiState.modules,
                            onStartSession = { moduleId ->
                                viewModel.startStudySession(moduleId)
                                onNavigateToTimer()
                            }
                        )
                    }
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading your study data...",
                            color = colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun TimerTab(
    sessions: List<StudySession>,
    modules: List<Module>,
    onStartSession: (String) -> Unit
) {
    var timerSeconds by remember { mutableStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedDuration by remember { mutableStateOf(25 * 60) }
    var selectedModuleId by remember { mutableStateOf("") }

    LaunchedEffect(isRunning, timerSeconds) {
        if (isRunning && timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        } else if (timerSeconds == 0 && isRunning) {
            isRunning = false
            // Create study session record
            if (selectedModuleId.isNotEmpty()) {
                onStartSession(selectedModuleId)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Module selector
        item {
            ModuleSelectorCard(
                modules = modules,
                selectedModuleId = selectedModuleId,
                onModuleSelected = { selectedModuleId = it }
            )
        }

        item {
            PomodoroTimerCard(
                timerSeconds = timerSeconds,
                isRunning = isRunning,
                selectedDuration = selectedDuration,
                onStartPause = {
                    if (selectedModuleId.isNotEmpty() || !isRunning) {
                        isRunning = !isRunning
                    }
                },
                onReset = {
                    isRunning = false
                    timerSeconds = selectedDuration
                },
                canStart = selectedModuleId.isNotEmpty()
            )
        }

        item {
            TimerPresetsCard { duration ->
                selectedDuration = duration
                if (!isRunning) timerSeconds = duration
            }
        }

        item {
            StudySessionHistoryCard(
                sessions = sessions,
                moduleMap = modules.associateBy({ it.id }, { it.name })
            )
        }
    }
}
@Composable
fun PomodoroTimerCard(
    timerSeconds: Int,
    isRunning: Boolean,
    selectedDuration: Int,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    canStart: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Study Timer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(24.dp))

            // Enhanced circular progress timer
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                val progress = if (selectedDuration > 0) {
                    timerSeconds.toFloat() / selectedDuration
                } else 1f

                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(1000),
                    label = "timer_progress"
                )

                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = when {
                        timerSeconds <= 300 -> colorScheme.error // Last 5 minutes
                        timerSeconds <= 600 -> Color(0xFFFF9800) // Last 10 minutes
                        else -> colorScheme.primary
                    },
                    trackColor = colorScheme.primary.copy(alpha = 0.2f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        formatTime(timerSeconds),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        when {
                            !isRunning && timerSeconds == selectedDuration -> "Ready to start"
                            isRunning -> "Focus time"
                            timerSeconds == 0 -> "Session complete!"
                            else -> "Paused"
                        },
                        fontSize = 14.sp,
                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Timer controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartPause,
                    modifier = Modifier.weight(1f),
                    enabled = canStart || isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning)
                            colorScheme.error
                        else
                            colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isRunning) "Pause" else "Start",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Reset",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!canStart && !isRunning) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Please select a module to start studying",
                    fontSize = 12.sp,
                    color = colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
@Composable
fun CalendarTab(
    selectedDate: LocalDate,
    events: List<StudyEvent>,
    modules: List<Module>,
    onDateSelected: (LocalDate) -> Unit,
    showTimetable: Boolean,
    onToggleTimetable: () -> Unit
) {
    // Generate timetable events from your existing Module.schedule
    val timetableEvents = if (showTimetable) {
        generateTimetableEventsForMonth(selectedDate, modules)
    } else emptyList()

    val allEvents = events + timetableEvents

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CalendarViewToggleCard(
                showTimetable = showTimetable,
                onToggle = onToggleTimetable
            )
        }
        item {
            CalendarWidget(
                selectedDate = selectedDate,
                events = allEvents,
                onDateSelected = onDateSelected
            )
        }
        item {
            DayEventsCard(
                selectedDate = selectedDate,
                events = allEvents.filter {
                    longToLocalDateTime(it.startTime).toLocalDate() == selectedDate
                }.sortedBy { it.startTime }
            )
        }
    }
}
@Composable
fun ModuleSelectorCard(
    modules: List<Module>,
    selectedModuleId: String,
    onModuleSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Select Module",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            if (modules.isEmpty()) {
                Text(
                    "No modules available. Add modules to track your study sessions.",
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(modules) { module ->
                        FilterChip(
                            selected = selectedModuleId == module.id,
                            onClick = {
                                onModuleSelected(
                                    if (selectedModuleId == module.id) "" else module.id
                                )
                            },
                            label = {
                                Text(
                                    module.name,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = if (selectedModuleId == module.id) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressTopBar(
    selectedTab: Int,
    onAddEventClick: (() -> Unit)? = null,
    onToggleTimetable: (() -> Unit)? = null
) {
    val title = when (selectedTab) {
        0 -> "Dashboard"
        1 -> "Calendar"
        2 -> "Study Timer"
        else -> "Progress Hub"
    }

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    when (selectedTab) {
                        0 -> Icons.Outlined.Dashboard
                        1 -> Icons.Outlined.CalendarToday
                        2 -> Icons.Outlined.Timer
                        else -> Icons.Outlined.Timeline
                    },
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
            }
        },
        actions = {
            when (selectedTab) {
                0 -> {
                    if (onAddEventClick != null) {
                        IconButton(onClick = onAddEventClick) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Event",
                                tint = colorScheme.primary
                            )
                        }
                    }
                }
                1 -> {
                    if (onToggleTimetable != null) {
                        IconButton(onClick = onToggleTimetable) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Toggle Timetable",
                                tint = colorScheme.primary
                            )
                        }
                    }
                }
                2 -> {
                    IconButton(onClick = { /* TODO: Timer settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Timer Settings",
                            tint = colorScheme.primary
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.surface.copy(alpha = 0.95f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
    )
}

// Overview Tab with better performance and new features
@Composable
fun OverviewTab(
    uiState: ProgressUiState,
    onAddEventClicked: () -> Unit,
    onNavigateToTimer: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    // Memoize expensive calculations
    val upcomingEvents by remember(uiState.allEvents) {
        derivedStateOf {
            uiState.allEvents
                .filter { longToLocalDateTime(it.startTime).isAfter(LocalDateTime.now()) }
                .sortedBy { it.startTime }
                .take(5)
        }
    }

    val nextClass by remember(uiState.nextClass) {
        derivedStateOf { uiState.nextClass }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome section with time-based greeting
        item {
            WelcomeCard(
                nextClass = nextClass,
                todayEventsCount = uiState.todayEvents.size
            )
        }

        // Quick actions
        item {
            QuickActionsCard(
                onAddEventClicked = onAddEventClicked,
                hasUpcomingEvents = upcomingEvents.isNotEmpty()
            )
        }

        // Today's schedule
        if (uiState.todayEvents.isNotEmpty()) {
            item {
                TodayScheduleCard(events = uiState.todayEvents)
            }
        }

        // Stats
        item {
            StatsOverviewCard(stats = uiState.studyStats)
        }

        // Streak card
        item {
            StreakCard(streakDays = uiState.studyStats.streakDays)
        }

        // Upcoming events
        item {
            UpcomingEventsCard(events = upcomingEvents)
        }

        // Module progress
        if (uiState.modules.isNotEmpty()) {
            item {
                ModuleProgressCard(modules = uiState.modules)
            }
        }
    }
}

@Composable
fun StreakCard(streakDays: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        colorScheme.secondary.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = "Study Streak",
                    tint = colorScheme.secondary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Study Streak",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSecondaryContainer
                )
                Text(
                    "$streakDays days in a row!",
                    fontSize = 14.sp,
                    color = colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}


@Composable
fun WelcomeCard(
    nextClass: StudyEvent?,
    todayEventsCount: Int
) {
    val greeting = remember {
        val hour = LocalDateTime.now().hour
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                greeting,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(8.dp))

            Text(
                buildString {
                    if (todayEventsCount > 0) {
                        append("You have $todayEventsCount events today")
                        nextClass?.let {
                            val timeUntil = java.time.Duration.between(
                                LocalDateTime.now(),
                                longToLocalDateTime(it.startTime)
                            )
                            if (timeUntil.toMinutes() < 60) {
                                append("\nNext class: ${it.title} in ${timeUntil.toMinutes()} minutes")
                            }
                        }
                    } else {
                        append("No events scheduled for today")
                    }
                },
                fontSize = 16.sp,
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun QuickActionsCard(
    onAddEventClicked: () -> Unit,

    hasUpcomingEvents: Boolean
) {
    // Move color scheme access outside of remember
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline

    val actions = remember(hasUpcomingEvents, primaryColor, secondaryColor, tertiaryColor, outlineColor) {
        listOf(

            QuickAction(
                title = "Add Event",
                icon = Icons.Filled.Add,
                color = secondaryColor,
                onClick = onAddEventClicked
            ),

            QuickAction(
                title = if (hasUpcomingEvents) "Review Schedule" else "Plan Study",
                icon = if (hasUpcomingEvents) Icons.Filled.Schedule else Icons.Filled.CalendarToday,
                color = outlineColor,
                onClick = { /* Navigate to calendar */ }
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Grid layout for better space utilization
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(120.dp)
            ) {
                items(actions) { action ->
                    QuickActionButton(action = action)
                }
            }
        }
    }
}
@Composable
fun QuickActionButton(action: QuickAction) {
    Card(
        onClick = action.onClick,
        colors = CardDefaults.cardColors(
            containerColor = action.color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = action.color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = action.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = action.color
            )
        }
    }
}

@Composable
fun TodayScheduleCard(events: List<StudyEvent>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Today's Schedule",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    "${events.size} events",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            events.take(4).forEach { event ->
                CompactEventItem(event)
                if (event != events.last() && events.indexOf(event) < 3) {
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (events.size > 4) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "...and ${events.size - 4} more",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CompactEventItem(event: StudyEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(30.dp)
                .background(
                    getEventColor(event.type),
                    RoundedCornerShape(2.dp)
                )
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text(
                "${longToLocalDateTime(event.startTime).format(DateTimeFormatter.ofPattern("HH:mm"))} • ${event.moduleName.ifEmpty { "General" }}",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        if (event.isFromTimetable) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = "Class",
                tint = colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun StatsOverviewCard(stats: StudyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Study Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.height(16.dp))

            // Weekly progress indicator
            WeeklyProgressIndicator(stats)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${stats.totalHours.toInt()}h",
                    label = "Total Hours",
                    icon = Icons.Outlined.Schedule,
                    containerColor = colorScheme.onTertiaryContainer
                )
                StatItem(
                    value = "${stats.streakDays}",
                    label = "Day Streak",
                    icon = Icons.Outlined.LocalFireDepartment,
                    containerColor = colorScheme.onTertiaryContainer
                )
                StatItem(
                    value = "${stats.completedModules}/${stats.totalModules}",
                    label = "Modules",
                    icon = Icons.Outlined.CheckCircle,
                    containerColor = colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
@Composable
fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    containerColor: Color = colorScheme.onPrimaryContainer
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = label,
            tint = colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = containerColor
        )
        Text(
            label,
            fontSize = 12.sp,
            color = containerColor.copy(alpha = 0.7f)
        )
    }
}
@Composable
fun WeeklyProgressIndicator(stats: StudyStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val currentDay = LocalDate.now().dayOfWeek.value - 1

        daysOfWeek.forEachIndexed { index, day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            when {
                                index == currentDay -> colorScheme.primary
                                index < currentDay -> colorScheme.primary.copy(alpha = 0.6f)
                                else -> colorScheme.outline.copy(alpha = 0.3f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index <= currentDay) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    day,
                    fontSize = 10.sp,
                    color = colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ModuleProgressCard(modules: List<Module>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Module Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            modules.take(3).forEach { module ->
                ModuleProgressItem(module)
                if (module != modules.take(3).last()) {
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (modules.size > 3) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "View all ${modules.size} modules",
                    fontSize = 12.sp,
                    color = colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Navigate to modules */ },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ModuleProgressItem(module: Module) {
    val progress = remember(module) {
        if (module.totalLectures > 0) {
            module.completedLectures.toFloat() / module.totalLectures
        } else 0f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                module.name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colorScheme.primary,
                trackColor = colorScheme.primary.copy(alpha = 0.2f)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "${(progress * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UpcomingEventsCard(events: List<StudyEvent>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Upcoming Events",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                if (events.isNotEmpty()) {
                    Text(
                        "Next ${events.size}",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (events.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.EventAvailable,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No upcoming events",
                        textAlign = TextAlign.Center,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Relax or plan your next study session!",
                        textAlign = TextAlign.Center,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

        }
    }
}
@Composable
fun TimerPresetsCard(onDurationSelected: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Quick Presets", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val presets = listOf("5 min" to 300, "15 min" to 900, "25 min" to 1500, "45 min" to 2700)
                items(presets) { (label, duration) ->
                    OutlinedButton(
                        onClick = { onDurationSelected(duration) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
fun StudySessionHistoryCard(sessions: List<StudySession>, moduleMap: Map<String, String>) {
    if (sessions.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Recent Sessions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            sessions.forEachIndexed { index, session ->
                val moduleName = moduleMap[session.moduleId] ?: "Unknown Module"
                val durationText = "${session.duration} min"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(moduleName, fontWeight = FontWeight.Medium)
                    Text(durationText, color = colorScheme.onSurfaceVariant)
                }

                if (index < sessions.lastIndex) {
                    HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}

// Supporting composables for CalendarTab
@Composable
fun CalendarViewToggleCard(
    showTimetable: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Show Class Schedule",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = showTimetable,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

// Helper function to generate timetable events for a specific month
fun generateTimetableEventsForMonth(
    selectedDate: LocalDate,
    modules: List<Module>
): List<StudyEvent> {
    val events = mutableListOf<StudyEvent>()
    val monthStart = selectedDate.withDayOfMonth(1)
    val monthEnd = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())

    modules.forEach { module ->
        module.schedule.forEach { classSchedule ->
            var currentDate = monthStart
            while (currentDate <= monthEnd) {
                if (currentDate.dayOfWeek.value == classSchedule.dayOfWeek) {
                    val startDateTime = LocalDateTime.of(currentDate, LocalTime.parse(classSchedule.startTime))
                    val endDateTime = LocalDateTime.of(currentDate, LocalTime.parse(classSchedule.endTime))

                    events.add(
                        StudyEvent(
                            id = "${module.id}_${classSchedule.hashCode()}_${currentDate}",
                            title = "${module.name} (${classSchedule.type})",
                            description = classSchedule.location,
                            moduleId = module.id,
                            moduleName = module.name,
                            startTime = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            endTime = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                            duration = java.time.Duration.between(startDateTime, endDateTime).toMinutes().toInt(),
                            type = when (classSchedule.type) {
                                "LECTURE" -> EventType.STUDY
                                "LAB" -> EventType.ASSIGNMENT
                                "TUTORIAL" -> EventType.REVISION
                                "SEMINAR" -> EventType.STUDY
                                else -> EventType.STUDY
                            },
                            location = classSchedule.location,
                            isFromTimetable = true,
                            classScheduleId = classSchedule.hashCode().toString(),
                            instructor = module.lecturerName
                        )
                    )
                }
                currentDate = currentDate.plusDays(1)
            }
        }
    }

    return events
}

@Composable
fun CalendarWidget(
    selectedDate: LocalDate,
    events: List<StudyEvent>,
    onDateSelected: (LocalDate) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onDateSelected(selectedDate.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    "${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedDate.year}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onDateSelected(selectedDate.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            CalendarGrid(selectedDate, events, onDateSelected)
        }
    }
}

@Composable
fun CalendarGrid(
    selectedDate: LocalDate,
    events: List<StudyEvent>,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDayOfMonth = selectedDate.withDayOfMonth(1)
    val startDate = firstDayOfMonth.minusDays(firstDayOfMonth.dayOfWeek.value % 7L)

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(280.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(42) { index ->
            val date = startDate.plusDays(index.toLong())
            val dayEvents = events.filter {
                longToLocalDateTime(it.startTime).toLocalDate() == date
            }
            val isSelected = date == selectedDate
            val isToday = date == today
            val isCurrentMonth = date.month == selectedDate.month

            CalendarDay(
                date = date,
                events = dayEvents,
                isSelected = isSelected,
                isToday = isToday,
                isCurrentMonth = isCurrentMonth,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
fun CalendarDay(
    date: LocalDate,
    events: List<StudyEvent>,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> colorScheme.primary
                    isToday -> colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 12.sp,
                color = when {
                    isSelected -> colorScheme.onPrimary
                    !isCurrentMonth -> colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> colorScheme.onSurface
                },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )

            // Show event indicators
            if (events.isNotEmpty() && isCurrentMonth) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(events.take(3)) { event ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    if (event.isFromTimetable)
                                        colorScheme.secondary.copy(alpha = 0.8f)
                                    else getEventColor(event.type),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                    if (events.size > 3) {
                        item {
                            Text(
                                "+${events.size - 3}",
                                fontSize = 8.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayEventsCard(
    selectedDate: LocalDate,
    events: List<StudyEvent>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Schedule for ${selectedDate.format(DateTimeFormatter.ofPattern("MMMM dd, EEEE"))}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            if (events.isEmpty()) {
                Text(
                    "No events scheduled for this day.",
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else {
                // Group events by type
                val (timetableEvents, userEvents) = events.partition { it.isFromTimetable }

                if (timetableEvents.isNotEmpty()) {
                    Text(
                        "Classes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    timetableEvents.forEach { event ->
                        EventItem(event)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (userEvents.isNotEmpty()) {
                    if (timetableEvents.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        "Personal Events",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    userEvents.forEach { event ->
                        EventItem(event)
                        if (event != userEvents.last()) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun EventItem(event: StudyEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (event.isFromTimetable)
                    colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else colorScheme.primaryContainer.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    getEventColor(event.type).copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (event.isFromTimetable) Icons.Default.School else getEventIcon(event.type),
                contentDescription = event.type.name,
                tint = getEventColor(event.type),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            val timeText = if (event.endTime > 0) {
                "${longToLocalDateTime(event.startTime).format(DateTimeFormatter.ofPattern("HH:mm"))} - ${
                    longToLocalDateTime(event.endTime).format(DateTimeFormatter.ofPattern("HH:mm"))
                }"
            } else {
                longToLocalDateTime(event.startTime).format(DateTimeFormatter.ofPattern("HH:mm"))
            }

            Text(
                buildString {
                    append(event.moduleName.ifEmpty { "General" })
                    append(" • ")
                    append(timeText)
                    if (event.location.isNotEmpty()) {
                        append(" • ")
                        append(event.location)
                    }
                    if (event.instructor.isNotEmpty()) {
                        append(" • ")
                        append(event.instructor)
                    }
                },
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        if (event.isFromTimetable) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = "Timetable Event",
                tint = colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
