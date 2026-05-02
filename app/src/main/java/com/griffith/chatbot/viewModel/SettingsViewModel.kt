package com.griffith.chatbot.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.griffith.chatbot.data.repository.DatabaseManager

import com.griffith.chatbot.data.repository.UserProfile

// UI State for Settings Screen
data class SettingsUiState(
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val userStats: Map<String, Any> = emptyMap(),
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val databaseManager = DatabaseManager.getInstance()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    // Load user profile and stats
    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load user profile
                val profileResult = databaseManager.getUserProfile()
                val profile = profileResult.getOrNull()

                // Load user stats
                val statsResult = databaseManager.getUserStats()
                val stats = statsResult.getOrNull() ?: emptyMap()

                // If no profile exists, create a default one
                if (profile == null) {
                    databaseManager.initializeDefaultProfile()
                    val newProfileResult = databaseManager.getUserProfile()
                    val newProfile = newProfileResult.getOrNull()

                    _uiState.value = _uiState.value.copy(
                        userProfile = newProfile,
                        userStats = stats,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        userProfile = profile,
                        userStats = stats,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load user data: ${e.message}"
                )
            }
        }
    }

    // Update user profile
    fun updateProfile(name: String, phoneNumber: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val currentProfile = _uiState.value.userProfile
                if (currentProfile != null) {
                    val updatedProfile = currentProfile.copy(
                        name = name,
                        phoneNumber = phoneNumber,
                        updatedAt = System.currentTimeMillis()
                    )

                    val result = databaseManager.saveUserProfile(updatedProfile)
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            userProfile = updatedProfile,
                            isLoading = false,
                            successMessage = "Profile updated successfully"
                        )
                        clearMessageAfterDelay()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to update profile: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error updating profile: ${e.message}"
                )
            }
        }
    }

    // Upload profile image
    fun uploadProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isUploading = true, error = null)

                val result = databaseManager.uploadProfileImage(imageUri)
                if (result.isSuccess) {
                    val imageUrl = result.getOrNull()
                    val currentProfile = _uiState.value.userProfile
                    if (currentProfile != null && imageUrl != null) {
                        val updatedProfile = currentProfile.copy(
                            profileImageUrl = imageUrl,
                            updatedAt = System.currentTimeMillis()
                        )
                        _uiState.value = _uiState.value.copy(
                            userProfile = updatedProfile,
                            isUploading = false,
                            successMessage = "Profile image updated successfully"
                        )
                        clearMessageAfterDelay()
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = "Failed to upload image: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = "Error uploading image: ${e.message}"
                )
            }
        }
    }

    // Update theme
    fun updateTheme(theme: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val result = databaseManager.updateUserTheme(theme)
                if (result.isSuccess) {
                    val currentProfile = _uiState.value.userProfile
                    if (currentProfile != null) {
                        val updatedProfile = currentProfile.copy(
                            theme = theme,
                            updatedAt = System.currentTimeMillis()
                        )
                        _uiState.value = _uiState.value.copy(
                            userProfile = updatedProfile,
                            isLoading = false,
                            successMessage = "Theme updated to ${theme.replaceFirstChar { it.uppercase() }}"
                        )
                        clearMessageAfterDelay()
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update theme: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error updating theme: ${e.message}"
                )
            }
        }
    }

    // Update notification settings
    fun updateNotificationSettings(
        notificationsEnabled: Boolean,
        studyReminders: Boolean,
        emailNotifications: Boolean
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val result = databaseManager.updateNotificationSettings(
                    notificationsEnabled = notificationsEnabled,
                    studyReminders = studyReminders,
                    emailNotifications = emailNotifications
                )

                if (result.isSuccess) {
                    val currentProfile = _uiState.value.userProfile
                    if (currentProfile != null) {
                        val updatedProfile = currentProfile.copy(
                            notificationsEnabled = notificationsEnabled,
                            studyReminders = studyReminders,
                            emailNotifications = emailNotifications,
                            updatedAt = System.currentTimeMillis()
                        )
                        _uiState.value = _uiState.value.copy(
                            userProfile = updatedProfile,
                            isLoading = false,
                            successMessage = "Notification settings updated"
                        )
                        clearMessageAfterDelay()
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update notifications: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error updating notifications: ${e.message}"
                )
            }
        }
    }

    // Toggle individual notification settings
    fun toggleNotifications(enabled: Boolean) {
        val currentProfile = _uiState.value.userProfile
        if (currentProfile != null) {
            updateNotificationSettings(
                notificationsEnabled = enabled,
                studyReminders = currentProfile.studyReminders,
                emailNotifications = currentProfile.emailNotifications
            )
        }
    }

    fun toggleStudyReminders(enabled: Boolean) {
        val currentProfile = _uiState.value.userProfile
        if (currentProfile != null) {
            updateNotificationSettings(
                notificationsEnabled = currentProfile.notificationsEnabled,
                studyReminders = enabled,
                emailNotifications = currentProfile.emailNotifications
            )
        }
    }

    fun toggleEmailNotifications(enabled: Boolean) {
        val currentProfile = _uiState.value.userProfile
        if (currentProfile != null) {
            updateNotificationSettings(
                notificationsEnabled = currentProfile.notificationsEnabled,
                studyReminders = currentProfile.studyReminders,
                emailNotifications = enabled
            )
        }
    }

    // Export user data
    fun exportUserData(onExportReady: (Map<String, Any?>) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val result = databaseManager.exportUserData()
                if (result.isSuccess) {
                    val exportData = result.getOrNull()
                    if (exportData != null) {
                        onExportReady(exportData)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Data exported successfully"
                        )
                        clearMessageAfterDelay()
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to export data: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error exporting data: ${e.message}"
                )
            }
        }
    }

    // Sign out user
    fun signOut(onSignOutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSigningOut = true, error = null)

                val result = databaseManager.signOutUser()
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSigningOut = false,
                        successMessage = "Signed out successfully"
                    )
                    onSignOutComplete()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSigningOut = false,
                        error = "Failed to sign out: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSigningOut = false,
                    error = "Error signing out: ${e.message}"
                )
            }
        }
    }

    // Delete user account
    fun deleteAccount(onAccountDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDeletingAccount = true, error = null)

                val result = databaseManager.deleteUserProfile()
                if (result.isSuccess) {
                    // Also sign out the user
                    databaseManager.signOutUser()
                    _uiState.value = _uiState.value.copy(
                        isDeletingAccount = false,
                        successMessage = "Account deleted successfully"
                    )
                    onAccountDeleted()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDeletingAccount = false,
                        error = "Failed to delete account: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = false,
                    error = "Error deleting account: ${e.message}"
                )
            }
        }
    }

    // Refresh user data
    fun refresh() {
        loadUserData()
    }

    // Clear error message
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Clear success message
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    // Get current user email
    fun getCurrentUserEmail(): String? {
        return databaseManager.getCurrentUserEmail()
    }

    // Get current user display name
    fun getCurrentUserDisplayName(): String? {
        return databaseManager.getCurrentUserDisplayName()
    }

    // Check if user is authenticated
    fun isUserAuthenticated(): Boolean {
        return databaseManager.isUserAuthenticated()
    }

    // Get current preferences (for backward compatibility)
    fun getCurrentPreferences(): UserProfile? {
        return _uiState.value.userProfile
    }

    // Get available themes
    fun getAvailableThemes(): List<String> {
        return listOf("system", "light", "dark")
    }

    // Get theme display name
    fun getThemeDisplayName(theme: String): String {
        return when (theme) {
            "system" -> "System Default"
            "light" -> "Light"
            "dark" -> "Dark"
            else -> theme.replaceFirstChar { it.uppercase() }
        }
    }

    // Private helper to clear messages after delay
    private fun clearMessageAfterDelay() {
        viewModelScope.launch {
            delay(3000) // Clear after 3 seconds
            _uiState.value = _uiState.value.copy(
                error = null,
                successMessage = null
            )
        }
    }

    // Get user stats formatted for display
    fun getFormattedUserStats(): Map<String, String> {
        val stats = _uiState.value.userStats
        return mapOf(
            "Total Modules" to (stats["total_modules"]?.toString() ?: "0"),
            "Active Modules" to (stats["active_modules"]?.toString() ?: "0"),
            "Total Lectures" to (stats["total_lectures"]?.toString() ?: "0"),
            "Study Events" to (stats["total_study_events"]?.toString() ?: "0"),
            "Account Age" to "${stats["account_age_days"]?.toString() ?: "0"} days"
        )
    }
}