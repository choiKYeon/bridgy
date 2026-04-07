package org.grr.bridgy.domain.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.user.dto.UpdateUserRequest
import org.grr.bridgy.domain.user.dto.UserResponse
import org.grr.bridgy.domain.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "User V1", description = "회원 인증 API")
@RestController
@RequestMapping("/api/v1/users")
class UserControllerV1(
    private val userService: UserService
) {

    @Operation(summary = "회원 조회", description = "userId로 회원 정보 조회")
    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: Long): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.getUserById(userId))
    }

    @Operation(summary = "회원 수정", description = "닉네임, 프로필 이미지, 소개글 수정")
    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: Long,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.updateUser(userId, request))
    }

    @Operation(summary = "회원 삭제", description = "회원 탈퇴")
    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: Long): ResponseEntity<Void> {
        userService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }
}
