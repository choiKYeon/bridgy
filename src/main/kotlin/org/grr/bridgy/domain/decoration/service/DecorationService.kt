package org.grr.bridgy.domain.decoration.service

import org.grr.bridgy.common.exception.CustomException
import org.grr.bridgy.domain.decoration.dto.DecorationResponse
import org.grr.bridgy.domain.decoration.dto.EquipDecorationRequest
import org.grr.bridgy.domain.decoration.dto.PetDecorationResponse
import org.grr.bridgy.domain.decoration.entity.DecorationTier
import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.grr.bridgy.domain.decoration.entity.PetDecoration
import org.grr.bridgy.domain.decoration.repository.DecorationRepository
import org.grr.bridgy.domain.decoration.repository.PetDecorationRepository
import org.grr.bridgy.domain.pet.repository.PetRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DecorationService(
    private val decorationRepository: DecorationRepository,
    private val petDecorationRepository: PetDecorationRepository,
    private val petRepository: PetRepository
) {

    // 전체 데코레이션 목록 조회
    fun getAllDecorations(): List<DecorationResponse> {
        return decorationRepository.findAll().map { DecorationResponse.from(it) }
    }

    // 타입별 데코레이션 조회 (BORDER / BADGE)
    fun getDecorationsByType(type: DecorationType): List<DecorationResponse> {
        return decorationRepository.findByType(type).map { DecorationResponse.from(it) }
    }

    // 기본(무료) 데코레이션만 조회
    fun getBasicDecorations(): List<DecorationResponse> {
        return decorationRepository.findByTier(DecorationTier.BASIC).map { DecorationResponse.from(it) }
    }

    // 펫에 장착된 데코레이션 조회
    fun getPetDecorations(petId: Long): List<PetDecorationResponse> {
        return petDecorationRepository.findByPetId(petId).map { PetDecorationResponse.from(it) }
    }

    // 데코레이션 장착 (펫당 테두리 1개, 뱃지 1개)
    @Transactional
    fun equipDecoration(request: EquipDecorationRequest): PetDecorationResponse {
        val pet = petRepository.findById(request.petId)
            .orElseThrow { CustomException("반려동물을 찾을 수 없습니다. id=${request.petId}", HttpStatus.NOT_FOUND) }

        val decoration = decorationRepository.findById(request.decorationId)
            .orElseThrow { CustomException("데코레이션을 찾을 수 없습니다. id=${request.decorationId}", HttpStatus.NOT_FOUND) }

        // 기본(BASIC) 티어만 장착 가능 (향후 프리미엄 구매 시스템 확장 가능)
        if (decoration.tier != DecorationTier.BASIC) {
            throw CustomException("현재는 기본 데코레이션만 사용할 수 있습니다.", HttpStatus.FORBIDDEN)
        }

        // 같은 타입의 기존 데코레이션 해제 후 새로 장착
        petDecorationRepository.deleteByPetIdAndDecorationType(pet.id, decoration.type)

        val petDecoration = PetDecoration(
            pet = pet,
            decoration = decoration,
            decorationType = decoration.type
        )
        return PetDecorationResponse.from(petDecorationRepository.save(petDecoration))
    }

    // 데코레이션 해제
    @Transactional
    fun unequipDecoration(petId: Long, type: DecorationType) {
        if (!petRepository.existsById(petId)) {
            throw CustomException("반려동물을 찾을 수 없습니다. id=$petId", HttpStatus.NOT_FOUND)
        }
        petDecorationRepository.deleteByPetIdAndDecorationType(petId, type)
    }
}
