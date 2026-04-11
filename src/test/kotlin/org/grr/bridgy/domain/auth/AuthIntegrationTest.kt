package org.grr.bridgy.domain.auth

import org.grr.bridgy.common.BaseIntegrationTest
import org.grr.bridgy.domain.auth.repository.PasswordResetTokenRepository
import org.grr.bridgy.domain.user.entity.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Auth 통합 테스트")
class AuthIntegrationTest : BaseIntegrationTest() {

    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    private lateinit var user: User
    private val rawPassword = "testPassword123!"

    @BeforeEach
    fun setUp() {
        user = userRepository.save(
            User(
                email = "auth@example.com",
                password = passwordEncoder.encode(rawPassword),
                nickname = "인증테스트유저",
                bio = "인증 테스트용 계정"
            )
        )
    }

    @Nested
    @DisplayName("POST /api/v0/auth/login")
    inner class Login {

        @Test
        @DisplayName("올바른 이메일/비밀번호로 로그인 성공 - 토큰 반환")
        fun loginSuccess() {
            val request = mapOf("email" to user.email, "password" to rawPassword)

            mockMvc.perform(
                post("/api/v0/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.access_token").isNotEmpty)
                .andExpect(jsonPath("$.refresh_token").isNotEmpty)
                .andExpect(jsonPath("$.token_type").value("Bearer"))
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 실패 - 401")
        fun loginFailWrongPassword() {
            val request = mapOf("email" to user.email, "password" to "wrongPassword!")

            mockMvc.perform(
                post("/api/v0/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isUnauthorized)
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 실패 - 401")
        fun loginFailWrongEmail() {
            val request = mapOf("email" to "nouser@example.com", "password" to rawPassword)

            mockMvc.perform(
                post("/api/v0/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("POST /api/v0/auth/refresh")
    inner class Refresh {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 새 토큰 발급 성공")
        fun refreshSuccess() {
            // 먼저 로그인해서 리프레시 토큰 획득
            val loginRequest = mapOf("email" to user.email, "password" to rawPassword)
            val loginResult = mockMvc.perform(
                post("/api/v0/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest))
            )
                .andExpect(status().isOk)
                .andReturn()

            val responseBody = objectMapper.readTree(loginResult.response.contentAsString)
            val refreshToken = responseBody["refresh_token"].asText()

            // 리프레시 토큰으로 새 토큰 발급
            val refreshRequest = mapOf("refresh_token" to refreshToken)

            mockMvc.perform(
                post("/api/v0/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(refreshRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.access_token").isNotEmpty)
                .andExpect(jsonPath("$.refresh_token").isNotEmpty)
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 실패 - 401")
        fun refreshFailInvalidToken() {
            val refreshRequest = mapOf("refresh_token" to "invalid.token.value")

            mockMvc.perform(
                post("/api/v0/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(refreshRequest))
            )
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    inner class Logout {

        @Test
        @DisplayName("인증된 사용자 로그아웃 성공 - 204")
        fun logoutSuccess() {
            // 먼저 로그인
            val loginRequest = mapOf("email" to user.email, "password" to rawPassword)
            mockMvc.perform(
                post("/api/v0/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest))
            )
                .andExpect(status().isOk)

            // 로그아웃 (V1 - JWT 필요)
            mockMvc.perform(
                post("/api/v1/auth/logout")
                    .withAuth(user)
            )
                .andExpect(status().isNoContent)
        }

        @Test
        @DisplayName("인증 없이 로그아웃 시도 - 401 또는 403")
        fun logoutFailWithoutAuth() {
            mockMvc.perform(
                post("/api/v1/auth/logout")
            )
                .andExpect(status().is4xxClientError)
        }
    }

    @Nested
    @DisplayName("POST /api/v0/auth/find-id")
    inner class FindId {

        @Test
        @DisplayName("닉네임으로 마스킹된 이메일 반환 성공")
        fun findIdSuccess() {
            val request = mapOf("nickname" to user.nickname)

            mockMvc.perform(
                post("/api/v0/auth/find-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.masked_email").value("au**@example.com"))
        }

        @Test
        @DisplayName("존재하지 않는 닉네임 - 404")
        fun findIdFailNotFound() {
            val request = mapOf("nickname" to "없는닉네임")

            mockMvc.perform(
                post("/api/v0/auth/find-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("POST /api/v0/auth/find-password & reset-password")
    inner class FindPassword {

        @Test
        @DisplayName("비밀번호 재설정 코드 발급 성공")
        fun findPasswordSuccess() {
            val request = mapOf("email" to user.email)

            mockMvc.perform(
                post("/api/v0/auth/find-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.reset_code").isNotEmpty)
                .andExpect(jsonPath("$.expires_in_minutes").value(10))
        }

        @Test
        @DisplayName("존재하지 않는 이메일 - 404")
        fun findPasswordFailNotFound() {
            val request = mapOf("email" to "nobody@example.com")

            mockMvc.perform(
                post("/api/v0/auth/find-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("코드로 비밀번호 재설정 성공 후 새 비밀번호로 로그인")
        fun resetPasswordSuccess() {
            // 코드 발급
            val findRequest = mapOf("email" to user.email)
            val findResult = mockMvc.perform(
                post("/api/v0/auth/find-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(findRequest))
            ).andReturn()

            val code = objectMapper.readTree(findResult.response.contentAsString)["reset_code"].asText()

            // 비밀번호 재설정
            val resetRequest = mapOf("email" to user.email, "reset_code" to code, "new_password" to "newPass123!")
            mockMvc.perform(
                post("/api/v0/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(resetRequest))
            )
                .andExpect(status().isNoContent)

            // 새 비밀번호로 로그인 성공
            val loginRequest = mapOf("email" to user.email, "password" to "newPass123!")
            mockMvc.perform(
                post("/api/v0/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest))
            )
                .andExpect(status().isOk)

            // 코드 사용 후 삭제 확인
            Assertions.assertEquals(0, passwordResetTokenRepository.count())
        }

        @Test
        @DisplayName("잘못된 코드로 재설정 실패 - 400")
        fun resetPasswordFailWrongCode() {
            val request = mapOf("email" to user.email, "reset_code" to "000000", "new_password" to "newPass123!")

            mockMvc.perform(
                post("/api/v0/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("만료된 재설정 코드로 재설정 실패 - 400")
        fun resetPasswordFailExpiredCode() {
            // 만료된 토큰을 직접 저장
            passwordResetTokenRepository.save(
                org.grr.bridgy.domain.auth.entity.PasswordResetToken(
                    email = user.email,
                    code = "123456",
                    expiryDate = java.time.LocalDateTime.now().minusMinutes(1)
                )
            )

            val request = mapOf("email" to user.email, "reset_code" to "123456", "new_password" to "newPass123!")
            mockMvc.perform(
                post("/api/v0/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isBadRequest)
        }
    }

    @org.junit.jupiter.api.Nested
    @DisplayName("로그아웃 후 토큰 무효화")
    inner class TokenInvalidation {

        @Test
        @DisplayName("로그아웃 후 기존 리프레시 토큰으로 갱신 실패 - 401")
        fun refreshFailAfterLogout() {
            // 로그인
            val loginRequest = mapOf("email" to user.email, "password" to rawPassword)
            val loginResult = mockMvc.perform(
                post("/api/v0/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(loginRequest))
            ).andReturn()

            val responseBody = objectMapper.readTree(loginResult.response.contentAsString)
            val refreshToken = responseBody["refresh_token"].asText()

            // 로그아웃
            mockMvc.perform(
                post("/api/v1/auth/logout").withAuth(user)
            ).andExpect(status().isNoContent)

            // 로그아웃 후 리프레시 시도 → 401
            val refreshRequest = mapOf("refresh_token" to refreshToken)
            mockMvc.perform(
                post("/api/v0/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(refreshRequest))
            )
                .andExpect(status().isUnauthorized)
        }
    }
}
