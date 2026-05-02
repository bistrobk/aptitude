package com.griffith.chatbot.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.griffith.chatbot.data.models.ClassSchedule
import com.griffith.chatbot.data.models.Module

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ModuleCreationDialog(
    module: Module? = null,
    onDismiss: () -> Unit,
    onConfirm: (Module) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var moduleName by remember { mutableStateOf(module?.name ?: "") }
    var moduleCode by remember { mutableStateOf(module?.moduleCode ?: "") }
    var lecturerName by remember { mutableStateOf(module?.lecturerName ?: "") }
    var credits by remember { mutableStateOf(module?.credits?.toString() ?: "") }
    var semester by remember { mutableStateOf(module?.semester ?: "") }
    var description by remember { mutableStateOf(module?.description ?: "") }
    var selectedColor by remember { mutableStateOf(module?.color ?: "#6366F1") }
    var schedules by remember { mutableStateOf(module?.schedule ?: emptyList()) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val isEditing = module != null
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 50.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Enhanced Header with step indicator
                EnhancedHeader(
                    currentStep = currentStep,
                    totalSteps = 3,
                    isEditing = isEditing,
                    onBackClick = {
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            onDismiss()
                        }
                    }
                )

                // Content with smooth transitions
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { if (targetState > initialState) 300 else -300 }
                        ) + fadeIn() with slideOutHorizontally(
                            targetOffsetX = { if (targetState > initialState) -300 else 300 }
                        ) + fadeOut()
                    },
                    label = "step_transition"
                ) { step ->
                    when (step) {
                        0 -> ThemeSelectionStep(
                            selectedColor = selectedColor,
                            onColorSelected = { selectedColor = it },
                            moduleName = moduleName,
                            onNext = { currentStep = 1 }
                        )
                        1 -> BasicDetailsStep(
                            moduleName = moduleName,
                            moduleCode = moduleCode,
                            description = description,
                            onModuleNameChange = { moduleName = it },
                            onModuleCodeChange = { moduleCode = it },
                            onDescriptionChange = { description = it },
                            onNext = {
                                if (moduleName.isNotBlank() && moduleCode.isNotBlank()) {
                                    currentStep = 2
                                }
                            },
                            canProceed = moduleName.isNotBlank() && moduleCode.isNotBlank()
                        )
                        2 -> AdditionalDetailsStep(
                            lecturerName = lecturerName,
                            credits = credits,
                            semester = semester,
                            schedules = schedules,
                            onLecturerNameChange = { lecturerName = it },
                            onCreditsChange = { credits = it },
                            onSemesterChange = { semester = it },
                            onAddSchedule = { showScheduleDialog = true },
                            onRemoveSchedule = { scheduleToRemove ->
                                schedules = schedules.filter { it != scheduleToRemove }
                            },
                            onComplete = {
                                val newModule = Module(
                                    id = module?.id ?: "",
                                    name = moduleName,
                                    moduleCode = moduleCode,
                                    lecturerName = lecturerName,
                                    credits = credits.toIntOrNull() ?: 0,
                                    semester = semester,
                                    description = description,
                                    color = selectedColor,
                                    schedule = schedules,
                                    createdAt = module?.createdAt ?: System.currentTimeMillis()
                                )
                                onConfirm(newModule)
                            },
                            isEditing = isEditing
                        )
                    }
                }
            }
        }
    }

    if (showScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showScheduleDialog = false },
            onConfirm = { schedule ->
                schedules = schedules + schedule
                showScheduleDialog = false
            }
        )
    }
}

@Composable
fun EnhancedHeader(
    currentStep: Int,
    totalSteps: Int,
    isEditing: Boolean,
    onBackClick: () -> Unit
) {
    Column {
        // Top bar with back button and progress
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Progress indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    ProgressDot(
                        isActive = index <= currentStep,
                        isCurrent = index == currentStep
                    )
                }
            }
        }

        // Animated progress bar
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Title section
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = when (currentStep) {
                    0 -> if (isEditing) "Update Theme" else "Choose Theme"
                    1 -> if (isEditing) "Edit Details" else "Basic Details"
                    2 -> if (isEditing) "Update Settings" else "Additional Info"
                    else -> "Module Setup"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = when (currentStep) {
                    0 -> "Pick a color that represents your module"
                    1 -> "Enter the essential module information"
                    2 -> "Add lecturer details and class schedules"
                    else -> "Set up your module"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ProgressDot(
    isActive: Boolean,
    isCurrent: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dot_scale"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape
            )
    )
}

@Composable
fun ThemeSelectionStep(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    moduleName: String,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Preview card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = try {
                    Color(android.graphics.Color.parseColor(selectedColor))
                } catch (e: Exception) {
                    Color(0xFF6366F1)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (moduleName.isBlank()) "Your Module" else moduleName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Color Themes",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        val moduleColors = listOf(
            "#FF6B9D" to "Romantic Pink",
            "#FFD93D" to "Sunny Yellow",
            "#6BCF7F" to "Fresh Green",
            "#4A90E2" to "Ocean Blue",
            "#6366F1" to "Royal Indigo",
            "#8B5CF6" to "Mystic Purple",
            "#EF4444" to "Cherry Red",
            "#F59E0B" to "Amber Glow",
            "#10B981" to "Emerald",
            "#06B6D4" to "Turquoise"
        )

        // Color grid instead of horizontal scroll
        val chunkedColors = moduleColors.chunked(2)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            chunkedColors.forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowColors.forEach { (color, name) ->
                        Box(modifier = Modifier.weight(1f)) {
                            ColorCard(
                                color = color,
                                name = name,
                                isSelected = selectedColor == color,
                                onSelect = { onColorSelected(color) }
                            )
                        }
                    }
                    // Fill remaining space if odd number
                    if (rowColors.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Next button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Continue",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ColorCard(
    color: String,
    name: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "color_card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                this@Row.AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = name,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BasicDetailsStep(
    moduleName: String,
    moduleCode: String,
    description: String,
    onModuleNameChange: (String) -> Unit,
    onModuleCodeChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNext: () -> Unit,
    canProceed: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        ModernTextField(
            value = moduleName,
            onValueChange = onModuleNameChange,
            label = "Module Name *",
            placeholder = "e.g., Advanced Mathematics",
            leadingIcon = Icons.Outlined.School
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModernTextField(
            value = moduleCode,
            onValueChange = onModuleCodeChange,
            label = "Module Code *",
            placeholder = "e.g., MATH2001",
            leadingIcon = Icons.Outlined.Tag
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModernTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "Description",
            placeholder = "Brief description of the module...",
            minLines = 3,
            maxLines = 4,
            leadingIcon = Icons.Outlined.Description
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Continue",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun AdditionalDetailsStep(
    lecturerName: String,
    credits: String,
    semester: String,
    schedules: List<ClassSchedule>,
    onLecturerNameChange: (String) -> Unit,
    onCreditsChange: (String) -> Unit,
    onSemesterChange: (String) -> Unit,
    onAddSchedule: () -> Unit,
    onRemoveSchedule: (ClassSchedule) -> Unit,
    onComplete: () -> Unit,
    isEditing: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        ModernTextField(
            value = lecturerName,
            onValueChange = onLecturerNameChange,
            label = "Lecturer",
            placeholder = "Dr. Smith",
            leadingIcon = Icons.Outlined.Person
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ModernTextField(
                    value = credits,
                    onValueChange = onCreditsChange,
                    label = "Credits",
                    placeholder = "5",
                    leadingIcon = Icons.Outlined.Numbers
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                ModernTextField(
                    value = semester,
                    onValueChange = onSemesterChange,
                    label = "Semester",
                    placeholder = "Fall 2024",
                    leadingIcon = Icons.Outlined.CalendarMonth
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Schedule section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Class Schedule",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedButton(
                onClick = onAddSchedule,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (schedules.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No schedules added yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                schedules.forEach { schedule ->
                    ScheduleItem(
                        schedule = schedule,
                        onRemove = { onRemoveSchedule(schedule) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isEditing) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Update Module" else "Create Module",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    minLines: Int = 1,
    maxLines: Int = 1,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        minLines = minLines,
        maxLines = maxLines
    )
}

@Composable
fun ScheduleItem(
    schedule: ClassSchedule,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(getDayName(schedule.dayOfWeek), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "${schedule.startTime} - ${schedule.endTime}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (schedule.location.isNotBlank()) {
                    Text(
                        schedule.location,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    schedule.type,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (ClassSchedule) -> Unit
) {
    var selectedDay by remember { mutableStateOf(1) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var location by remember { mutableStateOf("") }
    var classType by remember { mutableStateOf("LECTURE") }

    val classTypes = listOf("LECTURE", "LAB", "TUTORIAL", "SEMINAR")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Class Schedule",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Day of Week",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(7) { day ->
                        FilterChip(
                            selected = selectedDay == day + 1,
                            onClick = { selectedDay = day + 1 },
                            label = { Text(getDayName(day + 1).take(3)) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start") },
                        placeholder = { Text("09:00") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End") },
                        placeholder = { Text("10:00") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Class Type",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(classTypes) { type ->
                        FilterChip(
                            selected = classType == type,
                            onClick = { classType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("Room 101") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val schedule = ClassSchedule(
                        dayOfWeek = selectedDay,
                        startTime = startTime,
                        endTime = endTime,
                        location = location,
                        type = classType
                    )
                    onConfirm(schedule)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Schedule")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}


