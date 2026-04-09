package org.grr.bridgy.domain.decoration.dto

import org.grr.bridgy.domain.decoration.entity.Decoration
import org.grr.bridgy.domain.decoration.entity.DecorationTier
import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.grr.bridgy.domain.decoration.entity.PetDecoration
import java.time.LocalDateTime

// 데코레이션 목록 조회 응답
data class DecorationResponse(
    val id: Long,
    val name: String,
    val type: DecorationType,
    val tier: DecorationTier,
    val description: String?,
    val imageUrl: String,
    val isDefault: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(decoration: Decoration) = DecorationResponse(
            id = decoration.id,
            name = decoration.name,
            type = decoration.type,
            tier = decoration.tier,
            description = decoration.description,
            imageUrl = decoration.imageUrl,
            isDefault = decoration.isDefault,
            createdAt = decoration.createdAt
        )
    }
}

// 펫 데코레이션 장착 요청
data class EquipDecorationRequest(
    val petId: Long,
    val decorationId: Long
)

// 펫에 장착된 데코레이션 응답
data class PetDecorationResponse(
    val id: Long,
    val petId: Long,
    val decorationId: Long,
    val decorationName: String,
    val decorationType: DecorationType,
    val decorationTier: DecorationTier,
    val imageUrl: String,
    val equippedAt: LocalDateTime
) {
    companion object {
        fun from(petDecoration: PetDecoration) = PetDecorationResponse(
            id = petDecoration.id,
            petId = petDecoration.pet.id,
            decorationId = petDecoration.decoration.id,
            decorationName = petDecoration.decoration.name,
            decorationType = petDecoration.decorationType,
            decorationTier = petDecoration.decoration.tier,
            imageUrl = petDecoration.decoration.imageUrl,
            equippedAt = petDecoration.createdAt
        )
    }
}
