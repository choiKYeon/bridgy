package org.grr.bridgy.domain.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.user.dto.UpdateUserRequest
import org.grr.bridgy.domain.user.dto.UserResponse
import org.grr.bridgy.domain.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "User V1", description = "회원 인증 API (JWT Bearer 토큰 필요)")
@RestController
@RequestMapping("/api/v1/users")
class UserControllerV1(
    private val userService: UserService
) {

    @Operation(
        summary = "회원 정보 조회",
        description = "사용자 ID로 회원 정보를 조회합니다. 닉네임, 프로필 이미지, 소개글 등을 포함합니다.",
        tags = ["User"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "회원 정보 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 사용자를 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/{userId}")
    fun getUser(
        @Parameter(
            description = "조회할 사용자의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable userId: Long
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.getUserById(userId))
    }

    @Operation(
        summary = "회원 정보 수정",
        description = "사용자의 닉네임, 프로필 이미지 URL, 소개글을 수정합니다.",
        tags = ["User"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "회원 정보 수정 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserResponse::class)
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
                description = "자신의 정보만 수정 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 사용자를 찾을 수 없음"
            )
        ]
    )
    @PutMapping("/{userId}")
    fun updateUser(
        @Parameter(
            description = "수정할 사용자의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable userId: Long,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.updateUser(userId, request))
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "사용자 계정을 삭제합니다. 관련된 모든 데이터(반려동물, 댓글, 좋아요 등)도 함께 삭제됩니다. 이 작업은 복구할 수 없습니다.",
        tags = ["User"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "회원 탈퇴 성공"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "자신의 계정만 삭제 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 사용자를 찾을 수 없음"
            )
        ]
    )
    @DeleteMapping("/{userId}")
    fun deleteUser(
        @Parameter(
            description = "삭제할 사용자의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        userService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }
}
