package org.grr.bridgy.domain.gallery

import org.grr.bridgy.common.BaseIntegrationTest
import org.grr.bridgy.common.config.FreeTierLimits
import org.grr.bridgy.domain.gallery.dto.CreateGalleryRequest
import org.grr.bridgy.domain.gallery.entity.Gallery
import org.grr.bridgy.domain.gallery.repository.GalleryRepository
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
class GalleryIntegrationTest : BaseIntegrationTest() {

    @Autowired lateinit var galleryRepository: GalleryRepository
    @Autowired lateinit var petRepository: PetRepository

    private fun createUserAndPet(): Pair<User, Pet> {
        val user = createTestUser(email = "gallery@test.com", nickname = "갤러리유저")
        val pet = petRepository.save(Pet(user = user, name = "뽀삐", species = "강아지", breed = "포메라니안", age = 3, gender = PetGender.MALE))
        return Pair(user, pet)
    }

    // ─── V0 (공개) API 테스트 ───

    @Test
    @Order(1)
    fun `V0 펫별 사진 조회 - 최신순 정렬`() {
        val (_, pet) = createUserAndPet()
        galleryRepository.save(Gallery(pet = pet, imageUrl = "photo1.jpg", caption = "첫번째"))
        galleryRepository.save(Gallery(pet = pet, imageUrl = "photo2.jpg", caption = "두번째"))
        galleryRepository.save(Gallery(pet = pet, imageUrl = "photo3.jpg", caption = "세번째"))

        mockMvc.perform(get("/api/v0/gallery/pet/${pet.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    @Order(2)
    fun `V0 사진 단건 조회`() {
        val (_, pet) = createUserAndPet()
        val gallery = galleryRepository.save(Gallery(pet = pet, imageUrl = "photo.jpg", caption = "귀여운 뽀삐"))

        mockMvc.perform(get("/api/v0/gallery/${gallery.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.caption").value("귀여운 뽀삐"))
    }

    @Test
    @Order(3)
    fun `V0 사진 단건 조회 실패 - 존재하지 않는 ID`() {
        mockMvc.perform(get("/api/v0/gallery/99999"))
            .andExpect(status().isNotFound)
    }

    // ─── V1 (인증) API 테스트 ───

    @Test
    @Order(4)
    fun `V1 사진 업로드 성공`() {
        val (user, pet) = createUserAndPet()
        val request = CreateGalleryRequest(
            petId = pet.id,
            imageUrl = "https://example.com/photo1.jpg",
            caption = "산책 중 뽀삐"
        )

        mockMvc.perform(
            post("/api/v1/gallery")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.image_url").value("https://example.com/photo1.jpg"))
            .andExpect(jsonPath("$.caption").value("산책 중 뽀삐"))

        Assertions.assertEquals(1, galleryRepository.countByPetId(pet.id))
    }

    @Test
    @Order(5)
    fun `V1 사진 업로드 실패 - caption에 비속어`() {
        val (user, pet) = createUserAndPet()
        val request = CreateGalleryRequest(
            petId = pet.id,
            imageUrl = "https://example.com/photo.jpg",
            caption = "씨발 귀엽다"
        )

        mockMvc.perform(
            post("/api/v1/gallery")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)

        Assertions.assertEquals(0, galleryRepository.countByPetId(pet.id))
    }

    @Test
    @Order(6)
    fun `V1 사진 업로드 실패 - 최대 업로드 수 초과`() {
        val (user, pet) = createUserAndPet()

        repeat(FreeTierLimits.MAX_PHOTOS_PER_PET) { i ->
            galleryRepository.save(Gallery(pet = pet, imageUrl = "photo$i.jpg"))
        }

        val request = CreateGalleryRequest(
            petId = pet.id,
            imageUrl = "https://example.com/overflow.jpg",
            caption = "한 장 더"
        )

        mockMvc.perform(
            post("/api/v1/gallery")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)

        Assertions.assertEquals(FreeTierLimits.MAX_PHOTOS_PER_PET.toLong(), galleryRepository.countByPetId(pet.id))
    }

    @Test
    @Order(7)
    fun `V1 사진 업로드 실패 - 존재하지 않는 펫`() {
        val (user, _) = createUserAndPet()
        val request = CreateGalleryRequest(petId = 99999, imageUrl = "https://example.com/photo.jpg")

        mockMvc.perform(
            post("/api/v1/gallery")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(8)
    fun `V1 사진 업로드 실패 - 인증 없음`() {
        val request = CreateGalleryRequest(petId = 1, imageUrl = "https://example.com/photo.jpg")

        mockMvc.perform(
            post("/api/v1/gallery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(9)
    fun `V1 사진 삭제 성공`() {
        val (user, pet) = createUserAndPet()
        val gallery = galleryRepository.save(Gallery(pet = pet, imageUrl = "photo.jpg"))

        mockMvc.perform(
            delete("/api/v1/gallery/${gallery.id}").withAuth(user)
        )
            .andExpect(status().isNoContent)

        Assertions.assertFalse(galleryRepository.existsById(gallery.id))
    }

    @Test
    @Order(10)
    fun `V1 사진 삭제 실패 - 존재하지 않는 ID`() {
        val (user, _) = createUserAndPet()

        mockMvc.perform(
            delete("/api/v1/gallery/99999").withAuth(user)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Order(11)
    fun `V1 사진 삭제 실패 - 인증 없음`() {
        val (_, pet) = createUserAndPet()
        val gallery = galleryRepository.save(Gallery(pet = pet, imageUrl = "photo.jpg"))

        mockMvc.perform(
            delete("/api/v1/gallery/${gallery.id}")
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(12)
    fun `V1 사진 삭제 후 재업로드 가능`() {
        val (user, pet) = createUserAndPet()

        val galleries = (1..FreeTierLimits.MAX_PHOTOS_PER_PET).map { i ->
            galleryRepository.save(Gallery(pet = pet, imageUrl = "photo$i.jpg"))
        }

        galleryRepository.deleteById(galleries.first().id)

        val request = CreateGalleryRequest(petId = pet.id, imageUrl = "new_photo.jpg")
        mockMvc.perform(
            post("/api/v1/gallery")
                .withAuth(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated)
    }

    @Test
    @Order(13)
    fun `트랜잭션 롤백 검증`() {
        Assertions.assertEquals(0, galleryRepository.count())
    }
}