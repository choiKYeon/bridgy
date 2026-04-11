package org.grr.bridgy.domain.pet.service

import org.grr.bridgy.common.config.FreeTierLimits
import org.grr.bridgy.common.exception.CustomException
import org.grr.bridgy.common.filter.ProfanityFilter
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.grr.bridgy.domain.comment.repository.CommentRepository
import org.grr.bridgy.domain.decoration.dto.PetDecorationResponse
import org.grr.bridgy.domain.decoration.repository.PetDecorationRepository
import org.grr.bridgy.domain.gallery.repository.GalleryRepository
import org.grr.bridgy.domain.like.repository.LikeRepository
import org.grr.bridgy.domain.pet.dto.CreatePetRequest
import org.grr.bridgy.domain.pet.dto.PetDashboardResponse
import org.grr.bridgy.domain.pet.dto.PetResponse
import org.grr.bridgy.domain.pet.dto.UpdatePetRequest
import org.grr.bridgy.domain.pet.entity.Pet
import org.grr.bridgy.domain.pet.repository.PetRepository
import org.grr.bridgy.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
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
    private val galleryRepository: GalleryRepository,
    private val petDecorationRepository: PetDecorationRepository,
    private val profanityFilter: ProfanityFilter
) {

    @Transactional
    @CacheEvict("petsByUser", key = "#request.userId")
    fun createPet(request: CreatePetRequest): PetResponse {
        val user = userRepository.findById(request.userId)
            .orElseThrow { CustomException("사용자를 찾을 수 없습니다. id=${request.userId}", HttpStatus.NOT_FOUND) }

        profanityFilter.check(request.name)
        request.bio?.let { profanityFilter.check(it) }

        val currentPetCount = petRepository.countByUserId(request.userId)
        if (currentPetCount >= FreeTierLimits.MAX_PETS_PER_USER) {
            throw CustomException(
                "반려동물은 최대 ${FreeTierLimits.MAX_PETS_PER_USER}마리까지 등록할 수 있습니다. (현재: ${currentPetCount}마리)",
                HttpStatus.BAD_REQUEST
            )
        }

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

    @Cacheable("pets", key = "#petId")
    fun getPetById(petId: Long): PetResponse {
        val pet = petRepository.findById(petId)
            .orElseThrow { CustomException("반려동물을 찾을 수 없습니다. id=$petId", HttpStatus.NOT_FOUND) }
        return PetResponse.from(pet)
    }

    @Cacheable("petsByUser", key = "#userId")
    fun getPetsByUserId(userId: Long): List<PetResponse> {
        return petRepository.findByUserId(userId).map { PetResponse.from(it) }
    }

    fun getAllPets(): List<PetResponse> {
        return petRepository.findAll().map { PetResponse.from(it) }
    }

    fun getPetsBySpecies(species: String): List<PetResponse> {
        return petRepository.findBySpecies(species).map { PetResponse.from(it) }
    }

    @Transactional
    @Caching(evict = [
        CacheEvict("pets", key = "#petId"),
        CacheEvict("petsByUser", allEntries = true)
    ])
    fun updatePet(petId: Long, request: UpdatePetRequest): PetResponse {
        val pet = petRepository.findById(petId)
            .orElseThrow { CustomException("반려동물을 찾을 수 없습니다. id=$petId", HttpStatus.NOT_FOUND) }

        request.name?.let { profanityFilter.check(it); pet.name = it }
        request.species?.let { pet.species = it }
        request.breed?.let { pet.breed = it }
        request.age?.let { pet.age = it }
        request.gender?.let { pet.gender = it }
        request.weight?.let { pet.weight = it }
        request.bio?.let { profanityFilter.check(it); pet.bio = it }
        request.profileImageUrl?.let { pet.profileImageUrl = it }
        pet.updatedAt = LocalDateTime.now()

        return PetResponse.from(petRepository.save(pet))
    }

    @Transactional
    @Caching(evict = [
        CacheEvict("pets", key = "#petId"),
        CacheEvict("petsByUser", allEntries = true)
    ])
    fun deletePet(petId: Long) {
        if (!petRepository.existsById(petId)) {
            throw CustomException("반려동물을 찾을 수 없습니다. id=$petId", HttpStatus.NOT_FOUND)
        }
        petRepository.deleteById(petId)
    }

    fun searchPetsByNamePaged(name: String, pageable: Pageable): Page<PetResponse> {
        val safePageable = capPageSize(pageable)
        return petRepository.findByNameContaining(name, safePageable).map { PetResponse.from(it) }
    }

    // ─── 대시보드 / 피드 ───

    fun getPopularPets(currentUserId: Long? = null, pageable: Pageable): Page<PetDashboardResponse> {
        val safePageable = capPageSize(pageable)
        return petRepository.findAllOrderByLikeCountDesc(safePageable).map { pet ->
            toDashboardResponse(pet, currentUserId)
        }
    }

    fun getRecentPets(currentUserId: Long? = null, pageable: Pageable): Page<PetDashboardResponse> {
        val safePageable = capPageSize(pageable)
        return petRepository.findAllByOrderByCreatedAtDesc(safePageable).map { pet ->
            toDashboardResponse(pet, currentUserId)
        }
    }

    private fun capPageSize(pageable: Pageable): Pageable {
        val maxSize = FreeTierLimits.MAX_PAGE_SIZE
        return if (pageable.pageSize > maxSize) {
            PageRequest.of(pageable.pageNumber, maxSize, pageable.sort)
        } else {
            pageable
        }
    }

    fun getPetDetail(petId: Long, currentUserId: Long? = null): PetDashboardResponse {
        val pet = petRepository.findById(petId)
            .orElseThrow { CustomException("반려동물을 찾을 수 없습니다. id=$petId", HttpStatus.NOT_FOUND) }
        return toDashboardResponse(pet, currentUserId)
    }

    private fun toDashboardResponse(pet: Pet, currentUserId: Long? = null): PetDashboardResponse {
        val likeCount = likeRepository.countByPetId(pet.id)
        val commentCount = commentRepository.countByPetId(pet.id)
        val galleryCount = galleryRepository.countByPetId(pet.id)
        val isLiked = currentUserId?.let { likeRepository.existsByPetIdAndUserId(pet.id, it) } ?: false
        val decorations = petDecorationRepository.findByPetId(pet.id).map { PetDecorationResponse.from(it) }

        return PetDashboardResponse.from(
            pet = pet,
            likeCount = likeCount,
            commentCount = commentCount,
            galleryCount = galleryCount,
            isLiked = isLiked,
            decorations = decorations
        )
    }
}
