package com.griffith.chatbot.viewModel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griffith.chatbot.data.models.Module
import com.griffith.chatbot.data.repository.DatabaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModuleViewModel : ViewModel() {

    private val databaseManager = DatabaseManager.getInstance()

    private val _uiState = MutableStateFlow<ModuleUiState>(ModuleUiState.Loading)
    val uiState: StateFlow<ModuleUiState> = _uiState.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _showEditDialog = MutableStateFlow<Module?>(null)
    val showEditDialog: StateFlow<Module?> = _showEditDialog.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private var allModules: List<Module> = emptyList()

    init {
        loadModules()
    }

    fun loadModules() {
        viewModelScope.launch {
            _uiState.value = ModuleUiState.Loading

            try {
                val result = databaseManager.getModules()
                if (result.isSuccess) {
                    allModules = result.getOrNull() ?: emptyList()
                    _uiState.value = ModuleUiState.Success(allModules)
                } else {
                    val error = result.exceptionOrNull()
                    _uiState.value = ModuleUiState.Error(
                        message = error?.message ?: "Failed to load modules"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ModuleUiState.Error(
                    message = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun getFilteredModules(): List<Module> {
        return if (searchQuery.value.isBlank()) {
            allModules
        } else {
            allModules.filter { module ->
                module.name.contains(searchQuery.value, ignoreCase = true) ||
                        module.moduleCode.contains(searchQuery.value, ignoreCase = true) ||
                        module.lecturerName.contains(searchQuery.value, ignoreCase = true)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun showEditDialog(module: Module) {
        _showEditDialog.value = module
    }

    fun hideEditDialog() {
        _showEditDialog.value = null
    }

    fun createModule(module: Module) {
        viewModelScope.launch {
            try {
                val result = databaseManager.createModule(module)
                if (result.isSuccess) {
                    hideCreateDialog()
                    loadModules() // Reload modules to show the new one
                } else {
                    val error = result.exceptionOrNull()
                    _uiState.value = ModuleUiState.Error(
                        message = error?.message ?: "Failed to create module"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ModuleUiState.Error(
                    message = e.message ?: "Failed to create module"
                )
            }
        }
    }

    fun updateModule(module: Module) {
        viewModelScope.launch {
            try {
                val result = databaseManager.updateModule(module)
                if (result.isSuccess) {
                    hideEditDialog()
                    loadModules() // Reload modules to show updates
                } else {
                    val error = result.exceptionOrNull()
                    _uiState.value = ModuleUiState.Error(
                        message = error?.message ?: "Failed to update module"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ModuleUiState.Error(
                    message = e.message ?: "Failed to update module"
                )
            }
        }
    }

    fun deleteModule(moduleId: String) {
        viewModelScope.launch {
            try {
                val result = databaseManager.deleteModule(moduleId)
                if (result.isSuccess) {
                    loadModules() // Reload modules to remove deleted one
                } else {
                    val error = result.exceptionOrNull()
                    _uiState.value = ModuleUiState.Error(
                        message = error?.message ?: "Failed to delete module"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ModuleUiState.Error(
                    message = e.message ?: "Failed to delete module"
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is ModuleUiState.Error) {
            _uiState.value = ModuleUiState.Success(allModules)
        }
    }
}

sealed class ModuleUiState {
    object Loading : ModuleUiState()

    data class Error(
        val message: String
    ) : ModuleUiState()

    data class Success(
        val modules: List<Module>
    ) : ModuleUiState()
}