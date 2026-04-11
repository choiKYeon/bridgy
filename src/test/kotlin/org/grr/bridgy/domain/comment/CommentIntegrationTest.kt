package org.grr.bridgy.domain.comment

import org.grr.bridgy.common.BaseIntegrationTest
import org.grr.bridgy.common.config.FreeTierLimits
import org.grr.bridgy.domain.comment.dto.CreateCommentRequest
import org.grr.bridgy.domain.comment.dto.UpdateCommentRequest
import org.grr.bridgy.domain.comment.entity.Comment
import org.grr.bridgy.domain.comment.repository.CommentRepository
import org.grr.bridgy.domain.pet.entity.Pet
import org.grr.bridgy.domain.pet.entity.PetGender
import org.grr.bridgy.domain.pet.repository.PetRepository
import org.grr.bridgy.domain.user.entity.User
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@TestMethodOrder(OrderAnnotation::class)
class CommentIntegrationTest : BaseIntegrationTest() {

    @Autowired lateinit var commentRepository: CommentRepository
    @Autowired lateinit var petRepository: PetRepository

    private fun setup(): Triple<User, User, Pet> {
        val owner = createTestUser(email = "owner@test.com", nickname = "펫주인")
        val commenter = createTestUser(email = "commenter@test.com", nickname = "댓글유저")
        val pet = petRepository.save(Pet(user = owner, name = "뽀삐", species = "강아지", gender = PetGender.MALE))
        return Triple(owner, commenter, pet)
    }

    // ─── V0 (공개) API 테스트 ───

    @Test
    @Order(1)
    fun `V0 펫별 댓글 조회`() {
        val (owner, commenter, pet) = setup()
        commentRepository.save(Comment(pet = pet, user = commenter, content = "귀여워요"))
        commentRepository.save(Comment(pet = pet, user = owner, content = "감사합니다"))

        mockMvc.perform(get("/api/v0/comments/pet/${pet.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(2)
    fun `V0 댓글 수 조회`() {
        val (owner, commenter, pet) = setup()
        commentRepository.save(Comment(pet = pet, user = commenter, content = "댓글1"))
        commentRepository.save(Comment(pet = pet, user = owner, content = "댓글2"))
        commentRepository.save(Comment(pet = pet, user = commenter, content = "댓글3"))

        mockMvc.perform(get("/api/v0/comments/pet/${pet.id}/count"))
            .andExpect(status().isOk)
            .andExpect(content().string("3"))
    }

    // ─── V1 (인증) API 테스트 ───

    @Test
    @Order(3)
    fun `V1 댓글 작성 성공`() {
        val (_, commenter, pet) = setup()
        val request = CreateCommentRequest(
            petId = pet.id, userId = commenter.id, content = "너무 귀여워요!"
        )

        mockMvc.perform(
            post("/api/v1/comments")
                .withAuth(commenter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.content").value("너무 귀여워요!"))
            .andExpect(jsonPath("$.user_nickname").value("댓글유저"))

        Assertions.assertEquals(1, commentRepository.countByPetId(pet.id))
    }

    @Test
    @Order(4)
    fun `V1 댓글 작성 실패 - 글자수 초과`() {
        val (_, commenter, pet) = setup()
        val longContent = "가".repeat(FreeTierLimits.MAX_COMMENT_LENGTH + 1)
        val request = CreateCommentRequest(
            petId = pet.id, userId = commenter.id, content = longContent
        )

        mockMvc.perform(
            post("/api/v1/comments")
                .withAuth(commenter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)

        Assertions.assertEquals(0, commentRepository.countByPetId(pet.id))
    }

    @Test
    @Order(5)
    fun `V1 댓글 작성 실패 - 일일 댓글 수 초과`() {
        val (_, commenter, pet) = setup()

        // 일일 최대치까지 댓글 저장
        repeat(FreeTierLimits.MAX_COMMENTS_PER_USER_PER_DAY) { i ->
            commentRepository.save(Comment(pet = pet, user = commenter, content = "댓글$i"))
        }

        // 하나 더 작성 시도 → 실패
        val request = CreateCommentRequest(
            petId = pet.id, userId = commenter.id, content = "한 개 더"
        )

        mockMvc.perform(
            post("/api/v1/comments")
                .withAuth(commenter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isTooManyRequests)
    }

    @Test
    @Order(6)
    fun `V1 댓글 작성 실패 - 존재하지 않는 펫`() {
        val (_, commenter, _) = setup()
        val request = CreateCommentRequest(petId = 99999, userId = commenter.id, content = "테스트")

        mockMvc.perform(
            post("/api/v1/comments")
                .withAuth(commenter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(7)
    fun `V1 댓글 작성 실패 - 인증 없음`() {
        val request = CreateCommentRequest(petId = 1, userId = 1, content = "테스트")

        mockMvc.perform(
            post("/api/v1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(8)
    fun `V1 댓글 수정 성공`() {
        val (_, commenter, pet) = setup()
        val comment = commentRepository.save(Comment(pet = pet, user = commenter, content = "원래 댓글"))
        val updateRequest = UpdateCommentRequest(content = "수정된 댓글")

        mockMvc.perform(
            put("/api/v1/comments/${comment.id}")
                .withAuth(commenter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("수정된 댓글"))

        val updated = commentRepository.findById(comment.id).get()
        Assertions.assertEquals("수정된 댓글", updated.content)
    }

    @Test
    @Order(9)
    fun `V1 댓글 수정 실패 - 글자수 초과`() {
        val (_, commenter, pet) = setup()
        val comment = commentRepository.save(Comment(pet = pet, user = commenter, content = "원래 댓글"))
        val longContent = "나".repeat(FreeTierLimits.MAX_COMMENT_LENGTH + 1)
        val updateRequest = UpdateCommentRequest(content = longContent)

        mockMvc.perform(
            put("/api/v1/comments/${comment.id}")
                .withAuth(commenter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest))
        )
            .andExpect(status().isBadRequest)

        // 원본 유지 확인
        val unchanged = commentRepository.findById(comment.id).get()
        Assertions.assertEquals("원래 댓글", unchanged.content)
    }

    @Test
    @Order(10)
    fun `V1 댓글 수정 실패 - 존재하지 않는 댓글 ID`() {
        val (_, commenter, _) = setup()
        val updateRequest = UpdateCommentRequest(content = "수정 시도")

        mockMvc.perform(
            put("/api/v1/comments/99999")
                .withAuth(commenter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(11)
    fun `V1 댓글 삭제 성공`() {
        val (_, commenter, pet) = setup()
        val comment = commentRepository.save(Comment(pet = pet, user = commenter, content = "삭제될 댓글"))

        mockMvc.perform(
            delete("/api/v1/comments/${comment.id}").withAuth(commenter)
        )
            .andExpect(status().isNoContent)

        Assertions.assertFalse(commentRepository.existsById(comment.id))
    }

    @Test
    @Order(12)
    fun `V1 댓글 삭제 실패 - 존재하지 않는 댓글 ID`() {
        val (_, commenter, _) = setup()

        mockMvc.perform(
            delete("/api/v1/comments/99999").withAuth(commenter)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(13)
    fun `V1 댓글 삭제 실패 - 인증 없음`() {
        val (_, commenter, pet) = setup()
        val comment = commentRepository.save(Comment(pet = pet, user = commenter, content = "댓글"))

        mockMvc.perform(
            delete("/api/v1/comments/${comment.id}")
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(14)
    fun `V1 댓글 작성 실패 - 비속어 포함`() {
        val (_, commenter, pet) = setup()
        val request = CreateCommentRequest(petId = pet.id, userId = commenter.id, content = "진짜 씨발 귀엽다")

        mockMvc.perform(
            post("/api/v1/comments")
                .withAuth(commenter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)

        Assertions.assertEquals(0, commentRepository.countByPetId(pet.id))
    }

    @Test
    @Order(15)
    fun `V1 댓글 수정 실패 - 비속어 포함`() {
        val (_, commenter, pet) = setup()
        val comment = commentRepository.save(Comment(pet = pet, user = commenter, content = "좋은 댓글"))
        val updateRequest = UpdateCommentRequest(content = "병신같은 댓글로 수정")

        mockMvc.perform(
            put("/api/v1/comments/${comment.id}")
                .withAuth(commenter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest))
        )
            .andExpect(status().isBadRequest)

        val unchanged = commentRepository.findById(comment.id).get()
        Assertions.assertEquals("좋은 댓글", unchanged.content)
    }

    @Test
    @Order(16)
    fun `트랜잭션 롤백 검증`() {
        Assertions.assertEquals(0, commentRepository.count())
    }
}
