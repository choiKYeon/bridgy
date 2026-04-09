package org.grr.bridgy.domain.decoration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.decoration.dto.DecorationResponse
import org.grr.bridgy.domain.decoration.dto.PetDecorationResponse
import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.grr.bridgy.domain.decoration.service.DecorationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Decoration V0", description = "데코레이션 공개 API (인증 불필요)")
@RestController
@RequestMapping("/api/v0/decorations")
class DecorationControllerV0(
    private val decorationService: DecorationService
) {

    @Operation(
        summary = "전체 데코레이션 목록 조회",
        description = "플랫폼에서 사용 가능한 모든 데코레이션(테두리, 뱃지)의 목록을 조회합니다.",
        tags = ["Decoration"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "데코레이션 목록 조회 성공"
            )
        ]
    )
    @GetMapping
    fun getAllDecorations(): ResponseEntity<List<DecorationResponse>> {
        return ResponseEntity.ok(decorationService.getAllDecorations())
    }

    @Operation(
        summary = "데코레이션 타입별 조회",
        description = "데코레이션의 종류(BORDER: 테두리, BADGE: 뱃지)로 필터링하여 조회합니다.",
        tags = ["Decoration"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "데코레이션 목록 조회 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 타입"
            )
        ]
    )
    @GetMapping("/type/{type}")
    fun getDecorationsByType(
        @Parameter(
            description = "데코레이션 타입 (BORDER: 테두리, BADGE: 뱃지)",
            example = "BORDER",
            required = true
        )
        @PathVariable type: DecorationType
    ): ResponseEntity<List<DecorationResponse>> {
        return ResponseEntity.ok(decorationService.getDecorationsByType(type))
    }

    @Operation(
        summary = "기본(무료) 데코레이션 조회",
        description = "무료로 사용 가능한 기본 데코레이션만 조회합니다. 프리미엄 사용자 대상 데코레이션은 제외됩니다.",
        tags = ["Decoration"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "기본 데코레이션 목록 조회 성공"
            )
        ]
    )
    @GetMapping("/basic")
    fun getBasicDecorations(): ResponseEntity<List<DecorationResponse>> {
        return ResponseEntity.ok(decorationService.getBasicDecorations())
    }

    @Operation(
        summary = "반려동물의 장착된 데코레이션 조회",
        description = "특정 반려동물에 현재 장착되어 있는 데코레이션의 목록을 조회합니다.",
        tags = ["Decoration"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "장착된 데코레이션 목록 조회 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/pet/{petId}")
    fun getPetDecorations(
        @Parameter(
            description = "조회할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long
    ): ResponseEntity<List<PetDecorationResponse>> {
        return ResponseEntity.ok(decorationService.getPetDecorations(petId))
    }
}
