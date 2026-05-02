package com.griffith.chatbot.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth

import com.griffith.chatbot.ui.screens.modules.ModuleScreen
import com.griffith.chatbot.ui.screens.AnalyticsScreen
import com.griffith.chatbot.ui.screens.HomeScreen
import com.griffith.chatbot.ui.screens.modules.LectureScreen
import com.griffith.chatbot.ui.screens.ProgressScreen
import com.griffith.chatbot.ui.screens.SettingsScreen
import com.griffith.chatbot.ui.auth.LoginScreen
import com.griffith.chatbot.ui.screens.SignupScreen
import com.griffith.chatbot.viewModel.AuthViewModel
import com.griffith.chatbot.viewModel.ChatViewModel
import com.griffith.chatbot.viewModel.LectureViewModel
import com.griffith.chatbot.viewModel.SettingsViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChatbotApp(settingsViewModel: SettingsViewModel? = null) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    // Use the passed settingsViewModel or create a new one
    val appSettingsViewModel: SettingsViewModel = settingsViewModel ?: viewModel()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Change startDestination to "home" instead of "modules"
    val startDestination = if (authViewModel.currentUser != null) "home" else "login"

    val hideBottomBarRoutes = setOf(
        "login",
        "signup",
        "forgot_password",
        "lectures/{moduleId}"
    )

    Scaffold(
        bottomBar = {
            if (currentRoute !in hideBottomBarRoutes) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            AppNavHost(
                navController = navController,
                authViewModel = authViewModel,
                chatViewModel = chatViewModel,
                settingsViewModel = appSettingsViewModel,
                startDestination = startDestination
            )
        }
    }
}

// Updated AppNavHost function for your navigation file
@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- Authentication Flow ---
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate("signup") },
                onNavigateToForgotPassword = { /* TODO */ }
            )
        }

        composable("signup") {
            SignupScreen(
                authViewModel = authViewModel,
                onSignupSuccess = {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToModules = {
                    navController.navigate("modules") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToLectures = {
                    navController.navigate("modules") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToAnalytics = {
                    navController.navigate("analytics") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("settings") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onModuleClick = { moduleId ->
                    navController.navigate("lectures/$moduleId")
                },
                onLectureClick = { moduleId, lectureId ->
                    navController.navigate("lectures/$moduleId")
                },

            )
        }

        // --- Main App Screens ---
        composable("modules") {
            ModuleScreen(
                onModuleSelected = { moduleId ->
                    navController.navigate("lectures/$moduleId")
                },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("modules") { inclusive = true }
                    }
                }
            )
        }

        composable("progress") {
            ProgressScreen()
        }

        composable("analytics") {
            AnalyticsScreen()
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                settingsViewModel = settingsViewModel
            )
        }

        // --- Lecture Screen ---
        composable(
            route = "lectures/{moduleId}",
            arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId").orEmpty()

            LectureScreen(
                moduleId = moduleId,
                moduleName = "Module",
                navController = navController,
                lectureViewModel = viewModel<LectureViewModel>(),
                chatViewModel = chatViewModel
            )
        }
    }
}