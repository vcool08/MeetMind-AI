package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthenticationRepository
import com.example.data.FirebaseAuthRepository
import com.example.model.AuthUser
import com.example.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthUiState {
  object Idle : AuthUiState()
  object Loading : AuthUiState()
  data class Authenticated(val user: AuthUser) : AuthUiState()
  data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
  private val authRepository: AuthenticationRepository = FirebaseAuthRepository.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  val currentUser: StateFlow<AuthUser?> = authRepository.currentUser

  val isAuthenticated: StateFlow<Boolean> = currentUser
    .map { it != null }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = authRepository.currentUser.value != null
    )

  init {
    viewModelScope.launch {
      authRepository.currentUser.collect { user ->
        if (user != null) {
          _uiState.value = AuthUiState.Authenticated(user)
        } else if (_uiState.value is AuthUiState.Authenticated) {
          _uiState.value = AuthUiState.Idle
        }
      }
    }
  }

  /**
   * Performs user login with email and password.
   */
  fun login(email: String, password: String) {
    val validationError = validateLoginForm(email, password)
    if (validationError != null) {
      _uiState.value = AuthUiState.Error(validationError)
      return
    }

    _uiState.value = AuthUiState.Loading
    viewModelScope.launch {
      val result = authRepository.login(email.trim(), password)
      result.fold(
        onSuccess = { user ->
          _uiState.value = AuthUiState.Authenticated(user)
        },
        onFailure = { error ->
          _uiState.value = AuthUiState.Error(
            error.message ?: "Authentication failed. Please check your credentials."
          )
        }
      )
    }
  }

  /**
   * Performs user registration with required enterprise profile attributes.
   */
  fun register(
    name: String,
    email: String,
    password: String,
    confirmPassword: String = password,
    role: UserRole = UserRole.EMPLOYEE,
    department: String = "Engineering"
  ) {
    val validationError = validateRegistrationForm(name, email, password, confirmPassword)
    if (validationError != null) {
      _uiState.value = AuthUiState.Error(validationError)
      return
    }

    _uiState.value = AuthUiState.Loading
    viewModelScope.launch {
      val result = authRepository.register(
        name = name.trim(),
        email = email.trim(),
        password = password,
        role = role,
        department = department.trim()
      )
      result.fold(
        onSuccess = { user ->
          _uiState.value = AuthUiState.Authenticated(user)
        },
        onFailure = { error ->
          _uiState.value = AuthUiState.Error(
            error.message ?: "Registration failed. Please try again."
          )
        }
      )
    }
  }

  /**
   * Performs sign-in via Google credentials / ID token.
   */
  fun signInWithGoogle(idToken: String) {
    if (idToken.isBlank()) {
      _uiState.value = AuthUiState.Error("Google sign-in token cannot be empty.")
      return
    }

    _uiState.value = AuthUiState.Loading
    viewModelScope.launch {
      val result = authRepository.signInWithGoogle(idToken)
      result.fold(
        onSuccess = { user ->
          _uiState.value = AuthUiState.Authenticated(user)
        },
        onFailure = { error ->
          _uiState.value = AuthUiState.Error(
            error.message ?: "Google sign-in failed. Please try again."
          )
        }
      )
    }
  }

  /**
   * Sets authenticated user state directly from UserAuthManager.
   */
  fun onAuthenticated(user: AuthUser) {
    _uiState.value = AuthUiState.Authenticated(user)
  }

  /**
   * Sets error state from Google Sign-In failure.
   */
  fun onGoogleSignInError(message: String) {
    _uiState.value = AuthUiState.Error(message)
  }

  /**
   * Signs the current user out of their session.
   */
  fun signOut() {
    viewModelScope.launch {
      authRepository.signOut()
      _uiState.value = AuthUiState.Idle
    }
  }

  /**
   * Clears the current error state.
   */
  fun clearError() {
    if (_uiState.value is AuthUiState.Error) {
      _uiState.value = AuthUiState.Idle
    }
  }

  // --- Form Validation Helpers ---

  private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

  fun validateLoginForm(email: String, password: String): String? {
    if (email.isBlank()) return "Please enter your email address."
    if (!emailRegex.matches(email.trim())) {
      return "Please enter a valid email address."
    }
    if (password.isBlank()) return "Please enter your password."
    if (password.length < 6) return "Password must be at least 6 characters."
    return null
  }

  fun validateRegistrationForm(
    name: String,
    email: String,
    password: String,
    confirmPassword: String
  ): String? {
    if (name.isBlank()) return "Please enter your full name."
    if (email.isBlank()) return "Please enter your work email."
    if (!emailRegex.matches(email.trim())) {
      return "Please enter a valid work email address."
    }
    if (password.length < 6) return "Password must be at least 6 characters."
    if (password != confirmPassword) return "Passwords do not match."
    return null
  }
}
