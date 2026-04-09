package org.grr.bridgy.domain.like.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.like.dto.LikeRequest
import org.grr.bridgy.domain.like.dto.LikeResponse
import org.grr.bridgy.domain.like.service.LikeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Like V1", description = "좋아요 인증 API (JWT Bearer 토큰 필요)")
@RestController
@RequestMapping("/api/v1/likes")
class LikeControllerV1(
    private val likeService: LikeService
) {

    @Operation(
        summary = "좋아요 토글",
        description = "반려동물에 대한 좋아요를 누르거나 취소합니다. 같은 요청을 반복하면 토글 방식으로 작동합니다.",
        tags = ["Like"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "좋아요 토글 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = LikeResponse::class)
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
                description = "접근 권한 없음"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물 또는 사용자를 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @PostMapping
    fun toggleLike(@RequestBody request: LikeRequest): ResponseEntity<LikeResponse> {
        return ResponseEntity.ok(likeService.toggleLike(request))
    }
}
