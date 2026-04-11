package org.grr.bridgy.domain.user

import org.grr.bridgy.common.BaseIntegrationTest
import org.grr.bridgy.domain.user.dto.SignUpRequest
import org.grr.bridgy.domain.user.dto.UpdateUserRequest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@TestMethodOrder(OrderAnnotation::class)
class UserIntegrationTest : BaseIntegrationTest() {

    // ─── V0 (공개) API 테스트 ───

    @Test
    @Order(1)
    fun `V0 회원가입 성공`() {
        val request = SignUpRequest(
            email = "newuser@example.com",
            password = "password123",
            nickname = "새유저"
        )

        mockMvc.perform(
            post("/api/v0/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.nickname").value("새유저"))
            .andExpect(jsonPath("$.id").exists())

        val savedUser = userRepository.findByEmail("newuser@example.com")
        Assertions.assertTrue(savedUser.isPresent)
        Assertions.assertEquals("새유저", savedUser.get().nickname)
    }

    @Test
    @Order(2)
    fun `V0 회원가입 실패 - 중복 이메일`() {
        createTestUser(email = "duplicate@example.com")

        val request = SignUpRequest(
            email = "duplicate@example.com",
            password = "password123",
            nickname = "다른닉네임"
        )

        mockMvc.perform(
            post("/api/v0/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    @Order(3)
    fun `V0 회원가입 실패 - 중복 닉네임`() {
        createTestUser(nickname = "중복닉네임")

        val request = SignUpRequest(
            email = "another@example.com",
            password = "password123",
            nickname = "중복닉네임"
        )

        mockMvc.perform(
            post("/api/v0/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isConflict)
    }

    // ─── V1 (인증) API 테스트 ───

    @Test
    @Order(4)
    fun `V1 사용자 조회 성공`() {
        val user = createTestUser()

        mockMvc.perform(
            get("/api/v1/users/${user.id}").withAuth(user)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.nickname").value("테스트유저"))
    }

    @Test
    @Order(5)
    fun `V1 사용자 조회 실패 - 인증 없음`() {
        val user = createTestUser()

        mockMvc.perform(get("/api/v1/users/${user.id}"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(6)
    fun `V1 사용자 조회 실패 - 존재하지 않는 ID`() {
        val user = createTestUser()

        mockMvc.perform(
            get("/api/v1/users/99999").withAuth(user)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(7)
    fun `V1 사용자 수정 성공`() {
        val user = createTestUser()
        val updateRequest = UpdateUserRequest(
            nickname = "수정된닉네임",
            bio = "수정된 소개글"
        )

        mockMvc.perform(
            put("/api/v1/users/${user.id}")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("수정된닉네임"))
            .andExpect(jsonPath("$.bio").value("수정된 소개글"))

        val updatedUser = userRepository.findById(user.id).get()
        Assertions.assertEquals("수정된닉네임", updatedUser.nickname)
    }

    @Test
    @Order(8)
    fun `V1 사용자 삭제 성공`() {
        val user = createTestUser()

        mockMvc.perform(
            delete("/api/v1/users/${user.id}").withAuth(user)
        )
            .andExpect(status().isNoContent)

        Assertions.assertFalse(userRepository.existsById(user.id))
    }

    @Test
    @Order(9)
    fun `V1 사용자 삭제 실패 - 존재하지 않는 ID`() {
        val user = createTestUser()

        mockMvc.perform(
            delete("/api/v1/users/99999").withAuth(user)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(10)
    fun `V0 회원가입 실패 - 비속어 포함 닉네임`() {
        val request = SignUpRequest(
            email = "badnick@example.com",
            password = "password123",
            nickname = "존나멋진유저"
        )

        mockMvc.perform(
            post("/api/v0/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)

        Assertions.assertFalse(userRepository.findByEmail("badnick@example.com").isPresent)
    }

    @Test
    @Order(11)
    fun `V1 사용자 수정 실패 - 비속어 포함 닉네임`() {
        val user = createTestUser()
        val updateRequest = UpdateUserRequest(nickname = "씨발유저")

        mockMvc.perform(
            put("/api/v1/users/${user.id}")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest))
        )
            .andExpect(status().isBadRequest)

        val unchanged = userRepository.findById(user.id).get()
        Assertions.assertEquals("테스트유저", unchanged.nickname)
    }

    @Test
    @Order(12)
    fun `트랜잭션 롤백 검증 - 이전 테스트 데이터가 없어야 함`() {
        Assertions.assertEquals(0, userRepository.count())
    }
}
