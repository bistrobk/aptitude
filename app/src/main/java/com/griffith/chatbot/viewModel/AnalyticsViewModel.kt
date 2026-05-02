package com.griffith.chatbot.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.griffith.chatbot.data.models.*
import com.griffith.chatbot.data.repository.DatabaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

class AnalyticsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val dbManager = DatabaseManager.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadAnalyticsData()
    }

    private fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState.Loading

            try {
                // Load data in parallel for better performance
                val modulesDeferred = async { dbManager.getModules() }
                val eventsDeferred = async { dbManager.getStudyEvents() }
                val sessionsDeferred = async { loadStudySessions() }
                val goalsDeferred = async { loadStudyGoals() }
                val achievementsDeferred = async { loadAchievements() }

                val modulesResult = modulesDeferred.await()
                val eventsResult = eventsDeferred.await()
                val sessionsResult = sessionsDeferred.await()
                val goalsResult = goalsDeferred.await()
                val achievementsResult = achievementsDeferred.await()

                if (modulesResult.isSuccess && eventsResult.isSuccess) {
                    val modules = modulesResult.getOrDefault(emptyList())
                    val events = eventsResult.getOrDefault(emptyList())
                    val sessions = sessionsResult.getOrDefault(emptyList())
                    val goals = goalsResult.getOrDefault(emptyList())
                    val achievements = achievementsResult.getOrDefault(emptyList())

                    val analytics = calculateAnalytics(modules, events, sessions, goals, achievements)
                    _uiState.value = AnalyticsUiState.Success(analytics)

                } else {
                    val error = modulesResult.exceptionOrNull()?.message
                        ?: eventsResult.exceptionOrNull()?.message
                        ?: "Failed to load analytics data"
                    _uiState.value = AnalyticsUiState.Error(error)
                }

            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.Error("Error loading analytics: ${e.message}")
            }
        }
    }

    private suspend fun loadStudySessions(): Result<List<StudySession>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))

            val sessions = mutableListOf<StudySession>()
            val ref = FirebaseDatabase.getInstance().getReference("users/$userId/sessions")

            val snapshot = ref.get().await()
            snapshot.children.forEach { sessionSnapshot ->
                sessionSnapshot.getValue(StudySession::class.java)?.let { session ->
                    sessions.add(session)
                }
            }

            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadStudyGoals(): Result<List<StudyGoal>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))

            val goals = mutableListOf<StudyGoal>()
            val ref = FirebaseDatabase.getInstance().getReference("users/$userId/goals")

            val snapshot = ref.get().await()
            snapshot.children.forEach { goalSnapshot ->
                goalSnapshot.getValue(StudyGoal::class.java)?.let { goal ->
                    goals.add(goal)
                }
            }

            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadAchievements(): Result<List<Achievement>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))

            val achievements = mutableListOf<Achievement>()
            val ref = FirebaseDatabase.getInstance().getReference("users/$userId/achievements")

            val snapshot = ref.get().await()
            snapshot.children.forEach { achievementSnapshot ->
                achievementSnapshot.getValue(Achievement::class.java)?.let { achievement ->
                    if (achievement.isUnlocked) {
                        achievements.add(achievement)
                    }
                }
            }

            Result.success(achievements.sortedByDescending { it.unlockedAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateAnalytics(
        modules: List<Module>,
        events: List<StudyEvent>,
        sessions: List<StudySession>,
        goals: List<StudyGoal>,
        achievements: List<Achievement>
    ): StudyAnalytics {

        // Calculate total hours from sessions
        val totalHours = sessions.sumOf { it.duration } / 60.0

        // Calculate streak
        val streakDays = calculateStudyStreak(sessions)

        // Calculate average session length
        val averageSessionLength = if (sessions.isNotEmpty()) {
            sessions.sumOf { it.duration }.toDouble() / sessions.size
        } else 0.0

        // Find most studied module
        val moduleHours = sessions.groupBy { it.moduleId }
            .mapValues { (_, sessions) -> sessions.sumOf { it.duration } / 60.0 }
        val mostStudiedModuleId = moduleHours.maxByOrNull { it.value }?.key ?: ""
        val mostStudiedModule = modules.find { it.id == mostStudiedModuleId }?.name ?: "No data"

        // Calculate weekly progress
        val weeklyProgress = calculateWeeklyProgress(sessions)

        // Calculate monthly progress
        val monthlyProgress = calculateMonthlyProgress(sessions)

        // Calculate module breakdown
        val moduleBreakdown = calculateModuleBreakdown(modules, sessions)

        // Calculate productivity trends
        val productivityTrends = calculateProductivityTrends(sessions)

        return StudyAnalytics(
            totalHours = totalHours,
            streakDays = streakDays,
            completedSessions = sessions.size,
            averageSessionLength = averageSessionLength,
            mostStudiedModule = mostStudiedModule,
            weeklyProgress = weeklyProgress,
            monthlyProgress = monthlyProgress,
            moduleBreakdown = moduleBreakdown,
            productivityTrends = productivityTrends,
            goals = goals,
            achievements = achievements
        )
    }

    private fun calculateStudyStreak(sessions: List<StudySession>): Int {
        val today = LocalDate.now()
        val sessionDates = sessions.map { session ->
            Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.distinct().sortedDescending()

        var streak = 0
        var currentDate = today

        for (date in sessionDates) {
            if (date == currentDate) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else if (date.isBefore(currentDate)) {
                break
            }
        }

        return streak
    }

    private fun calculateWeeklyProgress(sessions: List<StudySession>): List<DayProgress> {
        val today = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY)

        return (0..6).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            val daySessions = sessions.filter { session ->
                val sessionDate = Instant.ofEpochMilli(session.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                sessionDate == date
            }

            val hoursStudied = daySessions.sumOf { it.duration } / 60.0
            val targetHours = 2.0 // Default target of 2 hours per day

            DayProgress(
                date = date,
                hoursStudied = hoursStudied,
                sessionsCompleted = daySessions.size,
                targetMet = hoursStudied >= targetHours
            )
        }
    }

    private fun calculateMonthlyProgress(sessions: List<StudySession>): List<MonthProgress> {
        val monthlyData = sessions.groupBy { session ->
            val sessionDate = Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            sessionDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        }

        return monthlyData.map { (monthKey, sessions) ->
            val hoursStudied = sessions.sumOf { it.duration } / 60.0
            val targetHours = 50.0 // Default monthly target

            MonthProgress(
                month = monthKey,
                hoursStudied = hoursStudied,
                targetHours = targetHours
            )
        }.sortedBy { it.month }
    }

    private fun calculateModuleBreakdown(
        modules: List<Module>,
        sessions: List<StudySession>
    ): List<ModuleStats> {
        return modules.map { module ->
            val moduleSessions = sessions.filter { it.moduleId == module.id }
            val hoursStudied = moduleSessions.sumOf { it.duration } / 60.0
            val progress = if (module.totalLectures > 0) {
                module.completedLectures.toFloat() / module.totalLectures
            } else 0f

            ModuleStats(
                moduleId = module.id,
                moduleName = module.name,
                hoursStudied = hoursStudied,
                sessionsCompleted = moduleSessions.size,
                averageScore = 0.0, // You can add scoring if available
                progress = progress,
                color = module.color
            )
        }.sortedByDescending { it.hoursStudied }
    }

    private fun calculateProductivityTrends(sessions: List<StudySession>): ProductivityData {
        if (sessions.isEmpty()) {
            return ProductivityData()
        }

        // Find best study time
        val hourlyStudy = sessions.groupBy { session ->
            val hour = Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .hour
            when {
                hour < 12 -> "Morning"
                hour < 17 -> "Afternoon"
                hour < 21 -> "Evening"
                else -> "Night"
            }
        }

        val bestStudyTime = hourlyStudy.maxByOrNull { (_, sessions) ->
            sessions.sumOf { it.duration }
        }?.key ?: "Morning"

        // Calculate average focus score (if available in your sessions)
        val averageFocusScore = sessions.mapNotNull { it.focusScore.takeIf { it > 0 } }
            .average().takeIf { !it.isNaN() } ?: 7.5

        // Find most productive day
        val dailyStudy = sessions.groupBy { session ->
            Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .dayOfWeek.name
        }

        val mostProductiveDay = dailyStudy.maxByOrNull { (_, sessions) ->
            sessions.sumOf { it.duration }
        }?.key ?: "Monday"

        return ProductivityData(
            bestStudyTime = bestStudyTime,
            averageFocusScore = averageFocusScore,
            mostProductiveDay = mostProductiveDay
        )
    }

    fun refresh() {
        loadAnalyticsData()
    }
}

// Updated UI State to include analytics data
sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
    data class Success(val analytics: StudyAnalytics) : AnalyticsUiState()
}