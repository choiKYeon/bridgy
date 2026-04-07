package org.grr.bridgy.domain.auth.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer"
)

data class TokenRefreshRequest(
    val refreshToken: String
)
