package org.grr.bridgy.domain.pet.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.pet.dto.CreatePetRequest
import org.grr.bridgy.domain.pet.dto.PetResponse
import org.grr.bridgy.domain.pet.dto.UpdatePetRequest
import org.grr.bridgy.domain.pet.service.PetService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Pet V1", description = "반려동물 인증 API (JWT Bearer 토큰 필요)")
@RestController
@RequestMapping("/api/v1/pets")
class PetControllerV1(
    private val petService: PetService
) {

    @Operation(
        summary = "반려동물 등록",
        description = "새로운 반려동물 프로필을 등록합니다. 무료 사용자는 최대 3마리까지만 등록 가능합니다.",
        tags = ["Pet"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "반려동물 등록 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PetResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 입력 데이터"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "반려동물 등록 제한 초과 (최대 3마리)"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @PostMapping
    fun createPet(@RequestBody request: CreatePetRequest): ResponseEntity<PetResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(request))
    }

    @Operation(
        summary = "반려동물 정보 수정",
        description = "반려동물의 기본 정보(이름, 나이, 성별, 종류, 프로필 이미지 등)를 수정합니다.",
        tags = ["Pet"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "반려동물 정보 수정 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PetResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 입력 데이터"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "자신의 반려동물만 수정 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @PutMapping("/{petId}")
    fun updatePet(
        @Parameter(
            description = "수정할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long,
        @RequestBody request: UpdatePetRequest
    ): ResponseEntity<PetResponse> {
        return ResponseEntity.ok(petService.updatePet(petId, request))
    }

    @Operation(
        summary = "반려동물 삭제",
        description = "반려동물 프로필을 삭제합니다. 관련된 갤러리, 댓글, 좋아요 등도 함께 삭제됩니다. 이 작업은 복구할 수 없습니다.",
        tags = ["Pet"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "반려동물 삭제 성공"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "자신의 반려동물만 삭제 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @DeleteMapping("/{petId}")
    fun deletePet(
        @Parameter(
            description = "삭제할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long
    ): ResponseEntity<Void> {
        petService.deletePet(petId)
        return ResponseEntity.noContent().build()
    }
}
