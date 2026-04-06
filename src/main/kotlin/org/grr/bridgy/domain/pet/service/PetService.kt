package org.grr.bridgy.domain.pet.service

import org.grr.bridgy.domain.comment.repository.CommentRepository
import org.grr.bridgy.domain.gallery.repository.GalleryRepository
import org.grr.bridgy.domain.like.repository.LikeRepository
import org.grr.bridgy.domain.pet.dto.CreatePetRequest
import org.grr.bridgy.domain.pet.dto.PetDashboardResponse
import org.grr.bridgy.domain.pet.dto.PetResponse
import org.grr.bridgy.domain.pet.dto.UpdatePetRequest
import org.grr.bridgy.domain.pet.entity.Pet
import org.grr.bridgy.domain.pet.repository.PetRepository
import org.grr.bridgy.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class PetService(
    private val petRepository: PetRepository,
    private val userRepository: UserRepository,
    private val likeRepository: LikeRepository,
    private val commentRepository: CommentRepository,
    private val galleryRepository: GalleryRepository
) {

    @Transactional
    fun createPet(request: CreatePetRequest): PetResponse {
        val user = userRepository.findById(request.userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다. id=${request.userId}") }

        val pet = Pet(
            user = user,
            name = request.name,
            species = request.species,
            breed = request.breed,
            age = request.age,
            gender = request.gender,
            weight = request.weight,
            bio = request.bio,
            profileImageUrl = request.profileImageUrl
        )
        return PetResponse.from(petRepository.save(pet))
    }

    fun getPetById(petId: Long): PetResponse {
        val pet = petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("반려동물을 찾을 수 없습니다. id=$petId") }
        return PetResponse.from(pet)
    }

    fun getPetsByUserId(userId: Long): List<PetResponse> {
        return petRepository.findByUserId(userId).map { PetResponse.from(it) }
    }

    fun getAllPets(): List<PetResponse> {
        return petRepository.findAll().map { PetResponse.from(it) }
    }

    fun searchPetsByName(name: String): List<PetResponse> {
        return petRepository.findByNameContaining(name).map { PetResponse.from(it) }
    }

    fun getPetsBySpecies(species: String): List<PetResponse> {
        return petRepository.findBySpecies(species).map { PetResponse.from(it) }
    }

    @Transactional
    fun updatePet(petId: Long, request: UpdatePetRequest): PetResponse {
        val pet = petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("반려동물을 찾을 수 없습니다. id=$petId") }

        request.name?.let { pet.name = it }
        request.species?.let { pet.species = it }
        request.breed?.let { pet.breed = it }
        request.age?.let { pet.age = it }
        request.gender?.let { pet.gender = it }
        request.weight?.let { pet.weight = it }
        request.bio?.let { pet.bio = it }
        request.profileImageUrl?.let { pet.profileImageUrl = it }
        pet.updatedAt = LocalDateTime.now()

        return PetResponse.from(petRepository.save(pet))
    }

    @Transactional
    fun deletePet(petId: Long) {
        require(petRepository.existsById(petId)) { "반려동물을 찾을 수 없습니다. id=$petId" }
        petRepository.deleteById(petId)
    }

    // ─── 대시보드 / 피드 ───

    fun getPopularPets(currentUserId: Long? = null): List<PetDashboardResponse> {
        return petRepository.findAllOrderByLikeCountDesc().map { pet ->
            toDashboardResponse(pet, currentUserId)
        }
    }

    fun getRecentPets(currentUserId: Long? = null): List<PetDashboardResponse> {
        return petRepository.findAllByOrderByCreatedAtDesc().map { pet ->
            toDashboardResponse(pet, currentUserId)
        }
    }

    fun getPetDetail(petId: Long, currentUserId: Long? = null): PetDashboardResponse {
        val pet = petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("반려동물을 찾을 수 없습니다. id=$petId") }
        return toDashboardResponse(pet, currentUserId)
    }

    private fun toDashboardResponse(pet: Pet, currentUserId: Long? = null): PetDashboardResponse {
        val likeCount = likeRepository.countByPetId(pet.id)
        val commentCount = commentRepository.countByPetId(pet.id)
        val galleryCount = galleryRepository.countByPetId(pet.id)
        val isLiked = currentUserId?.let { likeRepository.existsByPetIdAndUserId(pet.id, it) } ?: false

        return PetDashboardResponse.from(
            pet = pet,
            likeCount = likeCount,
            commentCount = commentCount,
            galleryCount = galleryCount,
            isLiked = isLiked
        )
    }
}
