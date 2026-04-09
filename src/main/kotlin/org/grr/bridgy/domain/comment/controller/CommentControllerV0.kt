package org.grr.bridgy.domain.comment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.comment.dto.CommentResponse
import org.grr.bridgy.domain.comment.service.CommentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Comment V0", description = "댓글 공개 API (인증 불필요)")
@RestController
@RequestMapping("/api/v0/comments")
class CommentControllerV0(
    private val commentService: CommentService
) {

    @Operation(
        summary = "반려동물별 댓글 목록 조회",
        description = "특정 반려동물에 달린 댓글을 최신순으로 조회합니다. 각 댓글은 작성자 정보와 작성 시간을 포함합니다.",
        tags = ["Comment"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "댓글 목록 조회 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/pet/{petId}")
    fun getCommentsByPet(
        @Parameter(
            description = "조회할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long
    ): ResponseEntity<List<CommentResponse>> {
        return ResponseEntity.ok(commentService.getCommentsByPetId(petId))
    }

    @Operation(
        summary = "반려동물 댓글 수 조회",
        description = "특정 반려동물에 달린 총 댓글 수를 조회합니다.",
        tags = ["Comment"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "댓글 수 조회 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/pet/{petId}/count")
    fun getCommentCount(
        @Parameter(
            description = "조회할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long
    ): ResponseEntity<Long> {
        return ResponseEntity.ok(commentService.getCommentCount(petId))
    }
}
