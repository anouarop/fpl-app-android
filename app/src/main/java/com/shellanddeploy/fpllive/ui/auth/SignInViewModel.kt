package com.shellanddeploy.fpllive.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.auth.AuthenticationRepository
import com.shellanddeploy.fpllive.data.auth.AuthResult
import com.shellanddeploy.fpllive.data.auth.FplAuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val authState: FplAuthState = FplAuthState.SignedOut,
    val message: String? = null,
)

class SignInViewModel(
    private val authentication: AuthenticationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authentication.state.collect { authState ->
                _state.update { it.copy(authState = authState) }
            }
        }
    }

    fun setEmail(email: String) = _state.update { it.copy(email = email) }

    fun setPassword(password: String) = _state.update { it.copy(password = password) }

    fun signIn() {
        viewModelScope.launch {
            val result = authentication.signIn(_state.value.email, _state.value.password)
            when (result) {
                is AuthResult.Success -> _state.update { it.copy(message = null) }
                is AuthResult.Failure -> _state.update { it.copy(message = result.message) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authentication.signOut() }
    }
}
