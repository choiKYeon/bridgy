package org.grr.bridgy.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.auth.dto.LoginRequest
import org.grr.bridgy.domain.auth.dto.TokenRefreshRequest
import org.grr.bridgy.domain.auth.dto.TokenResponse
import org.grr.bridgy.domain.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth V0", description = "인증 공개 API")
@RestController
@RequestMapping("/api/v0/auth")
class AuthControllerV0(
    private val authService: AuthService
) {

    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인 후 JWT 토큰 발급")
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰 발급")
    @PostMapping("/refresh")
    fun refresh(@RequestBody request: TokenRefreshRequest): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(authService.refresh(request))
    }
}
