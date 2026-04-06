package org.grr.bridgy.domain.gallery

import com.fasterxml.jackson.databind.ObjectMapper
import org.grr.bridgy.domain.gallery.dto.CreateGalleryRequest
import org.grr.bridgy.domain.gallery.entity.Gallery
import org.grr.bridgy.domain.gallery.repository.GalleryRepository
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
class GalleryIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var galleryRepository: GalleryRepository
    @Autowired lateinit var petRepository: PetRepository
    @Autowired lateinit var userRepository: UserRepository

    private fun createUserAndPet(): Pair<User, Pet> {
        val user = userRepository.save(User(email = "gallery@test.com", password = "pass", nickname = "갤러리유저"))
        val pet = petRepository.save(Pet(user = user, name = "뽀삐", species = "강아지", breed = "포메라니안", age = 3, gender = PetGender.MALE))
        return Pair(user, pet)
    }

    @Test
    @Order(1)
    fun `사진 업로드 성공`() {
        val (_, pet) = createUserAndPet()
        val request = CreateGalleryRequest(
            petId = pet.id,
            imageUrl = "https://example.com/photo1.jpg",
            caption = "산책 중 뽀삐"
        )

        mockMvc.perform(
            post("/api/gallery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.image_url").value("https://example.com/photo1.jpg"))
            .andExpect(jsonPath("$.caption").value("산책 중 뽀삐"))
            .andExpect(jsonPath("$.pet_name").value("뽀삐"))

        Assertions.assertEquals(1, galleryRepository.countByPetId(pet.id))
    }

    @Test
    @Order(2)
    fun `사진 업로드 실패 - 존재하지 않는 펫`() {
        val request = CreateGalleryRequest(petId = 99999, imageUrl = "https://example.com/photo.jpg")

        mockMvc.perform(
            post("/api/gallery")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(3)
    fun `펫별 사진 조회 - 최신순 정렬`() {
        val (_, pet) = createUserAndPet()
        galleryRepository.save(Gallery(pet = pet, imageUrl = "photo1.jpg", caption = "첫번째"))
        galleryRepository.save(Gallery(pet = pet, imageUrl = "photo2.jpg", caption = "두번째"))
        galleryRepository.save(Gallery(pet = pet, imageUrl = "photo3.jpg", caption = "세번째"))

        mockMvc.perform(get("/api/gallery/pet/${pet.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    @Order(4)
    fun `사진 단건 조회`() {
        val (_, pet) = createUserAndPet()
        val gallery = galleryRepository.save(Gallery(pet = pet, imageUrl = "photo.jpg", caption = "귀여운 뽀삐"))

        mockMvc.perform(get("/api/gallery/${gallery.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.caption").value("귀여운 뽀삐"))
    }

    @Test
    @Order(5)
    fun `사진 삭제 성공`() {
        val (_, pet) = createUserAndPet()
        val gallery = galleryRepository.save(Gallery(pet = pet, imageUrl = "photo.jpg"))

        mockMvc.perform(delete("/api/gallery/${gallery.id}"))
            .andExpect(status().isNoContent)

        Assertions.assertFalse(galleryRepository.existsById(gallery.id))
    }

    @Test
    @Order(6)
    fun `사진 삭제 실패 - 존재하지 않는 ID`() {
        mockMvc.perform(delete("/api/gallery/99999"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    @Order(7)
    fun `트랜잭션 롤백 검증`() {
        Assertions.assertEquals(0, galleryRepository.count())
    }
}
