package org.grr.bridgy.domain.pet

import com.fasterxml.jackson.databind.ObjectMapper
import org.grr.bridgy.domain.pet.dto.CreatePetRequest
import org.grr.bridgy.domain.pet.dto.UpdatePetRequest
import org.grr.bridgy.domain.pet.entity.Pet
import org.grr.bridgy.domain.pet.entity.PetGender
import org.grr.bridgy.domain.pet.repository.PetRepository
import org.grr.bridgy.domain.user.entity.User
import org.grr.bridgy.domain.user.repository.UserRepository
import org.grr.bridgy.domain.like.entity.Like
import org.grr.bridgy.domain.like.repository.LikeRepository
import org.grr.bridgy.domain.comment.entity.Comment
import org.grr.bridgy.domain.comment.repository.CommentRepository
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
class PetIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var petRepository: PetRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var likeRepository: LikeRepository
    @Autowired lateinit var commentRepository: CommentRepository

    private fun createTestUser(email: String = "petowner@test.com", nickname: String = "펫주인"): User {
        return userRepository.save(User(email = email, password = "pass123", nickname = nickname))
    }

    private fun createTestPet(user: User, name: String = "뽀삐", species: String = "강아지"): Pet {
        return petRepository.save(
            Pet(user = user, name = name, species = species, breed = "포메라니안",
                age = 3, gender = PetGender.NEUTERED_MALE, weight = 3.2, bio = "귀여운 뽀삐")
        )
    }

    // ─── CRUD Tests ───

    @Test
    @Order(1)
    fun `반려동물 등록 성공`() {
        val user = createTestUser()
        val request = CreatePetRequest(
            userId = user.id, name = "뽀삐", species = "강아지",
            breed = "포메라니안", age = 3, gender = PetGender.MALE, weight = 3.2, bio = "귀여운 강아지"
        )

        mockMvc.perform(
            post("/api/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("뽀삐"))
            .andExpect(jsonPath("$.species").value("강아지"))
            .andExpect(jsonPath("$.breed").value("포메라니안"))
            .andExpect(jsonPath("$.owner_nickname").value("펫주인"))

        Assertions.assertEquals(1, petRepository.findByUserId(user.id).size)
    }

    @Test
    @Order(2)
    fun `반려동물 등록 실패 - 존재하지 않는 사용자`() {
        val request = CreatePetRequest(
            userId = 99999, name = "뽀삐", species = "강아지"
        )

        mockMvc.perform(
            post("/api/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(3)
    fun `반려동물 단건 조회`() {
        val user = createTestUser()
        val pet = createTestPet(user)

        mockMvc.perform(get("/api/pets/${pet.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("뽀삐"))
            .andExpect(jsonPath("$.species").value("강아지"))
    }

    @Test
    @Order(4)
    fun `다중 펫 - 한 사용자가 여러 마리 등록`() {
        val user = createTestUser()
        createTestPet(user, "뽀삐", "강아지")
        createTestPet(user, "나비", "고양이")
        createTestPet(user, "콩이", "토끼")

        mockMvc.perform(get("/api/pets/user/${user.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").exists())

        // DB 확인
        Assertions.assertEquals(3, petRepository.findByUserId(user.id).size)
    }

    @Test
    @Order(5)
    fun `전체 반려동물 조회`() {
        val user1 = createTestUser("user1@test.com", "유저1")
        val user2 = createTestUser("user2@test.com", "유저2")
        createTestPet(user1, "뽀삐", "강아지")
        createTestPet(user2, "나비", "고양이")

        mockMvc.perform(get("/api/pets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(6)
    fun `이름으로 검색`() {
        val user = createTestUser()
        createTestPet(user, "뽀삐", "강아지")
        createTestPet(user, "뽀미", "강아지")
        createTestPet(user, "나비", "고양이")

        mockMvc.perform(get("/api/pets/search").param("name", "뽀"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(7)
    fun `종류별 조회`() {
        val user = createTestUser()
        createTestPet(user, "뽀삐", "강아지")
        createTestPet(user, "코코", "강아지")
        createTestPet(user, "나비", "고양이")

        mockMvc.perform(get("/api/pets/species/강아지"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(8)
    fun `반려동물 수정`() {
        val user = createTestUser()
        val pet = createTestPet(user)

        val updateRequest = UpdatePetRequest(name = "뽀삐2", weight = 3.5, bio = "수정된 소개")

        mockMvc.perform(
            put("/api/pets/${pet.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("뽀삐2"))
            .andExpect(jsonPath("$.weight").value(3.5))
            .andExpect(jsonPath("$.bio").value("수정된 소개"))

        val updated = petRepository.findById(pet.id).get()
        Assertions.assertEquals("뽀삐2", updated.name)
    }

    @Test
    @Order(9)
    fun `반려동물 삭제`() {
        val user = createTestUser()
        val pet = createTestPet(user)

        mockMvc.perform(delete("/api/pets/${pet.id}"))
            .andExpect(status().isNoContent)

        Assertions.assertFalse(petRepository.existsById(pet.id))
    }

    // ─── Dashboard / Feed Tests ───

    @Test
    @Order(10)
    fun `대시보드 - 인기순 피드 (좋아요 많은 순)`() {
        val owner = createTestUser()
        val liker1 = createTestUser("liker1@test.com", "좋아요유저1")
        val liker2 = createTestUser("liker2@test.com", "좋아요유저2")
        val liker3 = createTestUser("liker3@test.com", "좋아요유저3")

        val pet1 = createTestPet(owner, "뽀삐", "강아지")  // 3 likes
        val pet2 = createTestPet(owner, "나비", "고양이")   // 1 like
        val pet3 = createTestPet(owner, "코코", "강아지")   // 2 likes

        // pet1: 3 likes
        likeRepository.save(Like(pet = pet1, user = liker1))
        likeRepository.save(Like(pet = pet1, user = liker2))
        likeRepository.save(Like(pet = pet1, user = liker3))
        // pet3: 2 likes
        likeRepository.save(Like(pet = pet3, user = liker1))
        likeRepository.save(Like(pet = pet3, user = liker2))
        // pet2: 1 like
        likeRepository.save(Like(pet = pet2, user = liker1))

        mockMvc.perform(get("/api/pets/feed/popular"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("뽀삐"))
            .andExpect(jsonPath("$[0].like_count").value(3))
            .andExpect(jsonPath("$[1].name").value("코코"))
            .andExpect(jsonPath("$[1].like_count").value(2))
            .andExpect(jsonPath("$[2].name").value("나비"))
            .andExpect(jsonPath("$[2].like_count").value(1))
    }

    @Test
    @Order(11)
    fun `대시보드 - 인기순 피드에서 현재 사용자 좋아요 여부`() {
        val owner = createTestUser()
        val viewer = createTestUser("viewer@test.com", "뷰어")

        val pet = createTestPet(owner, "뽀삐", "강아지")
        likeRepository.save(Like(pet = pet, user = viewer))

        mockMvc.perform(get("/api/pets/feed/popular").param("userId", viewer.id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].is_liked").value(true))

        // 좋아요 안 한 유저가 조회
        val other = createTestUser("other@test.com", "다른유저")
        mockMvc.perform(get("/api/pets/feed/popular").param("userId", other.id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].is_liked").value(false))
    }

    @Test
    @Order(12)
    fun `대시보드 - 최신순 피드`() {
        val user = createTestUser()
        createTestPet(user, "첫째", "강아지")
        createTestPet(user, "둘째", "고양이")
        createTestPet(user, "셋째", "토끼")

        mockMvc.perform(get("/api/pets/feed/recent"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("셋째")) // 가장 최근
    }

    @Test
    @Order(13)
    fun `펫 상세 조회 - 좋아요, 댓글 수 포함`() {
        val owner = createTestUser()
        val commenter = createTestUser("commenter@test.com", "댓글유저")
        val liker = createTestUser("liker@test.com", "좋아요유저")

        val pet = createTestPet(owner, "뽀삐", "강아지")
        likeRepository.save(Like(pet = pet, user = liker))
        commentRepository.save(Comment(pet = pet, user = commenter, content = "귀여워요!"))
        commentRepository.save(Comment(pet = pet, user = owner, content = "감사합니다!"))

        mockMvc.perform(
            get("/api/pets/${pet.id}/detail")
                .param("userId", liker.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("뽀삐"))
            .andExpect(jsonPath("$.like_count").value(1))
            .andExpect(jsonPath("$.comment_count").value(2))
            .andExpect(jsonPath("$.is_liked").value(true))
    }

    @Test
    @Order(14)
    fun `트랜잭션 롤백 검증 - 이전 테스트 데이터가 없어야 함`() {
        // 모든 테스트는 @Transactional로 롤백되므로 DB가 깨끗해야 함
        Assertions.assertEquals(0, petRepository.count())
        Assertions.assertEquals(0, userRepository.count())
    }
}
