package org.grr.bridgy.domain.like.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.like.dto.LikeRequest
import org.grr.bridgy.domain.like.dto.LikeResponse
import org.grr.bridgy.domain.like.service.LikeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Like V1", description = "좋아요 인증 API")
@RestController
@RequestMapping("/api/v1/likes")
class LikeControllerV1(
    private val likeService: LikeService
) {

    @Operation(summary = "좋아요 토글", description = "좋아요 누르기/취소 (토글 방식)")
    @PostMapping
    fun toggleLike(@RequestBody request: LikeRequest): ResponseEntity<LikeResponse> {
        return ResponseEntity.ok(likeService.toggleLike(request))
    }
}
