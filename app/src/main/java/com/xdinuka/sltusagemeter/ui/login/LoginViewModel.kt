package com.xdinuka.sltusagemeter.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xdinuka.sltusagemeter.data.repository.SltRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: SltRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state = _state.asStateFlow()

    private val _navigateToMain = MutableSharedFlow<Unit>()
    val navigateToMain = _navigateToMain.asSharedFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginUiState.Error("Please enter your email and password")
            return
        }
        viewModelScope.launch {
            _state.value = LoginUiState.Loading
            try {
                repository.login(username.trim(), password)
                _state.value = LoginUiState.Success
                _navigateToMain.emit(Unit)
            } catch (e: Exception) {
                _state.value = LoginUiState.Error(e.message ?: "Login failed. Please try again.")
            }
        }
    }
}
