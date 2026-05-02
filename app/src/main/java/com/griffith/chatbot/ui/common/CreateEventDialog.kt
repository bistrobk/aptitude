package com.griffith.chatbot.ui.common

import android.app.TimePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.griffith.chatbot.data.models.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    event: StudyEvent? = null,
    modules: List<Module>,
    onDismiss: () -> Unit,
    onConfirm: (StudyEvent) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var selectedModuleId by remember { mutableStateOf(event?.moduleId ?: "") }
    var selectedType by remember { mutableStateOf(event?.type ?: EventType.STUDY) }
    var selectedPriority by remember { mutableStateOf(event?.priority ?: Priority.MEDIUM) }

    var startDateTime by remember {
        mutableStateOf(
            event?.startTime?.let { longToLocalDateTime(it) } ?: LocalDateTime.now()
        )
    }
    var duration by remember { mutableStateOf(event?.duration?.toString() ?: "60") }
    var location by remember { mutableStateOf(event?.location ?: "") }

    val isEditing = event != null
    val context = LocalContext.current

    // --- State for Date and Time Pickers ---
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDateTimeToLong(startDateTime)
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Animation states
    val animationScope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        val selectedDate = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        startDateTime = startDateTime.with(selectedDate)
                        showDatePicker = false
                        showTimePicker = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    // --- Time Picker Logic ---
    if (showTimePicker) {
        showTimePicker = false

        val calendar = Calendar.getInstance()
        calendar.time = java.sql.Timestamp.valueOf(startDateTime.toString().replace("T", " "))

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val selectedTime = LocalTime.of(hourOfDay, minute)
                startDateTime = startDateTime.with(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                animationSpec = tween(300, easing = EaseOutBack)
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = scaleOut(
                animationSpec = tween(200)
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 50.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface) // Solid background instead of gradient
                ) {
                    // Enhanced Header with solid background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer) // Solid background
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isEditing) Icons.Outlined.Edit else Icons.Outlined.EventNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        if (isEditing) "Edit Event" else "Create New Event",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (isEditing) "Update your study session" else "Plan your study session",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    isVisible = false
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Scrollable content 
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Title Section
                        EnhancedInputSection(
                            title = "Event Details",
                            icon = Icons.Outlined.Title
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Event Title") },
                                placeholder = { Text("e.g., Calculus Study Session") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Outlined.Title, contentDescription = null) },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Event Type Section
                        EnhancedInputSection(
                            title = "Event Type",
                            icon = Icons.Outlined.Category
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(EventType.values()) { type ->
                                    EnhancedEventTypeChip(
                                        type = type,
                                        isSelected = selectedType == type,
                                        onSelect = { selectedType = type }
                                    )
                                }
                            }
                        }

                        // Module Selection
                        if (modules.isNotEmpty()) {
                            EnhancedInputSection(
                                title = "Module",
                                icon = Icons.Outlined.Folder
                            ) {
                                EnhancedModuleDropdown(
                                    modules = modules,
                                    selectedModuleId = selectedModuleId,
                                    onModuleSelected = { selectedModuleId = it }
                                )
                            }
                        }

                        // Schedule Section
                        EnhancedInputSection(
                            title = "Schedule",
                            icon = Icons.Outlined.Schedule
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Date and Time Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // DateTime Picker
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showDatePicker = true },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(16.dp)
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
                                            Column {
                                                Text(
                                                    "Date & Time",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    startDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    startDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    // Duration Input
                                    OutlinedTextField(
                                        value = duration,
                                        onValueChange = { duration = it.filter(Char::isDigit) },
                                        label = { Text("Duration") },
                                        suffix = { Text("min") },
                                        modifier = Modifier.width(120.dp),
                                        leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }

                        // Priority Section
                        EnhancedInputSection(
                            title = "Priority",
                            icon = Icons.Outlined.Flag
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(Priority.values()) { priority ->
                                    EnhancedPriorityChip(
                                        priority = priority,
                                        isSelected = selectedPriority == priority,
                                        onSelect = { selectedPriority = priority }
                                    )
                                }
                            }
                        }

                        // Location Section
                        EnhancedInputSection(
                            title = "Location",
                            icon = Icons.Outlined.LocationOn,
                            optional = true
                        ) {
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Location") },
                                placeholder = { Text("e.g., Library, Room 101") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Description Section
                        EnhancedInputSection(
                            title = "Notes",
                            icon = Icons.Outlined.Note,
                            optional = true
                        ) {
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Additional notes") },
                                placeholder = { Text("Any additional details...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // Action Buttons
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isVisible = false
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    width = 1.5.dp
                                )
                            ) {
                                Text(
                                    "Cancel",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Button(
                                onClick = {
                                    val selectedModule = modules.find { it.id == selectedModuleId }
                                    val durationLong = duration.toLongOrNull() ?: 60

                                    val newEvent = StudyEvent(
                                        id = event?.id ?: java.util.UUID.randomUUID().toString(),
                                        title = title,
                                        description = description,
                                        moduleId = selectedModuleId,
                                        moduleName = selectedModule?.name ?: "",
                                        startTime = localDateTimeToLong(startDateTime),
                                        endTime = localDateTimeToLong(startDateTime.plusMinutes(durationLong)),
                                        duration = durationLong.toInt(),
                                        type = selectedType,
                                        location = location,
                                        priority = selectedPriority,
                                        isFromTimetable = false,
                                        classScheduleId = null,
                                        instructor = ""
                                    )
                                    onConfirm(newEvent)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = title.isNotBlank(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(
                                    if (isEditing) Icons.Outlined.Update else Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isEditing) "Update Event" else "Create Event",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedInputSection(
    title: String,
    icon: ImageVector,
    optional: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (optional) {
                Text(
                    "(Optional)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        content()
    }
}

@Composable
fun EnhancedEventTypeChip(type: EventType, isSelected: Boolean, onSelect: () -> Unit) {
    val (icon, baseColor) = when (type) {
        EventType.STUDY -> Icons.Outlined.MenuBook to Color(0xFF2196F3)
        EventType.EXAM -> Icons.Outlined.Quiz to Color(0xFFF44336)
        EventType.ASSIGNMENT -> Icons.Outlined.Assignment to Color(0xFF9C27B0)
        EventType.REVISION -> Icons.Outlined.Refresh to Color(0xFF4CAF50)
        EventType.PROJECT -> Icons.Outlined.Work to Color(0xFFFF9800)
        EventType.RESEARCH -> Icons.Outlined.Search to Color(0xFF607D8B)
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .scale(animatedScale)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                baseColor // Solid color background when selected
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            null // No border when selected since we have solid background
        else
            CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EnhancedPriorityChip(priority: Priority, isSelected: Boolean, onSelect: () -> Unit) {
    val baseColor = when (priority) {
        Priority.LOW -> Color(0xFF4CAF50)
        Priority.MEDIUM -> Color(0xFF2196F3)
        Priority.HIGH -> Color(0xFFFF9800)
        Priority.URGENT -> Color(0xFFF44336)
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .scale(animatedScale)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                baseColor // Solid color background when selected
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            null // No border when selected since we have solid background
        else
            CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else baseColor)
            )
            Text(
                priority.name.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedModuleDropdown(
    modules: List<Module>,
    selectedModuleId: String,
    onModuleSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedModule = modules.find { it.id == selectedModuleId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedModule != null) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(selectedModule.color)))
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        selectedModule?.name ?: "Select Module",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedModule != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(16.dp)
            )
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("None (General Event)")
                    }
                },
                onClick = {
                    onModuleSelected("")
                    expanded = false
                }
            )
            modules.forEach { module ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(module.color)))
                            )
                            Text(module.name)
                        }
                    },
                    onClick = {
                        onModuleSelected(module.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun longToLocalDateTime(timeInMillis: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis), ZoneId.systemDefault())

private fun localDateTimeToLong(dateTime: LocalDateTime): Long =
    dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()