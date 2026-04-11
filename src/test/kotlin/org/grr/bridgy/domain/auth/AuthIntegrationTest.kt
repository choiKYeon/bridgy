package org.grr.bridgy.domain.auth

import org.grr.bridgy.common.BaseIntegrationTest
import org.grr.bridgy.domain.user.entity.User
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

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

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
}
