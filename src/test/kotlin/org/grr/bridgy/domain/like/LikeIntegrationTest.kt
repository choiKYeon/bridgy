package org.grr.bridgy.domain.like

import org.grr.bridgy.common.BaseIntegrationTest
import org.grr.bridgy.domain.like.dto.LikeRequest
import org.grr.bridgy.domain.like.entity.Like
import org.grr.bridgy.domain.like.repository.LikeRepository
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
class LikeIntegrationTest : BaseIntegrationTest() {

    @Autowired lateinit var likeRepository: LikeRepository
    @Autowired lateinit var petRepository: PetRepository

    private fun setup(): Triple<User, User, Pet> {
        val owner = createTestUser(email = "owner@test.com", nickname = "펫주인")
        val liker = createTestUser(email = "liker@test.com", nickname = "좋아요유저")
        val pet = petRepository.save(Pet(user = owner, name = "뽀삐", species = "강아지", gender = PetGender.MALE))
        return Triple(owner, liker, pet)
    }

    // ─── V0 (공개) API 테스트 ───

    @Test
    @Order(1)
    fun `V0 좋아요 상태 조회 - 좋아요 O`() {
        val (_, liker, pet) = setup()
        likeRepository.save(Like(pet = pet, user = liker))

        mockMvc.perform(get("/api/v0/likes/pet/${pet.id}/user/${liker.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.is_liked").value(true))
            .andExpect(jsonPath("$.like_count").value(1))
    }

    @Test
    @Order(2)
    fun `V0 좋아요 상태 조회 - 좋아요 X`() {
        val (owner, _, pet) = setup()

        mockMvc.perform(get("/api/v0/likes/pet/${pet.id}/user/${owner.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.is_liked").value(false))
            .andExpect(jsonPath("$.like_count").value(0))
    }

    @Test
    @Order(3)
    fun `V0 좋아요 수 조회`() {
        val (owner, liker, pet) = setup()
        val liker2 = createTestUser(email = "liker2@test.com", nickname = "좋아요2")
        likeRepository.save(Like(pet = pet, user = liker))
        likeRepository.save(Like(pet = pet, user = liker2))
        likeRepository.save(Like(pet = pet, user = owner))

        mockMvc.perform(get("/api/v0/likes/pet/${pet.id}/count"))
            .andExpect(status().isOk)
            .andExpect(content().string("3"))
    }

    // ─── V1 (인증) API 테스트 ───

    @Test
    @Order(4)
    fun `V1 좋아요 토글 - 좋아요 누르기`() {
        val (_, liker, pet) = setup()
        val request = LikeRequest(petId = pet.id, userId = liker.id)

        mockMvc.perform(
            post("/api/v1/likes")
                .withAuth(liker)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pet_id").value(pet.id))
            .andExpect(jsonPath("$.like_count").value(1))
            .andExpect(jsonPath("$.is_liked").value(true))

        Assertions.assertTrue(likeRepository.existsByPetIdAndUserId(pet.id, liker.id))
    }

    @Test
    @Order(5)
    fun `V1 좋아요 토글 - 좋아요 취소`() {
        val (_, liker, pet) = setup()
        likeRepository.save(Like(pet = pet, user = liker))
        val request = LikeRequest(petId = pet.id, userId = liker.id)

        mockMvc.perform(
            post("/api/v1/likes")
                .withAuth(liker)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.like_count").value(0))
            .andExpect(jsonPath("$.is_liked").value(false))

        Assertions.assertFalse(likeRepository.existsByPetIdAndUserId(pet.id, liker.id))
    }

    @Test
    @Order(6)
    fun `V1 좋아요 토글 실패 - 존재하지 않는 펫`() {
        val (_, liker, _) = setup()
        val request = LikeRequest(petId = 99999, userId = liker.id)

        mockMvc.perform(
            post("/api/v1/likes")
                .withAuth(liker)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(7)
    fun `V1 좋아요 토글 실패 - 인증 없음`() {
        val request = LikeRequest(petId = 1, userId = 1)

        mockMvc.perform(
            post("/api/v1/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(8)
    fun `여러 펫에 좋아요 - 각각 독립적 카운트`() {
        val (owner, liker, pet1) = setup()
        val pet2 = petRepository.save(Pet(user = owner, name = "나비", species = "고양이", gender = PetGender.FEMALE))

        likeRepository.save(Like(pet = pet1, user = liker))
        likeRepository.save(Like(pet = pet1, user = owner))
        likeRepository.save(Like(pet = pet2, user = liker))

        Assertions.assertEquals(2, likeRepository.countByPetId(pet1.id))
        Assertions.assertEquals(1, likeRepository.countByPetId(pet2.id))
    }

    @Test
    @Order(9)
    fun `트랜잭션 롤백 검증`() {
        Assertions.assertEquals(0, likeRepository.count())
        Assertions.assertEquals(0, petRepository.count())
    }
}
