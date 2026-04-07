package org.grr.bridgy.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth V1", description = "인증 필요 API")
@RestController
@RequestMapping("/api/v1/auth")
class AuthControllerV1(
    private val authService: AuthService
) {

    @Operation(summary = "로그아웃", description = "리프레시 토큰 삭제")
    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Void> {
        val userId = userDetails.username.toLong()
        authService.logout(userId)
        return ResponseEntity.noContent().build()
    }
}
