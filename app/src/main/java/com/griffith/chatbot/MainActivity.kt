package com.griffith.chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.griffith.chatbot.ui.navigation.ChatbotApp
import com.griffith.chatbot.ui.theme.AptitudeTheme
import com.griffith.chatbot.ui.theme.ChatbotTheme
import com.griffith.chatbot.viewModel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Get the SettingsViewModel to access theme settings
            val settingsViewModel: SettingsViewModel = viewModel()
            val uiState by settingsViewModel.uiState.collectAsState()

            // Determine the theme to use
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val userTheme = uiState.userProfile?.theme ?: "system"

            val darkTheme = when (userTheme) {
                "light" -> false
                "dark" -> true
                "system" -> isSystemInDarkTheme
                else -> isSystemInDarkTheme
            }

            AptitudeTheme(darkTheme = darkTheme) {
                // Pass the settingsViewModel to ChatbotApp so it can be used throughout the app
                ChatbotApp(settingsViewModel = settingsViewModel)
            }
        }
    }
}