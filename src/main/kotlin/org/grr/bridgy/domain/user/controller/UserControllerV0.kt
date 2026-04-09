package org.grr.bridgy.domain.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.user.dto.SignUpRequest
import org.grr.bridgy.domain.user.dto.UserResponse
import org.grr.bridgy.domain.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "User V0", description = "회원 공개 API (회원가입)")
@RestController
@RequestMapping("/api/v0/users")
class UserControllerV0(
    private val userService: UserService
) {

    @Operation(
        summary = "회원가입",
        description = "새로운 사용자 계정을 생성합니다. 이메일은 유일해야 하며, 비밀번호는 암호화되어 저장됩니다.",
        tags = ["User"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "회원가입 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UserResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 입력 데이터 또는 중복된 이메일"
            ),
            ApiResponse(
                responseCode = "409",
                description = "이미 존재하는 이메일 주소"
            )
        ]
    )
    @PostMapping("/signup")
    fun signUp(@RequestBody request: SignUpRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signUp(request))
    }
}
