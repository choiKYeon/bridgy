package org.grr.bridgy.domain.comment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.comment.dto.CommentResponse
import org.grr.bridgy.domain.comment.dto.CreateCommentRequest
import org.grr.bridgy.domain.comment.dto.UpdateCommentRequest
import org.grr.bridgy.domain.comment.service.CommentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentService: CommentService
) {

    @Operation(summary = "댓글 작성", description = "반려동물에 댓글 작성")
    @PostMapping
    fun createComment(@RequestBody request: CreateCommentRequest): ResponseEntity<CommentResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(request))
    }

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

    @Operation(summary = "댓글 수정", description = "댓글 내용 수정")
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody request: UpdateCommentRequest
    ): ResponseEntity<CommentResponse> {
        return ResponseEntity.ok(commentService.updateComment(commentId, request))
    }

    @Operation(summary = "댓글 삭제", description = "댓글 삭제")
    @DeleteMapping("/{commentId}")
    fun deleteComment(@PathVariable commentId: Long): ResponseEntity<Void> {
        commentService.deleteComment(commentId)
        return ResponseEntity.noContent().build()
    }
}
