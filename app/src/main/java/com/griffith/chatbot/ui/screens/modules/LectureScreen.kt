package com.griffith.chatbot.ui.screens.modules

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.griffith.chatbot.data.models.Lecture
import com.griffith.chatbot.ui.common.LectureCreationDialog
import com.griffith.chatbot.ui.commons.ChatTabWithInput
import com.griffith.chatbot.ui.commons.LectureFilesTab
import com.griffith.chatbot.ui.commons.SummaryTab
import com.griffith.chatbot.viewModel.ChatViewModel
import com.griffith.chatbot.viewModel.LectureViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun LectureScreen(
    moduleId: String,
    moduleName: String = "Module",
    navController: NavController,
    lectureViewModel: LectureViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) } // Grid or List view

    // State for dialogs
    var showQuizDialog by remember { mutableStateOf(false) }
    var showFlashcardsDialog by remember { mutableStateOf(false) }
    var quizContent by remember { mutableStateOf("") }
    var flashcardsContent by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            lectureViewModel.selectedLecture?.let { lecture ->
                chatViewModel.uploadLectureFile(
                    moduleId = moduleId,
                    lectureId = lecture.id,
                    uri = uri,
                    context = context
                ) { success, message ->
                    if (success) {
                        println("✅ File uploaded successfully: $message")
                    } else {
                        println("❌ Upload failed: $message")
                    }
                }
            }
        }
    }

    // Load lectures when screen opens
    LaunchedEffect(moduleId) {
        lectureViewModel.loadLectures(moduleId)
    }

    // Load chat when lecture is selected
    LaunchedEffect(lectureViewModel.selectedLecture, lectureViewModel.showChat) {
        if (lectureViewModel.showChat && lectureViewModel.selectedLecture != null) {
            chatViewModel.loadLectureChat(
                moduleId = moduleId,
                lectureId = lectureViewModel.selectedLecture!!.id
            )
        }
    }

    Scaffold(
        topBar = {
            EnhancedTopBar(
                title = if (lectureViewModel.showChat) {
                    lectureViewModel.selectedLecture?.title ?: "Lecture"
                } else {
                    "$moduleName - Lectures"
                },
                showChat = lectureViewModel.showChat,
                showViewToggle = !lectureViewModel.showChat && lectureViewModel.lectures.isNotEmpty(),
                viewMode = viewMode,
                lectureCount = lectureViewModel.lectures.size,
                onBackClick = {
                    if (lectureViewModel.showChat) {
                        lectureViewModel.backToList()
                        selectedTab = 0
                    } else {
                        navController.popBackStack()
                    }
                },
                onViewModeToggle = { viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID }
            )
        },
        floatingActionButton = {
            if (!lectureViewModel.showChat) {
                AnimatedFloatingActionButton(
                    onClick = { lectureViewModel.showCreateLectureDialog() }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!lectureViewModel.showChat) {
                // Enhanced Lecture List View
                EnhancedLectureListContent(
                    lectureViewModel = lectureViewModel,
                    viewMode = viewMode,
                    onLectureClick = { lecture ->
                        lectureViewModel.selectLecture(lecture)
                    },
                    onDeleteLecture = { lecture ->
                        lectureViewModel.deleteLecture(moduleId, lecture.id)
                    },
                    onEditLecture = { lecture ->
                        lectureViewModel.editLecture(lecture)
                    }
                )
            } else {
                // Enhanced Chat View with Tabs
                Column {
                    EnhancedTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )

                    // Tab Content with animations
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { if (targetState > initialState) 300 else -300 }
                            ) + fadeIn() with slideOutHorizontally(
                                targetOffsetX = { if (targetState > initialState) -300 else 300 }
                            ) + fadeOut()
                        },
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            0 -> SummaryTab(
                                summary = chatViewModel.summaryText,
                                keywords = chatViewModel.keywordsText,
                                onGenerateQuiz = {
                                    scope.launch {
                                        chatViewModel.generateQuiz { quiz ->
                                            quizContent = quiz
                                            showQuizDialog = true
                                        }
                                    }
                                },
                                onCreateFlashcards = {
                                    scope.launch {
                                        chatViewModel.generateFlashcards { flashcards ->
                                            flashcardsContent = flashcards
                                            showFlashcardsDialog = true
                                        }
                                    }
                                }
                            )
                            1 -> ChatTabWithInput(
                                viewModel = chatViewModel,
                                filePicker = filePicker,
                                onFileUploaded = { summary, keywords ->
                                    // File processing handled by ViewModel
                                }
                            )
                            2 -> LectureFilesTab(
                                lectureId = lectureViewModel.selectedLecture?.id ?: "",
                                moduleId = moduleId,
                                chatViewModel = chatViewModel,
                                onFileUpload = { filePicker.launch("*/*") }
                            )
                        }
                    }
                }
            }
        }

        // Enhanced Dialogs
        if (showQuizDialog) {
            EnhancedQuizDialog(
                quizContent = quizContent,
                onDismiss = {
                    showQuizDialog = false
                    quizContent = ""
                }
            )
        }

        if (showFlashcardsDialog) {
            EnhancedFlashcardsDialog(
                flashcardsContent = flashcardsContent,
                onDismiss = {
                    showFlashcardsDialog = false
                    flashcardsContent = ""
                }
            )
        }

        // Create/Edit Lecture Dialog
        if (lectureViewModel.showCreateDialog) {
            LectureCreationDialog(
                moduleId = moduleId,
                lecture = lectureViewModel.editingLecture,
                onDismiss = lectureViewModel::hideCreateLectureDialog,
                onConfirm = { lecture ->
                    if (lectureViewModel.editingLecture != null) {
                        lectureViewModel.updateLecture(moduleId, lecture)
                    } else {
                        lectureViewModel.createLecture(moduleId, lecture)
                    }
                }
            )
        }

        // Error/Success handling
        if (lectureViewModel.errorMessage.isNotEmpty()) {
            LaunchedEffect(lectureViewModel.errorMessage) {
                lectureViewModel.clearError()
            }
        }
    }
}

enum class ViewMode { GRID, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTopBar(
    title: String,
    showChat: Boolean,
    showViewToggle: Boolean,
    viewMode: ViewMode,
    lectureCount: Int,
    onBackClick: () -> Unit,
    onViewModeToggle: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!showChat && lectureCount > 0) {
                    Text(
                        text = "$lectureCount lecture${if (lectureCount != 1) "s" else ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            if (showViewToggle) {
                IconButton(
                    onClick = onViewModeToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle view",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

@Composable
fun EnhancedTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "Summary" to Icons.Outlined.Summarize,
        "Chat" to Icons.Outlined.Chat,
        "Files" to Icons.Outlined.Folder
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(4.dp)
                            .padding(horizontal = 16.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selectedTab == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedFloatingActionButton(
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_scale"
    )

    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Add Lecture",
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedLectureListContent(
    lectureViewModel: LectureViewModel,
    viewMode: ViewMode,
    onLectureClick: (Lecture) -> Unit,
    onDeleteLecture: (Lecture) -> Unit,
    onEditLecture: (Lecture) -> Unit
) {
    when {
        lectureViewModel.isLoading -> {
            EnhancedLoadingState()
        }
        lectureViewModel.lectures.isEmpty() -> {
            EnhancedEmptyLecturesState()
        }
        else -> {
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                },
                label = "view_mode_transition"
            ) { mode ->
                when (mode) {
                    ViewMode.GRID -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp
                        ) {
                            items(lectureViewModel.lectures) { lecture ->
                                EnhancedLectureGridCard(
                                    lecture = lecture,
                                    onClick = { onLectureClick(lecture) },
                                    onDelete = { onDeleteLecture(lecture) },
                                    onEdit = { onEditLecture(lecture) }
                                )
                            }
                        }
                    }
                    ViewMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(lectureViewModel.lectures) { lecture ->
                                EnhancedLectureListCard(
                                    lecture = lecture,
                                    onClick = { onLectureClick(lecture) },
                                    onDelete = { onDeleteLecture(lecture) },
                                    onEdit = { onEditLecture(lecture) }
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
fun EnhancedLectureGridCard(
    lecture: Lecture,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (showMenu) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    getLectureTypeIcon(lecture.lectureType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Text(
                text = lecture.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (lecture.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lecture.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags
            if (lecture.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    lecture.tags.take(2).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (lecture.tags.size > 2) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "+${lecture.tags.size - 2}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lecture.lectureType,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = formatDate(lecture.createdAt),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EnhancedLectureListCard(
    lecture: Lecture,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    getLectureTypeIcon(lecture.lectureType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = lecture.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (lecture.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lecture.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = lecture.lectureType,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "• ${formatDate(lecture.createdAt)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading lectures...",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EnhancedEmptyLecturesState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(120.dp)
            ) {
                Icon(
                    Icons.Outlined.MenuBook,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(30.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "No Lectures Yet",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add your first lecture to start learning\nand organize your study materials",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun EnhancedQuizDialog(
    quizContent: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Quiz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Generated Quiz",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn {
                item {
                    Text(
                        text = quizContent.ifEmpty { "Generating quiz..." },
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun EnhancedFlashcardsDialog(
    flashcardsContent: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Generated Flashcards",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn {
                item {
                    Text(
                        text = flashcardsContent.ifEmpty { "Generating flashcards..." },
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Helper functions
private fun getLectureTypeIcon(lectureType: String): ImageVector {
    return when (lectureType.uppercase()) {
        "LECTURE" -> Icons.Outlined.School
        "LAB" -> Icons.Outlined.Science
        "TUTORIAL" -> Icons.Outlined.Quiz
        "SEMINAR" -> Icons.Outlined.Groups
        "WORKSHOP" -> Icons.Outlined.Build
        "EXAM" -> Icons.Outlined.Assignment
        "ASSIGNMENT" -> Icons.Outlined.Task
        else -> Icons.Outlined.MenuBook
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}