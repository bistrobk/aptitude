// File: viewModel/HomeViewModel.kt
package com.griffith.chatbot.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griffith.chatbot.data.models.Module
import com.griffith.chatbot.data.models.Lecture
import com.griffith.chatbot.data.models.StudyEvent
import com.griffith.chatbot.data.repository.DatabaseManager
import com.griffith.chatbot.data.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val modules: List<Module>,
        val recentLectures: List<Lecture>,
        val upcomingEvents: List<StudyEvent>,
        val studyStats: Map<String, Any>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel : ViewModel() {
    private val databaseManager = DatabaseManager.getInstance()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        loadUserProfile()
        loadHomeData()
    }

    fun refresh() {
        loadUserProfile()
        loadHomeData()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val profileResult = databaseManager.getUserProfile()
                _userProfile.value = profileResult.getOrNull()
                Log.d("HomeViewModel", "User profile loaded: ${_userProfile.value?.name}")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading user profile", e)
            }
        }
    }

    fun getCurrentUserEmail(): String? {
        return databaseManager.getCurrentUserEmail()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            try {
                // Load all data in parallel
                val modulesResult = databaseManager.getModules()
                val lecturesResult = databaseManager.getAllLectures()
                val eventsResult = databaseManager.getStudyEvents()
                val statsResult = databaseManager.getUserStats()

                // Check if all operations were successful
                val modules = modulesResult.getOrElse {
                    Log.e("HomeViewModel", "Failed to load modules: ${it.message}")
                    emptyList()
                }

                val allLectures = lecturesResult.getOrElse {
                    Log.e("HomeViewModel", "Failed to load lectures: ${it.message}")
                    emptyList()
                }

                val events = eventsResult.getOrElse {
                    Log.e("HomeViewModel", "Failed to load events: ${it.message}")
                    emptyList()
                }

                val stats = statsResult.getOrElse {
                    Log.e("HomeViewModel", "Failed to load stats: ${it.message}")
                    emptyMap()
                }

                // Process data
                val activeModules = modules.filter { it.isActive }
                val recentLectures = allLectures
                    .sortedByDescending { it.lastModified }
                    .take(5)

                val currentTime = System.currentTimeMillis()
                val upcomingEvents = events
                    .filter { it.startTime > currentTime }
                    .sortedBy { it.startTime }
                    .take(5)

                _uiState.value = HomeUiState.Success(
                    modules = activeModules,
                    recentLectures = recentLectures,
                    upcomingEvents = upcomingEvents,
                    studyStats = stats
                )

                Log.d("HomeViewModel", "Home data loaded successfully")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading home data", e)
                _uiState.value = HomeUiState.Error(
                    message = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    // Method to get filtered modules by status
    fun getActiveModules(): List<Module> {
        return when (val state = _uiState.value) {
            is HomeUiState.Success -> state.modules
            else -> emptyList()
        }
    }

    // Method to get recent activity summary
    fun getRecentActivitySummary(): String {
        return when (val state = _uiState.value) {
            is HomeUiState.Success -> {
                val moduleCount = state.modules.size
                val lectureCount = state.recentLectures.size
                val eventCount = state.upcomingEvents.size
                "You have $moduleCount active modules, $lectureCount recent lectures, and $eventCount upcoming events."
            }
            else -> "Loading your study summary..."
        }
    }

    // Method to refresh specific data
    fun refreshModules() {
        viewModelScope.launch {
            try {
                val modulesResult = databaseManager.getModules()
                val modules = modulesResult.getOrThrow()

                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    _uiState.value = currentState.copy(
                        modules = modules.filter { it.isActive }
                    )
                }
                Log.d("HomeViewModel", "Modules refreshed successfully")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing modules", e)
            }
        }
    }

    fun refreshLectures() {
        viewModelScope.launch {
            try {
                val lecturesResult = databaseManager.getAllLectures()
                val lectures = lecturesResult.getOrThrow()

                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    _uiState.value = currentState.copy(
                        recentLectures = lectures
                            .sortedByDescending { it.lastModified }
                            .take(5)
                    )
                }
                Log.d("HomeViewModel", "Lectures refreshed successfully")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing lectures", e)
            }
        }
    }

    fun refreshEvents() {
        viewModelScope.launch {
            try {
                val eventsResult = databaseManager.getStudyEvents()
                val events = eventsResult.getOrThrow()

                val currentTime = System.currentTimeMillis()
                val upcomingEvents = events
                    .filter { it.startTime > currentTime }
                    .sortedBy { it.startTime }
                    .take(5)

                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    _uiState.value = currentState.copy(
                        upcomingEvents = upcomingEvents
                    )
                }
                Log.d("HomeViewModel", "Events refreshed successfully")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing events", e)
            }
        }
    }

    // Method to check if user has any data
    fun hasUserData(): Boolean {
        return when (val state = _uiState.value) {
            is HomeUiState.Success -> {
                state.modules.isNotEmpty() ||
                        state.recentLectures.isNotEmpty() ||
                        state.upcomingEvents.isNotEmpty()
            }
            else -> false
        }
    }

    // Method to get quick stats for dashboard
    fun getQuickStats(): Map<String, Int> {
        return when (val state = _uiState.value) {
            is HomeUiState.Success -> mapOf(
                "modules" to state.modules.size,
                "lectures" to state.recentLectures.size,
                "events" to state.upcomingEvents.size,
                "totalModules" to (state.studyStats["total_modules"] as? Int ?: 0),
                "totalLectures" to (state.studyStats["total_lectures"] as? Int ?: 0),
                "totalEvents" to (state.studyStats["total_study_events"] as? Int ?: 0)
            )
            else -> emptyMap()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("HomeViewModel", "HomeViewModel cleared")
    }
}