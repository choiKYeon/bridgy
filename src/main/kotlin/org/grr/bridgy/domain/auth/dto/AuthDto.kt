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

data class FindIdRequest(
    val nickname: String
)

data class FindIdResponse(
    val maskedEmail: String
)

data class FindPasswordRequest(
    val email: String
)

data class FindPasswordResponse(
    val resetCode: String,
    val expiresInMinutes: Int = 10,
    val message: String = "인증 코드가 발급되었습니다. 10분 내에 사용해주세요."
)

data class ResetPasswordRequest(
    val email: String,
    val resetCode: String,
    val newPassword: String
)
