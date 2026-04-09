package org.grr.bridgy.domain.comment.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.comment.dto.CommentResponse
import org.grr.bridgy.domain.comment.dto.CreateCommentRequest
import org.grr.bridgy.domain.comment.dto.UpdateCommentRequest
import org.grr.bridgy.domain.comment.service.CommentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Comment V1", description = "댓글 인증 API (JWT Bearer 토큰 필요)")
@RestController
@RequestMapping("/api/v1/comments")
class CommentControllerV1(
    private val commentService: CommentService
) {

    @Operation(
        summary = "댓글 작성",
        description = "반려동물에 댓글을 작성합니다. 최대 길이: 300자, 일일 제한: 30개. 하루가 지나면 제한이 초기화됩니다.",
        tags = ["Comment"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "댓글 작성 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CommentResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 입력 데이터 또는 댓글 길이 초과 (최대 300자)"
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
                description = "해당 반려동물을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "댓글 작성 초과 (하루 최대 30개) 또는 Rate limiting"
            )
        ]
    )
    @PostMapping
    fun createComment(@RequestBody request: CreateCommentRequest): ResponseEntity<CommentResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(request))
    }

    @Operation(
        summary = "댓글 수정",
        description = "작성한 댓글의 내용을 수정합니다. 최대 300자까지 작성 가능합니다.",
        tags = ["Comment"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "댓글 수정 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = CommentResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 입력 데이터 또는 댓글 길이 초과 (최대 300자)"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "자신의 댓글만 수정 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 댓글을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @PutMapping("/{commentId}")
    fun updateComment(
        @Parameter(
            description = "수정할 댓글의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable commentId: Long,
        @RequestBody request: UpdateCommentRequest
    ): ResponseEntity<CommentResponse> {
        return ResponseEntity.ok(commentService.updateComment(commentId, request))
    }

    @Operation(
        summary = "댓글 삭제",
        description = "작성한 댓글을 삭제합니다. 자신의 댓글만 삭제 가능하며, 이 작업은 복구할 수 없습니다.",
        tags = ["Comment"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "댓글 삭제 성공"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "자신의 댓글만 삭제 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 댓글을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @Parameter(
            description = "삭제할 댓글의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable commentId: Long
    ): ResponseEntity<Void> {
        commentService.deleteComment(commentId)
        return ResponseEntity.noContent().build()
    }
}
