package org.grr.bridgy.domain.comment.controller

import org.grr.bridgy.domain.comment.dto.CommentResponse
import org.grr.bridgy.domain.comment.dto.CreateCommentRequest
import org.grr.bridgy.domain.comment.dto.UpdateCommentRequest
import org.grr.bridgy.domain.comment.service.CommentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentService: CommentService
) {

    @PostMapping
    fun createComment(@RequestBody request: CreateCommentRequest): ResponseEntity<CommentResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(request))
    }

    @GetMapping("/pet/{petId}")
    fun getCommentsByPet(@PathVariable petId: Long): ResponseEntity<List<CommentResponse>> {
        return ResponseEntity.ok(commentService.getCommentsByPetId(petId))
    }

    @GetMapping("/pet/{petId}/count")
    fun getCommentCount(@PathVariable petId: Long): ResponseEntity<Long> {
        return ResponseEntity.ok(commentService.getCommentCount(petId))
    }

    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody request: UpdateCommentRequest
    ): ResponseEntity<CommentResponse> {
        return ResponseEntity.ok(commentService.updateComment(commentId, request))
    }

    @DeleteMapping("/{commentId}")
    fun deleteComment(@PathVariable commentId: Long): ResponseEntity<Void> {
        commentService.deleteComment(commentId)
        return ResponseEntity.noContent().build()
    }
}
