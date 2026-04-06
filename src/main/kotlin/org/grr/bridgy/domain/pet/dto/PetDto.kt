package org.grr.bridgy.domain.pet.dto

import org.grr.bridgy.domain.pet.entity.Pet
import org.grr.bridgy.domain.pet.entity.PetGender
import java.time.LocalDateTime

data class CreatePetRequest(
    val userId: Long,
    val name: String,
    val species: String,
    val breed: String? = null,
    val age: Int? = null,
    val gender: PetGender? = null,
    val weight: Double? = null,
    val bio: String? = null,
    val profileImageUrl: String? = null
)

data class UpdatePetRequest(
    val name: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val age: Int? = null,
    val gender: PetGender? = null,
    val weight: Double? = null,
    val bio: String? = null,
    val profileImageUrl: String? = null
)

data class PetResponse(
    val id: Long,
    val userId: Long,
    val ownerNickname: String,
    val name: String,
    val species: String,
    val breed: String?,
    val age: Int?,
    val gender: PetGender?,
    val weight: Double?,
    val bio: String?,
    val profileImageUrl: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(pet: Pet) = PetResponse(
            id = pet.id,
            userId = pet.user.id,
            ownerNickname = pet.user.nickname,
            name = pet.name,
            species = pet.species,
            breed = pet.breed,
            age = pet.age,
            gender = pet.gender,
            weight = pet.weight,
            bio = pet.bio,
            profileImageUrl = pet.profileImageUrl,
            createdAt = pet.createdAt
        )
    }
}

// 대시보드용 - 좋아요/댓글 수 포함
data class PetDashboardResponse(
    val id: Long,
    val userId: Long,
    val ownerNickname: String,
    val name: String,
    val species: String,
    val breed: String?,
    val age: Int?,
    val gender: PetGender?,
    val weight: Double?,
    val bio: String?,
    val profileImageUrl: String?,
    val likeCount: Long,
    val commentCount: Long,
    val galleryCount: Long,
    val isLiked: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(
            pet: Pet,
            likeCount: Long,
            commentCount: Long,
            galleryCount: Long,
            isLiked: Boolean = false
        ) = PetDashboardResponse(
            id = pet.id,
            userId = pet.user.id,
            ownerNickname = pet.user.nickname,
            name = pet.name,
            species = pet.species,
            breed = pet.breed,
            age = pet.age,
            gender = pet.gender,
            weight = pet.weight,
            bio = pet.bio,
            profileImageUrl = pet.profileImageUrl,
            likeCount = likeCount,
            commentCount = commentCount,
            galleryCount = galleryCount,
            isLiked = isLiked,
            createdAt = pet.createdAt
        )
    }
}
