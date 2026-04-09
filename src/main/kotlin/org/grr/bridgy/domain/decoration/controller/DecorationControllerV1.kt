package org.grr.bridgy.domain.decoration.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.decoration.dto.EquipDecorationRequest
import org.grr.bridgy.domain.decoration.dto.PetDecorationResponse
import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.grr.bridgy.domain.decoration.service.DecorationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Decoration V1", description = "데코레이션 인증 API (JWT Bearer 토큰 필요)")
@RestController
@RequestMapping("/api/v1/decorations")
class DecorationControllerV1(
    private val decorationService: DecorationService
) {

    @Operation(
        summary = "데코레이션 장착",
        description = "반려동물에 데코레이션을 장착합니다. 무료 사용자: 기본 데코레이션만 장착 가능, 프리미엄 사용자: 모든 데코레이션 사용 가능. 타입당 최대 1개 (테두리 1개, 뱃지 1개).",
        tags = ["Decoration"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "데코레이션 장착 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PetDecorationResponse::class)
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
                description = "무료 사용자는 기본 데코레이션만 장착 가능하거나 자신의 반려동물만 데코레이션 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물 또는 데코레이션을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @PostMapping("/equip")
    fun equipDecoration(@RequestBody request: EquipDecorationRequest): ResponseEntity<PetDecorationResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(decorationService.equipDecoration(request))
    }

    @Operation(
        summary = "데코레이션 해제",
        description = "반려동물에 장착된 데코레이션을 해제합니다. 타입별로 최대 1개씩만 장착할 수 있으므로, 타입을 지정하면 해당 타입의 데코레이션이 해제됩니다.",
        tags = ["Decoration"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "데코레이션 해제 성공"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "자신의 반려동물에만 해제 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물 또는 장착된 데코레이션을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @DeleteMapping("/pet/{petId}/type/{type}")
    fun unequipDecoration(
        @Parameter(
            description = "데코레이션을 해제할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long,
        @Parameter(
            description = "해제할 데코레이션 타입 (BORDER: 테두리, BADGE: 뱃지)",
            example = "BORDER",
            required = true
        )
        @PathVariable type: DecorationType
    ): ResponseEntity<Void> {
        decorationService.unequipDecoration(petId, type)
        return ResponseEntity.noContent().build()
    }
}
