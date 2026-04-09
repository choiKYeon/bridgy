package org.grr.bridgy.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth V1", description = "인증 필수 API (JWT Bearer 토큰 필요)")
@RestController
@RequestMapping("/api/v1/auth")
class AuthControllerV1(
    private val authService: AuthService
) {

    @Operation(
        summary = "로그아웃",
        description = "사용자 계정의 리프레시 토큰을 삭제하여 로그아웃 처리합니다. 이후 해당 리프레시 토큰으로는 새 액세스 토큰을 발급받을 수 없습니다.",
        tags = ["Auth"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "로그아웃 성공"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "접근 권한 없음"
            )
        ]
    )
    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Void> {
        val userId = userDetails.username.toLong()
        authService.logout(userId)
        return ResponseEntity.noContent().build()
    }
}
