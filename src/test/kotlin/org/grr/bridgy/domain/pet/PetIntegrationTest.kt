package org.grr.bridgy.domain.pet

import org.grr.bridgy.common.BaseIntegrationTest
import org.grr.bridgy.domain.comment.entity.Comment
import org.grr.bridgy.domain.comment.repository.CommentRepository
import org.grr.bridgy.domain.like.entity.Like
import org.grr.bridgy.domain.like.repository.LikeRepository
import org.grr.bridgy.domain.pet.dto.CreatePetRequest
import org.grr.bridgy.domain.pet.dto.UpdatePetRequest
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
class PetIntegrationTest : BaseIntegrationTest() {

    @Autowired lateinit var petRepository: PetRepository
    @Autowired lateinit var likeRepository: LikeRepository
    @Autowired lateinit var commentRepository: CommentRepository

    private fun createPetOwner(email: String = "petowner@test.com", nickname: String = "펫주인"): User {
        return createTestUser(email = email, nickname = nickname)
    }

    private fun createTestPet(user: User, name: String = "뽀삐", species: String = "강아지"): Pet {
        return petRepository.save(
            Pet(user = user, name = name, species = species, breed = "포메라니안",
                age = 3, gender = PetGender.NEUTERED_MALE, weight = 3.2, bio = "귀여운 뽀삐")
        )
    }

    // ─── V0 (공개) API 테스트 ───

    @Test
    @Order(1)
    fun `V0 반려동물 단건 조회`() {
        val user = createPetOwner()
        val pet = createTestPet(user)

        mockMvc.perform(get("/api/v0/pets/${pet.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("뽀삐"))
            .andExpect(jsonPath("$.species").value("강아지"))
    }

    @Test
    @Order(2)
    fun `V0 전체 반려동물 조회`() {
        val user1 = createPetOwner("user1@test.com", "유저1")
        val user2 = createPetOwner("user2@test.com", "유저2")
        createTestPet(user1, "뽀삐", "강아지")
        createTestPet(user2, "나비", "고양이")

        mockMvc.perform(get("/api/v0/pets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(3)
    fun `V0 이름으로 검색`() {
        val user = createPetOwner()
        createTestPet(user, "뽀삐", "강아지")
        createTestPet(user, "뽀미", "강아지")
        createTestPet(user, "나비", "고양이")

        mockMvc.perform(get("/api/v0/pets/search").param("name", "뽀"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(4)
    fun `V0 종류별 조회`() {
        val user = createPetOwner()
        createTestPet(user, "뽀삐", "강아지")
        createTestPet(user, "코코", "강아지")
        createTestPet(user, "나비", "고양이")

        mockMvc.perform(get("/api/v0/pets/species/강아지"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(5)
    fun `V0 대시보드 - 인기순 피드`() {
        val owner = createPetOwner()
        val liker1 = createTestUser("liker1@test.com", nickname = "좋아요유저1")
        val liker2 = createTestUser("liker2@test.com", nickname = "좋아요유저2")
        val liker3 = createTestUser("liker3@test.com", nickname = "좋아요유저3")

        val pet1 = createTestPet(owner, "뽀삐", "강아지")
        val pet2 = createTestPet(owner, "나비", "고양이")
        val pet3 = createTestPet(owner, "코코", "강아지")

        likeRepository.save(Like(pet = pet1, user = liker1))
        likeRepository.save(Like(pet = pet1, user = liker2))
        likeRepository.save(Like(pet = pet1, user = liker3))
        likeRepository.save(Like(pet = pet3, user = liker1))
        likeRepository.save(Like(pet = pet3, user = liker2))
        likeRepository.save(Like(pet = pet2, user = liker1))

        mockMvc.perform(get("/api/v0/pets/feed/popular"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("뽀삐"))
            .andExpect(jsonPath("$[0].like_count").value(3))
    }

    @Test
    @Order(6)
    fun `V0 최신순 피드`() {
        val user = createPetOwner()
        createTestPet(user, "첫째", "강아지")
        createTestPet(user, "둘째", "고양이")
        createTestPet(user, "셋째", "토끼")

        mockMvc.perform(get("/api/v0/pets/feed/recent"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }

    // ─── V1 (인증) API 테스트 ───

    @Test
    @Order(7)
    fun `V1 반려동물 등록 성공`() {
        val user = createPetOwner()
        val request = CreatePetRequest(
            userId = user.id, name = "뽀삐", species = "강아지",
            breed = "포메라니안", age = 3, gender = PetGender.MALE, weight = 3.2, bio = "귀여운 강아지"
        )

        mockMvc.perform(
            post("/api/v1/pets")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("뽀삐"))
            .andExpect(jsonPath("$.species").value("강아지"))

        Assertions.assertEquals(1, petRepository.findByUserId(user.id).size)
    }

    @Test
    @Order(8)
    fun `V1 반려동물 등록 실패 - 인증 없음`() {
        val request = CreatePetRequest(
            userId = 1, name = "뽀삐", species = "강아지"
        )

        mockMvc.perform(
            post("/api/v1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isUnauthorized.or(status().isForbidden))
    }

    @Test
    @Order(9)
    fun `V1 반려동물 수정`() {
        val user = createPetOwner()
        val pet = createTestPet(user)
        val updateRequest = UpdatePetRequest(name = "뽀삐2", weight = 3.5, bio = "수정된 소개")

        mockMvc.perform(
            put("/api/v1/pets/${pet.id}")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("뽀삐2"))

        val updated = petRepository.findById(pet.id).get()
        Assertions.assertEquals("뽀삐2", updated.name)
    }

    @Test
    @Order(10)
    fun `V1 반려동물 삭제`() {
        val user = createPetOwner()
        val pet = createTestPet(user)

        mockMvc.perform(
            delete("/api/v1/pets/${pet.id}").withAuth(user)
        )
            .andExpect(status().isNoContent)

        Assertions.assertFalse(petRepository.existsById(pet.id))
    }

    @Test
    @Order(11)
    fun `트랜잭션 롤백 검증`() {
        Assertions.assertEquals(0, petRepository.count())
        Assertions.assertEquals(0, userRepository.count())
    }
}
