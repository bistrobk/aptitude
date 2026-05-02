package com.griffith.chatbot.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Represents the different states the authentication UI can be in.
 * This makes state management in the UI type-safe and exhaustive.
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data object PasswordResetSent : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // A private MutableStateFlow that the ViewModel uses to emit states.
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    // An immutable StateFlow that the UI can collect to observe state changes.
    val uiState = _uiState.asStateFlow()

    val currentUser get() = auth.currentUser

    /**
     * Signs up a new user using email and password.
     * The result is communicated through the uiState Flow.
     */
    fun signup(email: String, password: String) {
        if (!isValidEmail(email)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }

        if (!isValidPassword(password)) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters long.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _uiState.value = AuthUiState.Success
            } catch (e: FirebaseAuthException) {
                _uiState.value = AuthUiState.Error(getFirebaseErrorMessage(e))
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "An unknown signup error occurred.")
            }
        }
    }

    /**
     * Logs in an existing user using email and password.
     * The result is communicated through the uiState Flow.
     */
    fun login(email: String, password: String) {
        if (!isValidEmail(email)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }

        if (password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _uiState.value = AuthUiState.Success
            } catch (e: FirebaseAuthException) {
                _uiState.value = AuthUiState.Error(getFirebaseErrorMessage(e))
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "An unknown login error occurred.")
            }
        }
    }

    /**
     * Sends a password reset email to the specified email address.
     * The result is communicated through the uiState Flow.
     */
    fun resetPassword(email: String) {
        if (!isValidEmail(email)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.value = AuthUiState.PasswordResetSent
            } catch (e: FirebaseAuthException) {
                _uiState.value = AuthUiState.Error(getFirebaseErrorMessage(e))
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to send password reset email.")
            }
        }
    }


    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Validates email format using a simple regex pattern.
     */
    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validates password requirements.
     */
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Converts Firebase auth error codes to user-friendly messages.
     */
    private fun getFirebaseErrorMessage(exception: FirebaseAuthException): String {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> "The email address is not valid."
            "ERROR_WRONG_PASSWORD" -> "The password is incorrect."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email address."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Please try again later."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email/password sign-in is not enabled."
            "ERROR_WEAK_PASSWORD" -> "The password is too weak. Please choose a stronger password."
            "ERROR_INVALID_CREDENTIAL" -> "The credentials provided are invalid."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "An account already exists with a different sign-in method."
            "ERROR_REQUIRES_RECENT_LOGIN" -> "This operation requires recent authentication. Please log in again."
            "ERROR_CREDENTIAL_ALREADY_IN_USE" -> "This credential is already associated with a different account."
            "ERROR_USER_TOKEN_EXPIRED" -> "Your session has expired. Please log in again."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection and try again."
            "ERROR_INVALID_API_KEY" -> "API key configuration error. Please contact support."
            "ERROR_USER_MISMATCH" -> "The supplied credentials do not correspond to the previously signed-in user."
            else -> exception.message ?: "An authentication error occurred. Please try again."
        }
    }
}