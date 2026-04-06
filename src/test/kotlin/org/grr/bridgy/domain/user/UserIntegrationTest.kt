package org.grr.bridgy.domain.user

import com.fasterxml.jackson.databind.ObjectMapper
import org.grr.bridgy.domain.user.dto.SignUpRequest
import org.grr.bridgy.domain.user.dto.UpdateUserRequest
import org.grr.bridgy.domain.user.entity.User
import org.grr.bridgy.domain.user.repository.UserRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation::class)
class UserIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    private fun createTestUser(
        email: String = "test@example.com",
        password: String = "password123",
        nickname: String = "테스트유저"
    ): User {
        return userRepository.save(
            User(
                email = email,
                password = password,
                nickname = nickname,
                bio = "테스트 소개글"
            )
        )
    }

    @Test
    @Order(1)
    fun `회원가입 성공`() {
        val request = SignUpRequest(
            email = "newuser@example.com",
            password = "password123",
            nickname = "새유저"
        )

        mockMvc.perform(
            post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.nickname").value("새유저"))
            .andExpect(jsonPath("$.id").exists())

        // DB에 실제로 저장되었는지 확인
        val savedUser = userRepository.findByEmail("newuser@example.com")
        Assertions.assertTrue(savedUser.isPresent)
        Assertions.assertEquals("새유저", savedUser.get().nickname)
    }

    @Test
    @Order(2)
    fun `회원가입 실패 - 중복 이메일`() {
        createTestUser(email = "duplicate@example.com")

        val request = SignUpRequest(
            email = "duplicate@example.com",
            password = "password123",
            nickname = "다른닉네임"
        )

        mockMvc.perform(
            post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(3)
    fun `회원가입 실패 - 중복 닉네임`() {
        createTestUser(nickname = "중복닉네임")

        val request = SignUpRequest(
            email = "another@example.com",
            password = "password123",
            nickname = "중복닉네임"
        )

        mockMvc.perform(
            post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(4)
    fun `사용자 조회 성공`() {
        val user = createTestUser()

        mockMvc.perform(get("/api/users/${user.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.nickname").value("테스트유저"))
            .andExpect(jsonPath("$.bio").value("테스트 소개글"))
    }

    @Test
    @Order(5)
    fun `사용자 조회 실패 - 존재하지 않는 ID`() {
        mockMvc.perform(get("/api/users/99999"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(6)
    fun `사용자 수정 성공`() {
        val user = createTestUser()

        val updateRequest = UpdateUserRequest(
            nickname = "수정된닉네임",
            bio = "수정된 소개글"
        )

        mockMvc.perform(
            put("/api/users/${user.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("수정된닉네임"))
            .andExpect(jsonPath("$.bio").value("수정된 소개글"))

        // DB에서 실제로 수정되었는지 확인
        val updatedUser = userRepository.findById(user.id).get()
        Assertions.assertEquals("수정된닉네임", updatedUser.nickname)
    }

    @Test
    @Order(7)
    fun `사용자 삭제 성공`() {
        val user = createTestUser()

        mockMvc.perform(delete("/api/users/${user.id}"))
            .andExpect(status().isNoContent)

        // DB에서 실제로 삭제되었는지 확인
        Assertions.assertFalse(userRepository.existsById(user.id))
    }

    @Test
    @Order(8)
    fun `사용자 삭제 실패 - 존재하지 않는 ID`() {
        mockMvc.perform(delete("/api/users/99999"))
            .andExpect(status().is4xxClientError)
    }
}
