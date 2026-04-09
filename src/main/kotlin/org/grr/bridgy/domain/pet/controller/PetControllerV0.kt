package org.grr.bridgy.domain.pet.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.common.config.FreeTierLimits
import org.grr.bridgy.domain.pet.dto.PetDashboardResponse
import org.grr.bridgy.domain.pet.dto.PetResponse
import org.grr.bridgy.domain.pet.service.PetService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Pet V0", description = "반려동물 공개 API (인증 불필요)")
@RestController
@RequestMapping("/api/v0/pets")
class PetControllerV0(
    private val petService: PetService
) {

    @Operation(
        summary = "반려동물 단건 조회",
        description = "반려동물 ID로 반려동물 기본 정보를 조회합니다.",
        tags = ["Pet"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "반려동물 정보 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PetResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/{petId}")
    fun getPet(
        @Parameter(
            description = "조회할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long
    ): ResponseEntity<PetResponse> {
        return ResponseEntity.ok(petService.getPetById(petId))
    }

    @Operation(
        summary = "전체 반려동물 목록",
        description = "플랫폼에 등록된 모든 반려동물을 조회합니다.",
        tags = ["Pet"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "반려동물 목록 조회 성공"
            )
        ]
    )
    @GetMapping
    fun getAllPets(): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.getAllPets())
    }

    @Operation(
        summary = "사용자별 반려동물 목록",
        description = "특정 사용자가 등록한 반려동물 목록을 조회합니다.",
        tags = ["Pet"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "반려동물 목록 조회 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 사용자를 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/user/{userId}")
    fun getPetsByUser(
        @Parameter(
            description = "사용자의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable userId: Long
    ): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.getPetsByUserId(userId))
    }

    @Operation(
        summary = "반려동물 이름으로 검색",
        description = "반려동물 이름 키워드로 검색합니다. 검색 결과는 페이징으로 제공되며, 페이지당 최대 20개까지 조회 가능합니다.",
        tags = ["Pet"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "검색 결과 조회 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 페이지 번호 또는 크기"
            )
        ]
    )
    @GetMapping("/search")
    fun searchPets(
        @Parameter(
            description = "검색 키워드 (반려동물 이름)",
            example = "뽀삐",
            required = true
        )
        @RequestParam name: String,
        @Parameter(
            description = "페이지 번호 (0부터 시작)",
            example = "0"
        )
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(
            description = "페이지 크기 (기본값: 20, 최대값: 20)",
            example = "20"
        )
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<PetResponse>> {
        val pageable = PageRequest.of(page, size.coerceAtMost(FreeTierLimits.MAX_PAGE_SIZE))
        return ResponseEntity.ok(petService.searchPetsByNamePaged(name, pageable))
    }

    @Operation(
        summary = "종류별 반려동물 조회",
        description = "반려동물의 종류(강아지, 고양이 등)로 필터링하여 조회합니다.",
        tags = ["Pet"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "반려동물 목록 조회 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 종류"
            )
        ]
    )
    @GetMapping("/species/{species}")
    fun getPetsBySpecies(
        @Parameter(
            description = "반려동물 종류 (예: 강아지, 고양이, 토끼)",
            example = "강아지",
            required = true
        )
        @PathVariable species: String
    ): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.getPetsBySpecies(species))
    }

    @Operation(
        summary = "인기순 피드 조회",
        description = "좋아요가 많은 순서로 반려동물을 조회합니다. 페이지당 최대 20개씩 조회 가능합니다. userId 파라미터를 전달하면 현재 사용자의 좋아요 상태를 함께 반환합니다.",
        tags = ["Pet"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "인기순 피드 조회 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 페이지 번호 또는 크기"
            )
        ]
    )
    @GetMapping("/feed/popular")
    fun getPopularPets(
        @Parameter(
            description = "현재 사용자 ID (선택사항, 좋아요 상태 확인용)",
            example = "1"
        )
        @RequestParam(required = false) userId: Long?,
        @Parameter(
            description = "페이지 번호 (0부터 시작)",
            example = "0"
        )
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(
            description = "페이지 크기 (기본값: 20, 최대값: 20)",
            example = "20"
        )
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<PetDashboardResponse>> {
        val pageable = PageRequest.of(page, size.coerceAtMost(FreeTierLimits.MAX_PAGE_SIZE))
        return ResponseEntity.ok(petService.getPopularPets(userId, pageable))
    }

    @Operation(
        summary = "최신순 피드 조회",
        description = "최근에 등록된 순서로 반려동물을 조회합니다. 페이지당 최대 20개씩 조회 가능합니다. userId 파라미터를 전달하면 현재 사용자의 좋아요 상태를 함께 반환합니다.",
        tags = ["Pet"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "최신순 피드 조회 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 페이지 번호 또는 크기"
            )
        ]
    )
    @GetMapping("/feed/recent")
    fun getRecentPets(
        @Parameter(
            description = "현재 사용자 ID (선택사항, 좋아요 상태 확인용)",
            example = "1"
        )
        @RequestParam(required = false) userId: Long?,
        @Parameter(
            description = "페이지 번호 (0부터 시작)",
            example = "0"
        )
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(
            description = "페이지 크기 (기본값: 20, 최대값: 20)",
            example = "20"
        )
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<PetDashboardResponse>> {
        val pageable = PageRequest.of(page, size.coerceAtMost(FreeTierLimits.MAX_PAGE_SIZE))
        return ResponseEntity.ok(petService.getRecentPets(userId, pageable))
    }

    @Operation(
        summary = "반려동물 상세 정보 조회",
        description = "반려동물의 상세 정보를 조회합니다. 좋아요 수, 댓글 수, 갤러리 사진 수 등의 통계가 포함됩니다.",
        tags = ["Pet"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "상세 정보 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PetDashboardResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/{petId}/detail")
    fun getPetDetail(
        @Parameter(
            description = "조회할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long,
        @Parameter(
            description = "현재 사용자 ID (선택사항, 좋아요 상태 확인용)",
            example = "1"
        )
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<PetDashboardResponse> {
        return ResponseEntity.ok(petService.getPetDetail(petId, userId))
    }
}
