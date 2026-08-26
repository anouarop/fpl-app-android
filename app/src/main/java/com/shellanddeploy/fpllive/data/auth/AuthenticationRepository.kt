package com.shellanddeploy.fpllive.data.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class FplAuthState {
    data object SignedOut : FplAuthState()
    data class SignedIn(val managerName: String) : FplAuthState()
}

sealed class AuthResult {
    data object Success : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * Authentication for the Fantasy Premier League account.
 *
 * The FPL public API has no authentication endpoint — sign-in happens via a web form that sets
 * session cookies, not a documented REST endpoint. This interface defines the contract the rest
 * of the app (transfers, private leagues) would consume once a real authenticated flow exists.
 */
interface AuthenticationRepository {
    val state: Flow<FplAuthState>
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signOut()
}

/**
 * Stand-in implementation: always signed out, sign-in always fails with an explanatory message.
 *
 * TODO/VERIFY: replace with a real implementation (official app deep-link, an authenticated
 * backend proxy, or a documented session flow). Until then the app remains signed out and the
 * features that require auth (making transfers, private leagues) stay read-only.
 */
class UnavailableAuthenticationRepository : AuthenticationRepository {
    private val _state = MutableStateFlow<FplAuthState>(FplAuthState.SignedOut)
    override val state: Flow<FplAuthState> = _state.asStateFlow()

    override suspend fun signIn(email: String, password: String): AuthResult =
        AuthResult.Failure("FPL does not provide a public authentication API. Sign in via the official FPL app or website.")

    override suspend fun signOut() = Unit
}
