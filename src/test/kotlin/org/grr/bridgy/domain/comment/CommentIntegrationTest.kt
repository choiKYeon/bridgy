package org.grr.bridgy.domain.comment

import com.fasterxml.jackson.databind.ObjectMapper
import org.grr.bridgy.domain.comment.dto.CreateCommentRequest
import org.grr.bridgy.domain.comment.dto.UpdateCommentRequest
import org.grr.bridgy.domain.comment.entity.Comment
import org.grr.bridgy.domain.comment.repository.CommentRepository
import org.grr.bridgy.domain.pet.entity.Pet
import org.grr.bridgy.domain.pet.entity.PetGender
import org.grr.bridgy.domain.pet.repository.PetRepository
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
class CommentIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var commentRepository: CommentRepository
    @Autowired lateinit var petRepository: PetRepository
    @Autowired lateinit var userRepository: UserRepository

    private fun setup(): Triple<User, User, Pet> {
        val owner = userRepository.save(User(email = "owner@test.com", password = "pass", nickname = "펫주인"))
        val commenter = userRepository.save(User(email = "commenter@test.com", password = "pass", nickname = "댓글유저"))
        val pet = petRepository.save(Pet(user = owner, name = "뽀삐", species = "강아지", gender = PetGender.MALE))
        return Triple(owner, commenter, pet)
    }

    @Test
    @Order(1)
    fun `댓글 작성 성공`() {
        val (_, commenter, pet) = setup()
        val request = CreateCommentRequest(
            petId = pet.id, userId = commenter.id, content = "너무 귀여워요!"
        )

        mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.content").value("너무 귀여워요!"))
            .andExpect(jsonPath("$.user_nickname").value("댓글유저"))
            .andExpect(jsonPath("$.pet_id").value(pet.id))

        Assertions.assertEquals(1, commentRepository.countByPetId(pet.id))
    }

    @Test
    @Order(2)
    fun `댓글 작성 실패 - 존재하지 않는 펫`() {
        val (_, commenter, _) = setup()
        val request = CreateCommentRequest(petId = 99999, userId = commenter.id, content = "테스트")

        mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(3)
    fun `펫별 댓글 조회`() {
        val (owner, commenter, pet) = setup()
        commentRepository.save(Comment(pet = pet, user = commenter, content = "귀여워요"))
        commentRepository.save(Comment(pet = pet, user = owner, content = "감사합니다"))

        mockMvc.perform(get("/api/comments/pet/${pet.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(4)
    fun `댓글 수 조회`() {
        val (owner, commenter, pet) = setup()
        commentRepository.save(Comment(pet = pet, user = commenter, content = "댓글1"))
        commentRepository.save(Comment(pet = pet, user = owner, content = "댓글2"))
        commentRepository.save(Comment(pet = pet, user = commenter, content = "댓글3"))

        mockMvc.perform(get("/api/comments/pet/${pet.id}/count"))
            .andExpect(status().isOk)
            .andExpect(content().string("3"))
    }

    @Test
    @Order(5)
    fun `댓글 수정 성공`() {
        val (_, commenter, pet) = setup()
        val comment = commentRepository.save(Comment(pet = pet, user = commenter, content = "원래 댓글"))

        val updateRequest = UpdateCommentRequest(content = "수정된 댓글")

        mockMvc.perform(
            put("/api/comments/${comment.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("수정된 댓글"))

        val updated = commentRepository.findById(comment.id).get()
        Assertions.assertEquals("수정된 댓글", updated.content)
    }

    @Test
    @Order(6)
    fun `댓글 삭제 성공`() {
        val (_, commenter, pet) = setup()
        val comment = commentRepository.save(Comment(pet = pet, user = commenter, content = "삭제될 댓글"))

        mockMvc.perform(delete("/api/comments/${comment.id}"))
            .andExpect(status().isNoContent)

        Assertions.assertFalse(commentRepository.existsById(comment.id))
    }

    @Test
    @Order(7)
    fun `트랜잭션 롤백 검증`() {
        Assertions.assertEquals(0, commentRepository.count())
    }
}
