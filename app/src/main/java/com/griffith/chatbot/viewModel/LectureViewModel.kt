package com.griffith.chatbot.viewModel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.griffith.chatbot.data.models.Lecture
import com.griffith.chatbot.data.repository.DatabaseManager
import kotlinx.coroutines.launch

class LectureViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val databaseManager = DatabaseManager.getInstance()

    var lectures by mutableStateOf(listOf<Lecture>())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf("")
        private set

    var selectedLecture by mutableStateOf<Lecture?>(null)
        private set

    var showChat by mutableStateOf(false)
        private set

    var showCreateDialog by mutableStateOf(false)
        private set

    var editingLecture by mutableStateOf<Lecture?>(null)
        private set

    var newLectureTitle by mutableStateOf("")
        private set

    private var isCreatingLecture = false

    fun loadLectures(moduleId: String) {
        if (moduleId.isBlank()) return

        isLoading = true
        errorMessage = ""

        viewModelScope.launch {
            val result = databaseManager.getLecturesForModule(moduleId)
            if (result.isSuccess) {
                lectures = result.getOrNull() ?: emptyList()
            } else {
                errorMessage = "Failed to load lectures: ${result.exceptionOrNull()?.message}"
            }
            isLoading = false
        }
    }

    fun selectLecture(lecture: Lecture) {
        selectedLecture = lecture
        showChat = true
    }

    fun selectLecture(moduleId: String, lectureId: String, lectureTitle: String) {
        val lecture = lectures.find { it.id == lectureId }
        if (lecture != null) {
            selectLecture(lecture)
        }
    }

    fun backToList() {
        showChat = false
        selectedLecture = null
    }

    fun showCreateLectureDialog() {
        showCreateDialog = true
        editingLecture = null
        newLectureTitle = ""
    }

    fun editLecture(lecture: Lecture) {
        editingLecture = lecture
        showCreateDialog = true
        newLectureTitle = lecture.title
    }

    fun hideCreateLectureDialog() {
        showCreateDialog = false
        editingLecture = null
        newLectureTitle = ""
    }

    fun updateNewLectureTitle(title: String) {
        newLectureTitle = title
    }

    fun createLecture(moduleId: String, onComplete: () -> Unit = {}) {
        if (newLectureTitle.isBlank() || moduleId.isBlank()) return

        val now = System.currentTimeMillis()

        val lecture = Lecture(
            moduleId = moduleId,
            title = newLectureTitle,
            createdAt = now,
            lastModified = now
        )

        createLecture(moduleId, lecture, onComplete)
    }

    fun createLecture(moduleId: String, lecture: Lecture, onComplete: () -> Unit = {}) {
        if (moduleId.isBlank() || isCreatingLecture) return

        // Check for duplicates
        val isDuplicate = lectures.any {
            it.title.trim().equals(lecture.title.trim(), ignoreCase = true) &&
                    it.timetable?.get("dayOfWeek") == lecture.timetable?.get("dayOfWeek") &&
                    it.timetable?.get("startTime") == lecture.timetable?.get("startTime")
        }

        if (isDuplicate) {
            errorMessage = "A lecture with the same title and time already exists."
            return
        }

        isCreatingLecture = true

        viewModelScope.launch {
            val lectureWithTimestamps = lecture.copy(
                moduleId = moduleId,
                createdAt = if (lecture.createdAt == 0L) System.currentTimeMillis() else lecture.createdAt,
                lastModified = System.currentTimeMillis()
            )

            val result = databaseManager.createLecture(moduleId, lectureWithTimestamps)
            isCreatingLecture = false

            if (result.isSuccess) {
                loadLectures(moduleId)
                hideCreateLectureDialog()
                onComplete()
            } else {
                errorMessage = "Failed to create lecture: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun createDetailedLecture(moduleId: String, lecture: Lecture, onComplete: () -> Unit = {}) {
        createLecture(moduleId, lecture, onComplete)
    }

    fun updateLecture(moduleId: String, lecture: Lecture, onComplete: () -> Unit = {}) {
        if (moduleId.isBlank() || lecture.id.isBlank()) return

        viewModelScope.launch {
            val updatedLecture = lecture.copy(
                moduleId = moduleId,
                lastModified = System.currentTimeMillis()
            )

            val result = databaseManager.updateLecture(moduleId, updatedLecture)
            if (result.isSuccess) {
                loadLectures(moduleId)
                hideCreateLectureDialog()
                onComplete()
            } else {
                errorMessage = "Failed to update lecture: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteLecture(moduleId: String, lectureId: String) {
        if (moduleId.isBlank() || lectureId.isBlank()) return

        viewModelScope.launch {
            val result = databaseManager.deleteLecture(moduleId, lectureId)
            if (result.isSuccess) {
                loadLectures(moduleId)
                if (selectedLecture?.id == lectureId) {
                    backToList()
                }
            } else {
                errorMessage = "Failed to delete lecture: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun searchLectures(moduleId: String, query: String) {
        if (query.isBlank()) {
            loadLectures(moduleId)
            return
        }

        viewModelScope.launch {
            val result = databaseManager.searchLectures(query)
            if (result.isSuccess) {
                lectures = result.getOrNull()?.filter { it.moduleId == moduleId } ?: emptyList()
            } else {
                errorMessage = "Failed to search lectures: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun getLecturesByTag(moduleId: String, tag: String) {
        viewModelScope.launch {
            val result = databaseManager.getLecturesByTag(tag)
            if (result.isSuccess) {
                lectures = result.getOrNull()?.filter { it.moduleId == moduleId } ?: emptyList()
            } else {
                errorMessage = "Failed to get lectures by tag: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun getLecture(moduleId: String, lectureId: String, onResult: (Lecture?) -> Unit) {
        viewModelScope.launch {
            val result = databaseManager.getLecture(moduleId, lectureId)
            if (result.isSuccess) {
                onResult(result.getOrNull())
            } else {
                errorMessage = "Failed to get lecture: ${result.exceptionOrNull()?.message}"
                onResult(null)
            }
        }
    }

    fun refreshLectures(moduleId: String) {
        if (moduleId.isNotBlank()) {
            loadLectures(moduleId)
        }
    }

    fun clearError() {
        errorMessage = ""
    }

    fun hasLectures(): Boolean = lectures.isNotEmpty()

    fun getLectureCount(): Int = lectures.size

    fun sortLecturesByTitle() {
        lectures = lectures.sortedBy { it.title }
    }

    fun sortLecturesByDate() {
        lectures = lectures.sortedByDescending { it.createdAt }
    }

    fun sortLecturesByLastModified() {
        lectures = lectures.sortedByDescending { it.lastModified }
    }

    fun cleanupLectureDuplicates(
        moduleId: String,
        title: String,
        onCleanupComplete: (deletedCount: Int, error: String?) -> Unit
    ) {
        if (moduleId.isBlank() || title.isBlank()) {
            onCleanupComplete(0, "Module ID and title cannot be blank.")
            return
        }
        isLoading = true
        errorMessage = null.toString()
        viewModelScope.launch {
            val result = databaseManager.cleanupDuplicateLecturesByTitle(moduleId, title)
            isLoading = false
            if (result.isSuccess) {
                val deletedCount = result.getOrNull() ?: 0
                if (deletedCount > 0) loadLectures(moduleId)
                onCleanupComplete(deletedCount, null)
            } else {
                val errorMsg = "Failed to cleanup duplicates: ${result.exceptionOrNull()?.message}"
                errorMessage = errorMsg
                onCleanupComplete(0, errorMsg)
            }
        }
    }
}