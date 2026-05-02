package com.griffith.chatbot.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griffith.chatbot.data.models.*
import com.griffith.chatbot.data.repository.DatabaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class EventsViewModel : ViewModel() {

    private val databaseManager = DatabaseManager.getInstance()

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _editingEvent = MutableStateFlow<StudyEvent?>(null)
    val editingEvent: StateFlow<StudyEvent?> = _editingEvent.asStateFlow()

    private val _modules = MutableStateFlow<List<Module>>(emptyList())
    val modules: StateFlow<List<Module>> = _modules.asStateFlow()

    init {
        loadEvents()
        loadModules()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = EventsUiState.Loading

            try {
                val eventsResult = databaseManager.getStudyEvents()
                eventsResult.onSuccess { events ->
                    _uiState.value = EventsUiState.Success(events = events)
                }.onFailure { exception ->
                    _uiState.value = EventsUiState.Error(
                        message = exception.message ?: "Failed to load events"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(
                    message = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun loadModules() {
        viewModelScope.launch {
            val modulesResult = databaseManager.getModules()
            modulesResult.onSuccess { moduleList ->
                _modules.value = moduleList
            }
        }
    }

    fun createEvent(event: StudyEvent) {
        viewModelScope.launch {
            try {
                val result = databaseManager.createStudyEvent(event)
                result.onSuccess {
                    loadEvents() // Reload events
                    hideCreateDialog()
                }.onFailure { exception ->
                    _uiState.value = EventsUiState.Error(
                        message = "Failed to create event: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(
                    message = "Error creating event: ${e.message}"
                )
            }
        }
    }

    fun updateEvent(event: StudyEvent) {
        viewModelScope.launch {
            try {
                val result = databaseManager.updateStudyEvent(event)
                result.onSuccess {
                    loadEvents() // Reload events
                    hideEditDialog()
                }.onFailure { exception ->
                    _uiState.value = EventsUiState.Error(
                        message = "Failed to update event: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(
                    message = "Error updating event: ${e.message}"
                )
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                val result = databaseManager.deleteStudyEvent(eventId)
                result.onSuccess {
                    loadEvents() // Reload events
                }.onFailure { exception ->
                    _uiState.value = EventsUiState.Error(
                        message = "Failed to delete event: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = EventsUiState.Error(
                    message = "Error deleting event: ${e.message}"
                )
            }
        }
    }

    fun markEventComplete(eventId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is EventsUiState.Success) {
                val event = currentState.events.find { it.id == eventId }
                event?.let {
                    updateEvent(it.copy(isCompleted = true))
                }
            }
        }
    }

    // Dialog management
    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun showEditDialog(event: StudyEvent) {
        _editingEvent.value = event
    }

    fun hideEditDialog() {
        _editingEvent.value = null
    }

    fun clearError() {
        val currentState = _uiState.value
        if (currentState is EventsUiState.Error) {
            loadEvents()
        }
    }
}

sealed class EventsUiState {
    object Loading : EventsUiState()

    data class Error(
        val message: String
    ) : EventsUiState()

    data class Success(
        val events: List<StudyEvent>
    ) : EventsUiState()
}