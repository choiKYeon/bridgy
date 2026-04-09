package org.grr.bridgy.domain.like.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.like.dto.LikeResponse
import org.grr.bridgy.domain.like.service.LikeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Like V0", description = "좋아요 공개 API (인증 불필요)")
@RestController
@RequestMapping("/api/v0/likes")
class LikeControllerV0(
    private val likeService: LikeService
) {

    @Operation(
        summary = "사용자의 좋아요 상태 조회",
        description = "특정 반려동물에 대한 사용자의 좋아요 여부를 확인하고, 해당 반려동물의 총 좋아요 수를 조회합니다.",
        tags = ["Like"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "좋아요 상태 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = LikeResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물 또는 사용자를 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/pet/{petId}/user/{userId}")
    fun getLikeStatus(
        @Parameter(
            description = "조회할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long,
        @Parameter(
            description = "좋아요 상태를 확인할 사용자의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable userId: Long
    ): ResponseEntity<LikeResponse> {
        return ResponseEntity.ok(likeService.getLikeStatus(petId, userId))
    }

    @Operation(
        summary = "반려동물의 좋아요 수 조회",
        description = "특정 반려동물이 받은 총 좋아요 수를 조회합니다.",
        tags = ["Like"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "좋아요 수 조회 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/pet/{petId}/count")
    fun getLikeCount(
        @Parameter(
            description = "조회할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long
    ): ResponseEntity<Long> {
        return ResponseEntity.ok(likeService.getLikeCount(petId))
    }
}
