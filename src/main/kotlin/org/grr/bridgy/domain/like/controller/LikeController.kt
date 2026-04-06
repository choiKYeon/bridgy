package org.grr.bridgy.domain.like.controller

import org.grr.bridgy.domain.like.dto.LikeRequest
import org.grr.bridgy.domain.like.dto.LikeResponse
import org.grr.bridgy.domain.like.service.LikeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/likes")
class LikeController(
    private val likeService: LikeService
) {

    @PostMapping
    fun toggleLike(@RequestBody request: LikeRequest): ResponseEntity<LikeResponse> {
        return ResponseEntity.ok(likeService.toggleLike(request))
    }

    @GetMapping("/pet/{petId}/user/{userId}")
    fun getLikeStatus(
        @PathVariable petId: Long,
        @PathVariable userId: Long
    ): ResponseEntity<LikeResponse> {
        return ResponseEntity.ok(likeService.getLikeStatus(petId, userId))
    }

    @GetMapping("/pet/{petId}/count")
    fun getLikeCount(@PathVariable petId: Long): ResponseEntity<Long> {
        return ResponseEntity.ok(likeService.getLikeCount(petId))
    }
}
