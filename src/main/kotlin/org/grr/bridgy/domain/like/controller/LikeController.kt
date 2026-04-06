package org.grr.bridgy.domain.like.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.like.dto.LikeRequest
import org.grr.bridgy.domain.like.dto.LikeResponse
import org.grr.bridgy.domain.like.service.LikeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Like", description = "좋아요 API")
@RestController
@RequestMapping("/api/likes")
class LikeController(
    private val likeService: LikeService
) {

    @Operation(summary = "좋아요 토글", description = "좋아요 누르기/취소 (토글 방식)")
    @PostMapping
    fun toggleLike(@RequestBody request: LikeRequest): ResponseEntity<LikeResponse> {
        return ResponseEntity.ok(likeService.toggleLike(request))
    }

    @Operation(summary = "좋아요 상태 조회", description = "특정 유저의 좋아요 여부 및 총 좋아요 수")
    @GetMapping("/pet/{petId}/user/{userId}")
    fun getLikeStatus(
        @PathVariable petId: Long,
        @PathVariable userId: Long
    ): ResponseEntity<LikeResponse> {
        return ResponseEntity.ok(likeService.getLikeStatus(petId, userId))
    }

    @Operation(summary = "좋아요 수 조회", description = "특정 반려동물의 총 좋아요 수")
    @GetMapping("/pet/{petId}/count")
    fun getLikeCount(@PathVariable petId: Long): ResponseEntity<Long> {
        return ResponseEntity.ok(likeService.getLikeCount(petId))
    }
}
