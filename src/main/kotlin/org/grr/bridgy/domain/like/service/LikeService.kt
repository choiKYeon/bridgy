package org.grr.bridgy.domain.like.service

import org.grr.bridgy.common.exception.CustomException
import org.grr.bridgy.domain.like.dto.LikeRequest
import org.grr.bridgy.domain.like.dto.LikeResponse
import org.grr.bridgy.domain.like.entity.Like
import org.grr.bridgy.domain.like.repository.LikeRepository
import org.grr.bridgy.domain.pet.repository.PetRepository
import org.grr.bridgy.domain.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LikeService(
    private val likeRepository: LikeRepository,
    private val petRepository: PetRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun toggleLike(request: LikeRequest): LikeResponse {
        val pet = petRepository.findById(request.petId)
            .orElseThrow { CustomException("반려동물을 찾을 수 없습니다. id=${request.petId}", HttpStatus.NOT_FOUND) }
        val user = userRepository.findById(request.userId)
            .orElseThrow { CustomException("사용자를 찾을 수 없습니다. id=${request.userId}", HttpStatus.NOT_FOUND) }

        val existingLike = likeRepository.findByPetIdAndUserId(request.petId, request.userId)

        if (existingLike != null) {
            likeRepository.delete(existingLike)
            return LikeResponse(
                petId = request.petId,
                likeCount = likeRepository.countByPetId(request.petId),
                isLiked = false
            )
        } else {
            likeRepository.save(Like(pet = pet, user = user))
            return LikeResponse(
                petId = request.petId,
                likeCount = likeRepository.countByPetId(request.petId),
                isLiked = true
            )
        }
    }

    fun getLikeStatus(petId: Long, userId: Long): LikeResponse {
        return LikeResponse(
            petId = petId,
            likeCount = likeRepository.countByPetId(petId),
            isLiked = likeRepository.existsByPetIdAndUserId(petId, userId)
        )
    }

    fun getLikeCount(petId: Long): Long {
        return likeRepository.countByPetId(petId)
    }
}
