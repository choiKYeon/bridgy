package org.grr.bridgy.domain.decoration

import org.grr.bridgy.common.BaseIntegrationTest
import org.grr.bridgy.domain.decoration.dto.EquipDecorationRequest
import org.grr.bridgy.domain.decoration.entity.Decoration
import org.grr.bridgy.domain.decoration.entity.DecorationTier
import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.grr.bridgy.domain.decoration.repository.DecorationRepository
import org.grr.bridgy.domain.decoration.repository.PetDecorationRepository
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
class DecorationIntegrationTest : BaseIntegrationTest() {

    @Autowired lateinit var decorationRepository: DecorationRepository
    @Autowired lateinit var petDecorationRepository: PetDecorationRepository
    @Autowired lateinit var petRepository: PetRepository

    private fun createUserAndPet(): Pair<User, Pet> {
        val user = createTestUser(email = "deco@test.com", nickname = "데코유저")
        val pet = petRepository.save(Pet(user = user, name = "뽀삐", species = "강아지", gender = PetGender.MALE))
        return Pair(user, pet)
    }

    private fun createBasicBorder(name: String = "테스트 테두리"): Decoration {
        return decorationRepository.save(
            Decoration(name = name, type = DecorationType.BORDER, tier = DecorationTier.BASIC,
                description = "테스트용", imageUrl = "/test/border.png", isDefault = false)
        )
    }

    private fun createBasicBadge(name: String = "테스트 뱃지"): Decoration {
        return decorationRepository.save(
            Decoration(name = name, type = DecorationType.BADGE, tier = DecorationTier.BASIC,
                description = "테스트용", imageUrl = "/test/badge.png", isDefault = false)
        )
    }

    private fun createPremiumBorder(): Decoration {
        return decorationRepository.save(
            Decoration(name = "프리미엄 테두리", type = DecorationType.BORDER, tier = DecorationTier.PREMIUM,
                description = "프리미엄", imageUrl = "/test/premium.png", isDefault = false)
        )
    }

    // ─── V0 (공개) API 테스트 ───

    @Test
    @Order(1)
    fun `V0 전체 데코레이션 목록 조회`() {
        createBasicBorder()
        createBasicBadge()

        mockMvc.perform(get("/api/v0/decorations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(2)
    fun `V0 타입별 데코레이션 조회 - BORDER`() {
        createBasicBorder("테두리1")
        createBasicBorder("테두리2")
        createBasicBadge("뱃지1")

        mockMvc.perform(get("/api/v0/decorations/type/BORDER"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    @Order(3)
    fun `V0 기본(무료) 데코레이션만 조회`() {
        createBasicBorder()
        createPremiumBorder()

        mockMvc.perform(get("/api/v0/decorations/basic"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tier").value("BASIC"))
    }

    @Test
    @Order(4)
    fun `V0 펫 데코레이션 조회 - 장착 없음`() {
        val (_, pet) = createUserAndPet()

        mockMvc.perform(get("/api/v0/decorations/pet/${pet.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // ─── V1 (인증) API 테스트 ───

    @Test
    @Order(5)
    fun `V1 데코레이션 장착 성공 - BASIC 테두리`() {
        val (user, pet) = createUserAndPet()
        val border = createBasicBorder()

        val request = EquipDecorationRequest(petId = pet.id, decorationId = border.id)

        mockMvc.perform(
            post("/api/v1/decorations/equip")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.decoration_name").value("테스트 테두리"))
            .andExpect(jsonPath("$.decoration_type").value("BORDER"))
    }

    @Test
    @Order(6)
    fun `V1 데코레이션 장착 - 같은 타입 교체`() {
        val (user, pet) = createUserAndPet()
        val border1 = createBasicBorder("첫번째 테두리")
        val border2 = createBasicBorder("두번째 테두리")

        // 첫 번째 장착
        val request1 = EquipDecorationRequest(petId = pet.id, decorationId = border1.id)
        mockMvc.perform(
            post("/api/v1/decorations/equip")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request1))
        )
            .andExpect(status().isCreated)

        // 두 번째로 교체 (기존 해제 후 장착)
        val request2 = EquipDecorationRequest(petId = pet.id, decorationId = border2.id)
        mockMvc.perform(
            post("/api/v1/decorations/equip")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request2))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.decoration_name").value("두번째 테두리"))

        // 펫에 BORDER 타입 1개만 장착됨
        val equipped = petDecorationRepository.findByPetId(pet.id)
        val borders = equipped.filter { it.decorationType == DecorationType.BORDER }
        Assertions.assertEquals(1, borders.size)
        Assertions.assertEquals(border2.id, borders[0].decoration.id)
    }

    @Test
    @Order(7)
    fun `V1 데코레이션 장착 실패 - PREMIUM 티어 차단`() {
        val (user, pet) = createUserAndPet()
        val premiumBorder = createPremiumBorder()

        val request = EquipDecorationRequest(petId = pet.id, decorationId = premiumBorder.id)

        mockMvc.perform(
            post("/api/v1/decorations/equip")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @Order(8)
    fun `V1 데코레이션 장착 실패 - 존재하지 않는 펫`() {
        val (user, _) = createUserAndPet()
        val border = createBasicBorder()

        val request = EquipDecorationRequest(petId = 99999, decorationId = border.id)

        mockMvc.perform(
            post("/api/v1/decorations/equip")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(9)
    fun `V1 데코레이션 해제 성공`() {
        val (user, pet) = createUserAndPet()
        val border = createBasicBorder()

        // 먼저 장착
        val request = EquipDecorationRequest(petId = pet.id, decorationId = border.id)
        mockMvc.perform(
            post("/api/v1/decorations/equip")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated)

        // 해제
        mockMvc.perform(
            delete("/api/v1/decorations/pet/${pet.id}/type/BORDER").withAuth(user)
        )
            .andExpect(status().isNoContent)

        Assertions.assertEquals(0, petDecorationRepository.findByPetId(pet.id).size)
    }

    @Test
    @Order(10)
    fun `V1 데코레이션 장착 실패 - 인증 없음`() {
        val request = EquipDecorationRequest(petId = 1, decorationId = 1)

        mockMvc.perform(
            post("/api/v1/decorations/equip")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isUnauthorized.or(status().isForbidden))
    }

    @Test
    @Order(11)
    fun `트랜잭션 롤백 검증`() {
        Assertions.assertEquals(0, petDecorationRepository.count())
    }
}
