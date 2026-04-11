package org.grr.bridgy.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.auth.dto.*
import org.grr.bridgy.domain.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth V0", description = "인증 공개 API (JWT 토큰 발급)")
@RestController
@RequestMapping("/api/v0/auth")
class AuthControllerV0(
    private val authService: AuthService
) {

    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인하여 JWT 액세스 토큰과 리프레시 토큰을 발급받습니다.",
        tags = ["Auth"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TokenResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 이메일 또는 비밀번호"
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음"
            )
        ]
    )
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @Operation(
        summary = "토큰 갱신",
        description = "리프레시 토큰을 이용하여 새 액세스 토큰을 발급받습니다.",
        tags = ["Auth"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토큰 갱신 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = TokenResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않거나 만료된 리프레시 토큰"
            ),
            ApiResponse(
                responseCode = "401",
                description = "리프레시 토큰이 없음"
            )
        ]
    )
    @PostMapping("/refresh")
    fun refresh(@RequestBody request: TokenRefreshRequest): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(authService.refresh(request))
    }

    @Operation(summary = "아이디 찾기", description = "닉네임으로 마스킹된 이메일을 반환합니다.")
    @PostMapping("/find-id")
    fun findId(@RequestBody request: FindIdRequest): ResponseEntity<FindIdResponse> {
        return ResponseEntity.ok(authService.findId(request))
    }

    @Operation(summary = "비밀번호 찾기", description = "이메일로 비밀번호 재설정 코드를 발급합니다. (10분 유효)")
    @PostMapping("/find-password")
    fun findPassword(@RequestBody request: FindPasswordRequest): ResponseEntity<FindPasswordResponse> {
        return ResponseEntity.ok(authService.requestPasswordReset(request))
    }

    @Operation(summary = "비밀번호 재설정", description = "발급받은 코드와 새 비밀번호로 비밀번호를 변경합니다.")
    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
        authService.resetPassword(request)
        return ResponseEntity.noContent().build()
    }
}
