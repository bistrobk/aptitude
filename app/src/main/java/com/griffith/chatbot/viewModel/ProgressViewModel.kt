package com.griffith.chatbot.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.griffith.chatbot.data.models.*
import com.griffith.chatbot.data.repository.DatabaseManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.time.*

// Enhanced UI State with new fields
data class ProgressUiState(
    val studyStats: StudyStats = StudyStats(),
    val allEvents: List<StudyEvent> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val showAddEventDialog: Boolean = false,
    val errorMessage: String? = null,
    val modules: List<Module> = emptyList(),
    val recentSessions: List<StudySession> = emptyList(),
    // NEW: Enhanced fields
    val showTimetableView: Boolean = true, // Toggle for showing class schedules
    val nextClass: StudyEvent? = null, // Next upcoming class
    val todayEvents: List<StudyEvent> = emptyList() // Today's events
)

class ProgressViewModel : ViewModel() {

    private val dbManager = DatabaseManager.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        loadRecentSessions()
    }

    // Converts a Long timestamp to LocalDateTime
    private fun longToLocalDateTime(timestamp: Long): LocalDateTime {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    // Converts a LocalDateTime to Long timestamp
    fun localDateTimeToLong(dateTime: LocalDateTime): Long {
        return dateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    // NEW: Helper function to convert ClassSchedule to StudyEvent
    private fun ClassSchedule.toStudyEvent(module: Module, date: LocalDate): StudyEvent {
        val startDateTime = LocalDateTime.of(date, LocalTime.parse(this.startTime))
        val endDateTime = LocalDateTime.of(date, LocalTime.parse(this.endTime))

        return StudyEvent(
            id = "${module.id}_${this.hashCode()}_${date}",
            title = "${module.name} (${this.type})",
            description = this.location,
            moduleId = module.id,
            moduleName = module.name,
            startTime = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            endTime = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            duration = java.time.Duration.between(startDateTime, endDateTime).toMinutes().toInt(),
            type = when (this.type) {
                "LECTURE" -> EventType.STUDY
                "LAB" -> EventType.ASSIGNMENT
                "TUTORIAL" -> EventType.REVISION
                "SEMINAR" -> EventType.STUDY
                else -> EventType.STUDY
            },
            location = this.location,
            isFromTimetable = true,
            classScheduleId = this.hashCode().toString(),
            instructor = module.lecturerName
        )
    }

    // NEW: Generate timetable events from Module.schedule
    private fun generateTimetableEventsForMonth(
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
                        events.add(classSchedule.toStudyEvent(module, currentDate))
                    }
                    currentDate = currentDate.plusDays(1)
                }
            }
        }

        return events.sortedBy { it.startTime }
    }

    // NEW: Get events for a specific date including timetable
    fun getEventsForDate(date: LocalDate): List<StudyEvent> {
        val currentState = _uiState.value
        val userEvents = currentState.allEvents.filter {
            longToLocalDateTime(it.startTime).toLocalDate() == date
        }

        val timetableEvents = if (currentState.showTimetableView) {
            generateTimetableEventsForDate(date, currentState.modules)
        } else emptyList()

        return (userEvents + timetableEvents).sortedBy { it.startTime }
    }

    private fun generateTimetableEventsForDate(
        date: LocalDate,
        modules: List<Module>
    ): List<StudyEvent> {
        return modules.flatMap { module ->
            module.schedule.filter { classSchedule ->
                classSchedule.dayOfWeek == date.dayOfWeek.value
            }.map { classSchedule ->
                classSchedule.toStudyEvent(module, date)
            }
        }
    }

    /** Enhanced load function with parallel loading and improved stats */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load data in parallel for better performance
                val eventsDeferred = async { dbManager.getStudyEvents() }
                val modulesDeferred = async { dbManager.getModules() }

                val eventsResult = eventsDeferred.await()
                val modulesResult = modulesDeferred.await()

                if (eventsResult.isSuccess && modulesResult.isSuccess) {
                    val events = eventsResult.getOrDefault(emptyList())
                    val modules = modulesResult.getOrDefault(emptyList())

                    // Enhanced stats calculation
                    val stats = calculateEnhancedStats(events, modules)

                    // Generate today's events including timetable
                    val today = LocalDate.now()
                    val todayEvents = getEventsForDateInternal(today, events, modules)

                    // Find next class
                    val now = LocalDateTime.now()
                    val allEventsWithTimetable = events + generateTimetableEventsForMonth(today, modules)
                    val nextClass = allEventsWithTimetable
                        .filter { it.isFromTimetable && longToLocalDateTime(it.startTime).isAfter(now) }
                        .minByOrNull { it.startTime }

                    _uiState.update {
                        it.copy(
                            allEvents = events,
                            modules = modules,
                            studyStats = stats,
                            todayEvents = todayEvents,
                            nextClass = nextClass,
                            isLoading = false
                        )
                    }
                } else {
                    val error = eventsResult.exceptionOrNull()?.message
                        ?: modulesResult.exceptionOrNull()?.message
                        ?: "Unknown error loading data."

                    _uiState.update {
                        it.copy(errorMessage = error, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to load data: ${e.message}", isLoading = false)
                }
            }
        }
    }

    private fun getEventsForDateInternal(
        date: LocalDate,
        events: List<StudyEvent>,
        modules: List<Module>
    ): List<StudyEvent> {
        val userEvents = events.filter {
            longToLocalDateTime(it.startTime).toLocalDate() == date
        }

        val timetableEvents = if (_uiState.value.showTimetableView) {
            generateTimetableEventsForDate(date, modules)
        } else emptyList()

        return (userEvents + timetableEvents).sortedBy { it.startTime }
    }

    // NEW: Enhanced stats calculation
    private fun calculateEnhancedStats(events: List<StudyEvent>, modules: List<Module>): StudyStats {
        val totalHours = events.sumOf { it.duration } / 60.0
        val weekStart = LocalDate.now().minusDays(7)
        val weeklyHours = events.filter { event ->
            val eventDate = longToLocalDateTime(event.startTime).toLocalDate()
            eventDate.isAfter(weekStart)
        }.sumOf { it.duration } / 60.0

        // Calculate streak (simplified - you can enhance this)
        val streakDays = calculateStudyStreak(events)

        return StudyStats(
            totalHours = totalHours,
            streakDays = streakDays,
            completedModules = modules.count { it.completedLectures >= it.totalLectures && it.totalLectures > 0 },
            totalModules = modules.size,
            weeklyActualHours = weeklyHours,
            averageSessionDuration = if (events.isNotEmpty()) events.sumOf { it.duration }.toDouble() / events.size else 0.0,
            totalSessions = events.size,
            lastStudyDate = events.maxOfOrNull { it.startTime } ?: 0L
        )
    }

    private fun calculateStudyStreak(events: List<StudyEvent>): Int {
        val today = LocalDate.now()
        val eventDates = events.map {
            longToLocalDateTime(it.startTime).toLocalDate()
        }.distinct().sortedDescending()

        var streak = 0
        var currentDate = today

        for (date in eventDates) {
            if (date == currentDate) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else if (date.isBefore(currentDate)) {
                break
            }
        }

        return streak
    }

    /** Save a new StudyEvent */
    fun addStudyEvent(event: StudyEvent) {
        viewModelScope.launch {
            dbManager.createStudyEvent(event)
                .onSuccess { loadInitialData() }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to add event: ${exception.message}")
                    }
                }
        }
    }

    /** Show/hide the create event dialog */
    fun showAddEventDialog(show: Boolean) {
        _uiState.update { it.copy(showAddEventDialog = show) }
    }

    /** Track selected date for calendar view */
    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    /** NEW: Toggle timetable view */
    fun toggleTimetableView() {
        _uiState.update {
            it.copy(showTimetableView = !it.showTimetableView)
        }
    }

    /** Reset error state */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Load last 5 study sessions */
    private fun loadRecentSessions() {
        val userId = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("users/$userId/sessions")

        ref.orderByChild("startTime")
            .limitToLast(5)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sessions = snapshot.children.mapNotNull {
                        it.getValue(StudySession::class.java)
                    }.sortedByDescending { it.startTime }

                    _uiState.update { it.copy(recentSessions = sessions) }
                }

                override fun onCancelled(error: DatabaseError) {
                    _uiState.update {
                        it.copy(errorMessage = "Failed to load recent sessions: ${error.message}")
                    }
                }
            })
    }

    /** NEW: Start a study session */
    fun startStudySession(moduleId: String) {
        viewModelScope.launch {
            try {
                val session = StudySession(
                    id = java.util.UUID.randomUUID().toString(),
                    moduleId = moduleId,
                    startTime = System.currentTimeMillis(),
                    duration = 0, // Will be updated when session ends
                    type = "STUDY"
                )

                // You would save this session to your database
                // dbManager.createStudySession(session)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to start session: ${e.message}")
                }
            }
        }
    }
}