package org.grr.bridgy.domain.comment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.comment.dto.CommentResponse
import org.grr.bridgy.domain.comment.service.CommentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Comment V0", description = "댓글 공개 API")
@RestController
@RequestMapping("/api/v0/comments")
class CommentControllerV0(
    private val commentService: CommentService
) {

    @Operation(summary = "펫별 댓글 목록", description = "특정 반려동물의 댓글 목록 (최신순)")
    @GetMapping("/pet/{petId}")
    fun getCommentsByPet(@PathVariable petId: Long): ResponseEntity<List<CommentResponse>> {
        return ResponseEntity.ok(commentService.getCommentsByPetId(petId))
    }

    @Operation(summary = "댓글 수 조회", description = "특정 반려동물의 총 댓글 수")
    @GetMapping("/pet/{petId}/count")
    fun getCommentCount(@PathVariable petId: Long): ResponseEntity<Long> {
        return ResponseEntity.ok(commentService.getCommentCount(petId))
    }
}
