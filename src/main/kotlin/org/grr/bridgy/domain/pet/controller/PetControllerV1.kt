package org.grr.bridgy.domain.pet.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.pet.dto.CreatePetRequest
import org.grr.bridgy.domain.pet.dto.PetResponse
import org.grr.bridgy.domain.pet.dto.UpdatePetRequest
import org.grr.bridgy.domain.pet.service.PetService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Pet V1", description = "반려동물 인증 API")
@RestController
@RequestMapping("/api/v1/pets")
class PetControllerV1(
    private val petService: PetService
) {

    @Operation(summary = "반려동물 등록", description = "새 반려동물 프로필 등록")
    @PostMapping
    fun createPet(@RequestBody request: CreatePetRequest): ResponseEntity<PetResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(request))
    }

    @Operation(summary = "반려동물 수정", description = "반려동물 프로필 정보 수정")
    @PutMapping("/{petId}")
    fun updatePet(
        @PathVariable petId: Long,
        @RequestBody request: UpdatePetRequest
    ): ResponseEntity<PetResponse> {
        return ResponseEntity.ok(petService.updatePet(petId, request))
    }

    @Operation(summary = "반려동물 삭제", description = "반려동물 프로필 삭제")
    @DeleteMapping("/{petId}")
    fun deletePet(@PathVariable petId: Long): ResponseEntity<Void> {
        petService.deletePet(petId)
        return ResponseEntity.noContent().build()
    }
}
