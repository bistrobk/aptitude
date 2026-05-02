package com.griffith.chatbot.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.griffith.chatbot.data.models.Lecture
import com.griffith.chatbot.data.repository.DatabaseManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Helper function to determine file type
fun getFileTypeFromName(fileName: String): String {
    return when {
        fileName.endsWith(".pdf", ignoreCase = true) -> "pdf"
        fileName.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp|webp)$", RegexOption.IGNORE_CASE)) -> "image"
        fileName.matches(Regex(".*\\.(doc|docx)$", RegexOption.IGNORE_CASE)) -> "document"
        fileName.matches(Regex(".*\\.(ppt|pptx)$", RegexOption.IGNORE_CASE)) -> "presentation"
        fileName.matches(Regex(".*\\.(xls|xlsx)$", RegexOption.IGNORE_CASE)) -> "spreadsheet"
        fileName.matches(Regex(".*\\.(txt|rtf)$", RegexOption.IGNORE_CASE)) -> "text"
        fileName.matches(Regex(".*\\.(mp4|avi|mov|wmv|flv|mkv)$", RegexOption.IGNORE_CASE)) -> "video"
        fileName.matches(Regex(".*\\.(mp3|wav|aac|flac|ogg)$", RegexOption.IGNORE_CASE)) -> "audio"
        fileName.matches(Regex(".*\\.(zip|rar|7z|tar|gz)$", RegexOption.IGNORE_CASE)) -> "archive"
        else -> "file"
    }
}

// Enhanced lecture types with better visual design
enum class LectureType(
    val displayName: String,
    val color: String,
    val icon: ImageVector,
    val description: String
) {
    LECTURE("Lecture", "#6366F1", Icons.Outlined.School, "Traditional classroom lecture"),
    LAB("Lab", "#10B981", Icons.Outlined.Science, "Hands-on laboratory session"),
    TUTORIAL("Tutorial", "#F59E0B", Icons.Outlined.Quiz, "Interactive tutorial or workshop"),
    SEMINAR("Seminar", "#EF4444", Icons.Outlined.Groups, "Discussion-based seminar")
}

// Class schedule data
data class ClassTimetable(
    val dayOfWeek: Int = 1, // 1-7 (Monday-Sunday)
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val location: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun LectureCreationDialog(
    moduleId: String,
    lecture: Lecture? = null,
    onDismiss: () -> Unit,
    onConfirm: (Lecture) -> Unit,
    onFileUpload: ((String, Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val databaseManager = DatabaseManager.getInstance()

    // State management
    var currentStep by remember { mutableStateOf(0) }
    var lectureTitle by remember { mutableStateOf(lecture?.title ?: "") }
    var selectedType by remember {
        mutableStateOf(
            LectureType.entries.find { it.name == lecture?.lectureType } ?: LectureType.LECTURE
        )
    }
    var location by remember { mutableStateOf(lecture?.location ?: "") }
    var timetable by remember {
        mutableStateOf(
            if (lecture?.timetable != null) {
                ClassTimetable(
                    dayOfWeek = (lecture.timetable["dayOfWeek"] as? Long)?.toInt() ?: 1,
                    startTime = lecture.timetable["startTime"] as? String ?: "09:00",
                    endTime = lecture.timetable["endTime"] as? String ?: "10:00",
                    location = lecture.timetable["location"] as? String ?: ""
                )
            } else {
                ClassTimetable()
            }
        )
    }
    var tags by remember { mutableStateOf(lecture?.tags?.joinToString(", ") ?: "") }
    var pendingFiles by remember { mutableStateOf(listOf<Pair<String, Uri>>()) }
    var showTimetableDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    // MULTIPLE SAFEGUARDS TO PREVENT DUPLICATE CREATION
    var isCreating by remember { mutableStateOf(false) }
    var creationJob by remember { mutableStateOf<Job?>(null) }
    var lastCreationTime by remember { mutableStateOf(0L) }

    val isEditing = lecture != null

    // Cleanup job on dismiss
    DisposableEffect(Unit) {
        onDispose {
            creationJob?.cancel()
        }
    }

    // Reset creation flag after timeout (safety measure)
    LaunchedEffect(isCreating) {
        if (isCreating) {
            delay(30000) // 30 seconds timeout
            if (isCreating) {
                isCreating = false
                isUploading = false
                println("DatabaseManager: Creation timeout - resetting flags")
            }
        }
    }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(selectedUri)

            val fileName = when {
                mimeType?.startsWith("application/pdf") == true ->
                    "lecture_notes_${System.currentTimeMillis()}.pdf"
                mimeType?.startsWith("image/") == true -> {
                    val extension = when (mimeType) {
                        "image/jpeg" -> "jpg"
                        "image/png" -> "png"
                        "image/gif" -> "gif"
                        "image/bmp" -> "bmp"
                        "image/webp" -> "webp"
                        else -> "jpg"
                    }
                    "lecture_notes_${System.currentTimeMillis()}.$extension"
                }
                mimeType?.startsWith("application/vnd.openxmlformats-officedocument") == true ||
                        mimeType?.startsWith("application/msword") == true ->
                    "lecture_document_${System.currentTimeMillis()}.docx"
                mimeType?.startsWith("application/vnd.ms-powerpoint") == true ||
                        mimeType?.startsWith("application/vnd.openxmlformats-officedocument.presentationml") == true ->
                    "lecture_presentation_${System.currentTimeMillis()}.pptx"
                mimeType?.startsWith("text/") == true ->
                    "lecture_text_${System.currentTimeMillis()}.txt"
                else -> "lecture_file_${System.currentTimeMillis()}"
            }

            pendingFiles = pendingFiles + (fileName to selectedUri)
        }
    }

    Dialog(
        onDismissRequest = {
            // Cancel any ongoing creation when dialog is dismissed
            creationJob?.cancel()
            onDismiss()
        },
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Enhanced Header
                LectureHeader(
                    currentStep = currentStep,
                    totalSteps = 3,
                    isEditing = isEditing,
                    onBackClick = {
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            creationJob?.cancel()
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
                    label = "lecture_step_transition"
                ) { step ->
                    when (step) {
                        0 -> LectureBasicDetailsStep(
                            lectureTitle = lectureTitle,
                            selectedType = selectedType,
                            location = location,
                            onTitleChange = { lectureTitle = it },
                            onTypeChange = { selectedType = it },
                            onLocationChange = { location = it },
                            onNext = {
                                if (lectureTitle.isNotBlank()) {
                                    currentStep = 1
                                }
                            },
                            canProceed = lectureTitle.isNotBlank(),
                            isUploading = isUploading
                        )
                        1 -> LectureScheduleStep(
                            timetable = timetable,
                            tags = tags,
                            onTimetableChange = { timetable = it },
                            onTagsChange = { tags = it },
                            onNext = { currentStep = 2 },
                            isUploading = isUploading
                        )
                        2 -> LectureFilesStep(
                            pendingFiles = pendingFiles,
                            onFilePick = { filePicker.launch("*/*") },
                            onFileRemove = { fileName ->
                                pendingFiles = pendingFiles.filter { it.first != fileName }
                            },
                            onComplete = {
                                // ROBUST DUPLICATE PREVENTION
                                val currentTime = System.currentTimeMillis()

                                // Check multiple conditions to prevent duplicates
                                if (!isCreating &&
                                    !isUploading &&
                                    creationJob?.isActive != true &&
                                    (currentTime - lastCreationTime) > 2000 // 2 second cooldown
                                ) {
                                    lastCreationTime = currentTime
                                    isCreating = true

                                    println("DatabaseManager: Starting lecture creation at $currentTime")

                                    creationJob = scope.launch {
                                        try {
                                            isUploading = true

                                            val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                            val newLecture = Lecture(
                                                id = lecture?.id ?: "",
                                                moduleId = moduleId,
                                                title = lectureTitle,
                                                description = "",
                                                tags = tagsList,
                                                lectureType = selectedType.name,
                                                location = location,
                                                timetable = mapOf(
                                                    "dayOfWeek" to timetable.dayOfWeek,
                                                    "startTime" to timetable.startTime,
                                                    "endTime" to timetable.endTime,
                                                    "location" to timetable.location
                                                ),
                                                createdAt = lecture?.createdAt ?: System.currentTimeMillis(),
                                                lastModified = System.currentTimeMillis()
                                            )

                                            println("DatabaseManager: Creating lecture with title: ${newLecture.title}")

                                            val lectureResult = if (isEditing) {
                                                databaseManager.updateLecture(moduleId, newLecture)
                                                Result.success(newLecture.id)
                                            } else {
                                                databaseManager.createLecture(moduleId, newLecture)
                                            }

                                            lectureResult.onSuccess { lectureId ->
                                                println("DatabaseManager: Lecture creation successful with ID: $lectureId")

                                                // Upload files
                                                pendingFiles.forEach { (fileName, uri) ->
                                                    try {
                                                        databaseManager.uploadLectureFile(
                                                            lectureId = lectureId,
                                                            fileUri = uri,
                                                            fileName = fileName,
                                                            context = context

                                                        )
                                                    } catch (e: Exception) {
                                                        println("DatabaseManager: File upload error: ${e.message}")
                                                    }
                                                }

                                                val finalLecture = newLecture.copy(id = lectureId)

                                                // Small delay to ensure database consistency
                                                delay(100)

                                                onConfirm(finalLecture)

                                            }.onFailure { error ->
                                                println("DatabaseManager: Lecture creation failed: ${error.message}")
                                                isUploading = false
                                                isCreating = false
                                            }
                                        } catch (e: Exception) {
                                            println("DatabaseManager: Creation exception: ${e.message}")
                                            isUploading = false
                                            isCreating = false
                                        }
                                    }
                                } else {
                                    println("DatabaseManager: Creation blocked - already in progress or cooldown active")
                                    println("DatabaseManager: isCreating=$isCreating, isUploading=$isUploading, jobActive=${creationJob?.isActive}, timeSince=${currentTime - lastCreationTime}ms")
                                }
                            },
                            isEditing = isEditing,
                            isUploading = isUploading,
                            isCreating = isCreating
                        )
                    }
                }
            }
        }
    }

    if (showTimetableDialog) {
        TimetableDialog(
            timetable = timetable,
            onDismiss = { showTimetableDialog = false },
            onConfirm = { newTimetable ->
                timetable = newTimetable
                showTimetableDialog = false
            }
        )
    }
}

@Composable
fun LectureHeader(
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
                    LectureProgressDot(
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
                    0 -> if (isEditing) "Edit Lecture" else "Create Lecture"
                    1 -> "Schedule & Tags"
                    2 -> "Upload Materials"
                    else -> "Lecture Setup"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = when (currentStep) {
                    0 -> "Enter basic lecture information"
                    1 -> "Set timing and add relevant tags"
                    2 -> "Upload notes and learning materials"
                    else -> "Set up your lecture"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun LectureProgressDot(
    isActive: Boolean,
    isCurrent: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "lecture_dot_scale"
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
fun LectureBasicDetailsStep(
    lectureTitle: String,
    selectedType: LectureType,
    location: String,
    onTitleChange: (String) -> Unit,
    onTypeChange: (LectureType) -> Unit,
    onLocationChange: (String) -> Unit,
    onNext: () -> Unit,
    canProceed: Boolean,
    isUploading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Lecture Title
        LectureTextField(
            value = lectureTitle,
            onValueChange = onTitleChange,
            label = "Lecture Title *",
            placeholder = "e.g., Introduction to Calculus",
            leadingIcon = Icons.Outlined.School,
            enabled = !isUploading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Lecture Type Section
        Text(
            text = "Lecture Type",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Choose the type that best describes this session",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Type cards in grid
        val chunkedTypes = LectureType.values().toList().chunked(2)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            chunkedTypes.forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowTypes.forEach { type ->
                        Box(modifier = Modifier.weight(1f)) {
                            LectureTypeCard(
                                type = type,
                                isSelected = selectedType == type,
                                onSelect = { if (!isUploading) onTypeChange(type) },
                                enabled = !isUploading
                            )
                        }
                    }
                    // Fill remaining space if odd number
                    if (rowTypes.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Location
        LectureTextField(
            value = location,
            onValueChange = onLocationChange,
            label = "Location",
            placeholder = "e.g., Room 101, Lab A",
            leadingIcon = Icons.Outlined.LocationOn,
            enabled = !isUploading
        )

        Spacer(modifier = Modifier.weight(1f))

        // Continue button
        Button(
            onClick = onNext,
            enabled = canProceed && !isUploading,
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

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun LectureTypeCard(
    type: LectureType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "type_card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(enabled = enabled) { onSelect() },
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(type.color)).copy(alpha = 0.2f)
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    type.icon,
                    contentDescription = null,
                    tint = try {
                        Color(android.graphics.Color.parseColor(type.color))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = type.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = type.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LectureScheduleStep(
    timetable: ClassTimetable,
    tags: String,
    onTimetableChange: (ClassTimetable) -> Unit,
    onTagsChange: (String) -> Unit,
    onNext: () -> Unit,
    isUploading: Boolean
) {
    var showTimetableDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Schedule Section
        Text(
            text = "Class Schedule",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = getDayName(timetable.dayOfWeek),
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${timetable.startTime} - ${timetable.endTime}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (timetable.location.isNotEmpty()) {
                            Text(
                                text = timetable.location,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showTimetableDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUploading
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tags
        LectureTextField(
            value = tags,
            onValueChange = onTagsChange,
            label = "Tags",
            placeholder = "calculus, derivatives, limits",
            leadingIcon = Icons.Outlined.Tag,
            supportingText = "Separate tags with commas",
            enabled = !isUploading
        )

        Spacer(modifier = Modifier.weight(1f))

        // Continue button
        Button(
            onClick = onNext,
            enabled = !isUploading,
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

    if (showTimetableDialog) {
        TimetableDialog(
            timetable = timetable,
            onDismiss = { showTimetableDialog = false },
            onConfirm = { newTimetable ->
                onTimetableChange(newTimetable)
                showTimetableDialog = false
            }
        )
    }
}

@Composable
fun LectureFilesStep(
    pendingFiles: List<Pair<String, Uri>>,
    onFilePick: () -> Unit,
    onFileRemove: (String) -> Unit,
    onComplete: () -> Unit,
    isEditing: Boolean,
    isUploading: Boolean,
    isCreating: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Upload section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lecture Materials",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedButton(
                onClick = onFilePick,
                shape = RoundedCornerShape(12.dp),
                enabled = !isUploading && !isCreating
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Files")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (pendingFiles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No files uploaded yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add PDFs, images, documents, or any lecture materials",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pendingFiles.forEach { (fileName, uri) ->
                    LectureNotesItem(
                        fileName = fileName,
                        fileType = getFileTypeFromName(fileName),
                        onRemove = { onFileRemove(fileName) },
                        enabled = !isUploading && !isCreating
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Loading indicator when uploading
        if (isUploading || isCreating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when {
                            isCreating && isUploading -> "Creating lecture and uploading files..."
                            isCreating -> "Creating lecture..."
                            isUploading -> "Uploading files..."
                            else -> "Processing..."
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Complete button with enhanced protection
        Button(
            onClick = {
                // Additional click protection - ignore rapid clicks
                println("DatabaseManager: Create button clicked")
                onComplete()
            },
            enabled = !isUploading && !isCreating,
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
                if (isCreating || isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        if (isEditing) Icons.Outlined.Check else Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isCreating -> "Creating..."
                        isUploading -> "Uploading..."
                        isEditing -> "Update Lecture"
                        else -> "Create Lecture"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    supportingText: String? = null,
    enabled: Boolean = true
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
        supportingText = supportingText?.let { text ->
            {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableDialog(
    timetable: ClassTimetable,
    onDismiss: () -> Unit,
    onConfirm: (ClassTimetable) -> Unit
) {
    var selectedDay by remember { mutableStateOf(timetable.dayOfWeek) }
    var startTime by remember { mutableStateOf(timetable.startTime) }
    var endTime by remember { mutableStateOf(timetable.endTime) }
    var location by remember { mutableStateOf(timetable.location) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Set Class Schedule",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    "Day of Week",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(7) { day ->
                        FilterChip(
                            selected = selectedDay == day + 1,
                            onClick = { selectedDay = day + 1 },
                            label = {
                                Text(
                                    getDayName(day + 1).take(3),
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Time",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (Optional)") },
                    placeholder = { Text("Room 101") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        ClassTimetable(
                            dayOfWeek = selectedDay,
                            startTime = startTime,
                            endTime = endTime,
                            location = location
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Set Schedule")
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

@Composable
fun LectureNotesItem(
    fileName: String,
    fileType: String,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon with background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when (fileType.lowercase()) {
                            "pdf" -> Color(0xFFFFEBEE)
                            "image" -> Color(0xFFE3F2FD)
                            "document" -> Color(0xFFE3F2FD)
                            "presentation" -> Color(0xFFFFF3E0)
                            "spreadsheet" -> Color(0xFFE8F5E8)
                            "text" -> Color(0xFFECEFF1)
                            "video" -> Color(0xFFF3E5F5)
                            "audio" -> Color(0xFFFCE4EC)
                            "archive" -> Color(0xFFEFEBE9)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (fileType.lowercase()) {
                        "pdf" -> Icons.Outlined.PictureAsPdf
                        "image" -> Icons.Outlined.Image
                        "document" -> Icons.Outlined.Description
                        "presentation" -> Icons.Outlined.Slideshow
                        "spreadsheet" -> Icons.Outlined.TableChart
                        "text" -> Icons.Outlined.TextSnippet
                        "video" -> Icons.Outlined.VideoFile
                        "audio" -> Icons.Outlined.AudioFile
                        "archive" -> Icons.Outlined.Archive
                        else -> Icons.Outlined.AttachFile
                    },
                    contentDescription = null,
                    tint = when (fileType.lowercase()) {
                        "pdf" -> Color(0xFFD32F2F)
                        "image" -> Color(0xFF1976D2)
                        "document" -> Color(0xFF1976D2)
                        "presentation" -> Color(0xFFFF9800)
                        "spreadsheet" -> Color(0xFF4CAF50)
                        "text" -> Color(0xFF607D8B)
                        "video" -> Color(0xFF9C27B0)
                        "audio" -> Color(0xFFE91E63)
                        "archive" -> Color(0xFF795548)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${fileType.uppercase()} File",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // File type badge
            Surface(
                color = when (fileType.lowercase()) {
                    "pdf" -> Color(0xFFFFEBEE)
                    "image" -> Color(0xFFE3F2FD)
                    "document" -> Color(0xFFE3F2FD)
                    "presentation" -> Color(0xFFFFF3E0)
                    "spreadsheet" -> Color(0xFFE8F5E8)
                    "text" -> Color(0xFFECEFF1)
                    "video" -> Color(0xFFF3E5F5)
                    "audio" -> Color(0xFFFCE4EC)
                    "archive" -> Color(0xFFEFEBE9)
                    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    fileType.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (fileType.lowercase()) {
                        "pdf" -> Color(0xFFD32F2F)
                        "image" -> Color(0xFF1976D2)
                        "document" -> Color(0xFF1976D2)
                        "presentation" -> Color(0xFFFF9800)
                        "spreadsheet" -> Color(0xFF4CAF50)
                        "text" -> Color(0xFF607D8B)
                        "video" -> Color(0xFF9C27B0)
                        "audio" -> Color(0xFFE91E63)
                        "archive" -> Color(0xFF795548)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}

// Compatibility components
@Composable
fun LectureNotesItem(
    fileName: String,
    fileType: String,
    onRemove: () -> Unit
) {
    LectureNotesItem(
        fileName = fileName,
        fileType = fileType,
        onRemove = onRemove
    )
}



fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Unknown"
    }
}